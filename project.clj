(defproject hello-devcards "0.1.0-SNAPSHOT"
  :description "Hello Devcards"
  :url "http://wbabic.github.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/tools.reader "1.0.0-alpha1"]
                 [devcards "0.2.1-5" :exclusions [org.clojure/tools.reader]]
                 [sablono "0.5.3"]
                 [org.omcljs/om "0.9.0"]
                 [reagent "0.5.1"]

                 [thi.ng/geom "0.0.908"]
                 [thi.ng/color "1.0.0"]
                 [thi.ng/strf "0.2.1"]

                 [complex/complex "0.1.9"]

                 [ring/ring-core "1.4.0"]
                 [clj-time "0.9.0"]

                 ;; for fun
                 [timothypratley/reanimated "0.1.1"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-3"
             :exclusions [org.clojure/clojure
                          ring/ring-core joda-time
                          org.clojure/tools.reader]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "devcards"
                        :source-paths ["src"]
                        :figwheel { :devcards true } ;; <- note this
                        :compiler { :main       "hello-devcards.core"
                                    :asset-path "js/compiled/devcards_out"
                                    :output-to  "resources/public/js/compiled/hello_devcards_devcards.js"
                                    :output-dir "resources/public/js/compiled/devcards_out"
                                    :source-map-timestamp true }}
                       {:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "hello-devcards.core"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/hello_devcards.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true }}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:main       "hello-devcards.core"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/hello_devcards.js"
                                   :optimizations :advanced}}]}

  :figwheel { :css-dirs ["resources/public/css"] })
