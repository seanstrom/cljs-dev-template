(ns app.main)

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))
