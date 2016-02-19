(ns pages.core
  (:require
   [devcards.core]
   [complex.number :as n :refer [zero one i negative-one negative-i infinity add sub mult div]]
   [hello-devcards.bezier]
   [hello-devcards.pixie-turtle]
   [hello-devcards.chessboard]
   [hello-devcards.color-wheel])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest defcard-rg defcard-doc]]))

(devcards.core/start-devcard-ui!)