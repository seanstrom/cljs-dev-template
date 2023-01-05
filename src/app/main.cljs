(ns app.main
  (:require
   [reagent.core :as r]
   ["react" :as react :refer [StrictMode]]
   ["react-dom/client" :as react-dom]
   ["reactflow" :as reactflow :refer [Background Controls ReactFlow]]))

(defn make-client-root []
  (let
   [domNode (js/document.getElementById "root")]
    (react-dom/createRoot domNode)))

(defonce rf-background (r/adapt-react-class Background))
(defonce rf-main (r/adapt-react-class ReactFlow))
(defonce rf-controls (r/adapt-react-class Controls))
(defonce strict-mode (r/adapt-react-class StrictMode))

;; (defonce client-root (make-client-root))
(defonce nodes
  #js [#js {:id "1"
            :position #js {:x 0 :y 0}}])

(defonce props {:nodes nodes})
(defonce client-root (make-client-root))

(defn main-ui []
  [strict-mode
   [:div {:style {:height "100%"}}
    [rf-main {:nodes nodes}
     [rf-background]
     [rf-controls]]]])

(defn ^:export render []
  (println "[main]: render")
  (let
   [element (r/as-element [main-ui])]
    (. client-root render (r/create-element StrictMode nil element))))
