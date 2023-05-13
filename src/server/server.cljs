(ns server.server
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]))

(defn ^:export main []
  (prn "Hello World!"))

(defn ^:dev/after-load reload! []
  (prn "Reload CLJS"))

(comment
  (sigf plus1 [:=> [:cat :int] :int])
  (defn plus1 [x] (inc x))
  (plus1 (plus1 0)))
