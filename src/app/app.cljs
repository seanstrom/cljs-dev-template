(ns app.app
  (:require
   [cljs.core.match :refer-macros [match]]
   [malli.core :as m :refer [=>] :rename {=> sigf}]))

;; --- Elm Architecture Stuff ---

(def person [:map
             [:id :int]
             [:name :string]])

(def model [:map
            [:found-person [:maybe person]]])

(def message [:enum
              :Ready
              :Loading
              [:tuple :Error :string]
              [:tuple :Data person]])

(defn task [on-success on-error]
  [:map
   [:action [:cat :keyword :any]]
   [:on-success on-success]
   [:on-error on-error]])

(sigf init [:=> [:cat :int] [:map
                             [:model model]
                             [:message message]]])
(defn init [n]
  {:model {:found-person nil}
   :message :Ready #_{:action [:sleep n]
                      :on-success :Data
                      :on-error :Error}})

(sigf something [:=> [:cat message] :any])
(defn something [task]
  (js/console.log "something" task))

(let [stuff (init 0)]
  (something :test))
(comment
  (let [stuff (init 0)]
    (something (:model stuff))))

(sigf update-model [:=> [:cat message model] model])
(defn update-model [message model]
  (match message
    :Ready model
    :Loading (assoc model :found-person nil)
    [:Error err] (assoc model :found-person nil)
    [:Data person] (assoc model :found-person person)
    :else model))

(sigf view [:=> [:cat model] :string])
(defn view [model]
  (match model
    {:found-person nil} "Loading..."
    {:found-person person} (str "Found person: " (:name person))
    :else "Error"))

(sigf subscribe [:=> [:cat model] :any])
(defn subscribe [app-model]
  [:Task {:sleep 1000}
   :on-success
   :on-failure])

(defn run []
  (let [{app-model :model
         app-message :messsage} (init 0)]
    (subscribe app-model)
    (update-model app-message app-model)))

(comment
  (run))

;; ---

(sigf plus1 [:=> [:cat :int] :string])
(defn plus1 [x] (inc x))

(sigf tap [:=> [:cat person] person])
(defn tap [person]
  (println "tap" {:name person})
  person)

;; ---

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (let [person {:id 1
                  :name "foo"}
          app-model {:found-person person}
          nil-model {:found-person nil}
          app-text (js/document.createTextNode (view app-model))
          nil-text (js/document.createTextNode (view nil-model))]
      (js/setTimeout #(do
                        (.replaceChild node
                                       nil-text
                                       text-node)) 1000)
      (js/setTimeout #(do
                        (.replaceChild node
                                       app-text
                                       nil-text)) 2000))
    (.appendChild node text-node))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
