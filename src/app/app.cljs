(ns app.app)

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (let [button-element (js/document.createElement "button")]
    (.appendChild js/document.body button-element)
    (.addEventListener button-element "click"
                       (fn [e]
                         (throw (js/Error. "button clicked"))
                         (println "[main]: button clicked"))))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))
