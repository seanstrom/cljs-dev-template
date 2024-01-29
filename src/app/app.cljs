(ns app.app
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   ["atomico" :as atomico :refer [c html css useProp]]))

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (println "[main]: render"))

(defn component []
  (let [icon (useProp "icon")]
    (println "component test")
    (html [:host {:shadowDom true}
           icon
           [:slot]]))

  (let [icon (useProp "icon")]
    (html [:host {:shadowDom true}
           icon
           [:slot]])))

(set! (.-props component)
      #js{:icon js/String})

;; TODO: create css function that converts hiccup to string
#_(set! (.-styles component)
        (css #js[":host" "{" "font-size: 30px;" "}"]))

(.define js/customElements "my-component" (c component))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
