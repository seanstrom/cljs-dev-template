(ns app.app
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]))

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
