(ns hello-devcards.chessboard
  (:require
   [devcards.core]
   [hello-devcards.pixie :as p]
   [hello-devcards.svg :as svg]
   [reagent.core :as reagent]
   [cljs.core.async :as async :refer [>! <! put! chan alts! timeout]])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest defcard-rg defcard-doc]]
   [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def init-turtle
  {:position [0 0]
   :heading :east
   :scale 1})

(def init-app-state
  {:turtle init-turtle
   :polyline []
   :squares []
   :pen :up
   :base 32
   :resolution (* 4 32)})

(defn add-point [app point]
  (update-in app [:polyline] #(conj % point)))

(defn add-square [app square]
  (update-in app [:squares] #(conj % square)))

(def app-state (reagent/atom init-app-state))

(defn send!
  "Send information from the user to the message queue.
  The message must be a record which implements the Processor protocol."
  [channel message]
  (fn [dom-event]
    (put! channel message)
    (.stopPropagation dom-event)))

(defn transform-str [turtle]
  (let [{:keys [position heading scale]} turtle
        [x y] position
        angle (p/heading->angle heading)]
    (svg/transform-str position angle scale)))

(defrecord Penup [])
(defrecord Pendown [])
(defrecord ClosePoly [c])

(defrecord Pause [s])

(defprotocol ChessCommand
  (process-command [command app-state]))

(extend-protocol ChessCommand
  Penup
  (process-command [_ app]
    (let [pen (:pen app)]
      (if (= pen :down)
        (-> app
            (assoc-in [:pen] :up)
            (assoc-in [:polyline] []))
        app)))
  Pendown
  (process-command [_ app]
    (let [{:keys [turtle pen polyline]} app]
      ;; do nothing if pen is already down
      (if (= pen :up)
        (-> app
            (assoc-in [:pen] :down)
            (assoc-in [:polyline] [(:position turtle)]))
        app)))
  ClosePoly
  (process-command [{c :c} app]
    (-> app
        (add-square {:class-name c :points (drop-last (:polyline app))})
        (assoc-in [:polyline] [])
        (assoc-in [:pen] :up))))

(defn square-program [base color]
  (vec (flatten
           (list
            (->Pendown)
            (flatten
             (repeat 4
                     [(p/->Forward base)
                      (p/->Right)
                      (->Pause 100)]))
            (->ClosePoly color)))))

(defn four-square [base]
  (flatten
   (repeat 2
           (concat
            (conj (square-program base "white")
                  (p/->Right))
            (conj (square-program base "black")
                  (p/->Right))))))

(defn row [base n]
  (flatten
   (repeat n
           (concat
            (conj (square-program base "white")
                  (p/->Forward base))
            (conj (square-program base "black")
                  (p/->Forward base))))))

(defn two-row [base n]
  (concat
   (row base n)
   (list (p/->Right) (p/->Forward (* 2 base)) (p/->Right))
   (row base n)
   (list (p/->Right)(p/->Right))))

(defn four-row [base row-length]
  (flatten (repeat 2 (two-row base row-length))))

(defn run-program [chan program]
  (go
    (doseq [command program]
      (cond
        (instance? Pause command) (<! (timeout 100))
        :else (>! chan command)))))

(defn command-button
  "a single command button"
  [ui-channel name command]
  [:button {:on-click (send! ui-channel command)
            :class "command"} name])

(defn command-buttons
  "gui for command buttons"
  [ui-channel base]
  (let [commands
        [["Forward"  (p/->Forward (* base  1))]
         ["Backward" (p/->Forward (* base -1))]
         ["Left"     (p/->Left)]
         ["Right"    (p/->Right)]
         ["Half"     (p/->Resize (/ 2))]
         ["Double"   (p/->Resize 2)]]]
    (into [:div]
          (map #(apply command-button ui-channel %) commands))))

(defn reset-state []
  (reset! app-state init-app-state))

(defn program-buttons
  "program buttons"
  [ui-channel base]
  [:div
   [:button {:on-click #(run-program ui-channel (square-program base "white"))
             :class "command"}
    "White Square"]
   [:button {:on-click #(run-program ui-channel (square-program base "black"))
             :class "command"}
    "Black Square"]
   [:button {:on-click #(run-program ui-channel (four-square base))
             :class "command"}
    "Four Square"]
   [:button {:on-click #(run-program ui-channel (row base 2))
             :class "command"}
    "Two in a Row"]
   [:button {:on-click #(run-program ui-channel (two-row base 2))
             :class "command"}
    "Two Rows"]
   [:button {:on-click #(run-program ui-channel (four-row base 2))
             :class "command"}
    "four Rows"]
   [:button {:on-click #(reset-state)
             :class "command"}
    "reset"]])

(defn pixie? [command]
  (satisfies? p/Command command))

(defn update-state
  "return new state for given command"
  [state command]
  (let [{:keys [turtle polyline squares pen]} state]
    (if (pixie? command)
      (let [new-turtle (p/process-command command turtle)]
        (if (and (= :down pen) (instance? p/Forward command))
          (-> state
              (assoc-in [:turtle] new-turtle)
              (add-point (:position new-turtle)))
          (assoc-in state [:turtle] new-turtle)))
      (process-command command state))))

(defn process-channel [turtle-channel]
  (go (loop []
        (let [command (<! turtle-channel)]
          (swap! app-state #(update-state % command))
          (recur)))))

(defn svg-square
  [{:keys [class-name points]}]
  (apply svg/polygon class-name points))

(defn square-group
  "make some svg squares in a group"
  [squares]
  (let [svg-squares (mapv svg-square squares)]
    (into [:g {:class "squares"}]
          svg-squares)))

(defn board
  "an svg chessboard using a pixie turtle"
  [app-atate]
  (let [{:keys [turtle squares polyline base resolution]} @app-state
        chan (chan)
        _ (process-channel chan)]
    [:div
     (command-buttons chan base)
     (program-buttons chan base)
     [:svg {:width resolution :height resolution :class "board"}
      (svg/defs
        (svg/straight-arrow "arrow" base))
      (when (not (empty? polyline))
        (apply svg/polyline "lines" polyline))
      (square-group squares)
      (svg/use-path "#arrow" (transform-str turtle) "turtle")]]))

(defcard-rg chessboard
  "Lets build a chessboard, one square at a time, turtle style
&#9812;
"
  [board app-state]
  app-state
  {:inspect-data true :history true})

(comment
  (in-ns 'hello-devcards.chessboard)
  (board)
  ;;=>
  @app-state
  ;;=> {:turtle {:position [20 40], :heading :east, :scale 1}, :squares []}

  (square-program 1 "white")

  (update-state init-app-state (p/->Forward 1))
  (update-state init-app-state (->Pendown))
  (reduce update-state init-app-state
          [(->Pendown)
           (p/->Forward 1)])
  (reduce update-state init-app-state
          [(->Pendown)
           (p/->Forward 1)
           (->Penup)])
  (reduce update-state init-app-state
          [(->Pendown)
           (p/->Forward 1)
           (p/->Right)
           (p/->Forward 1)
           (p/->Right)
           (p/->Forward 1)
           (p/->Right)
           (p/->Forward 1)
           (p/->Right)
           (->ClosePoly :white)])

  (four-square 1))
