(ns server.server
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom]))

(defn ^:export main []
  (prn "Hello World!"))

(defn ^:dev/after-load reload! []
  (prn "Reload CLJS"))

(comment
  (sigf plus1 [:=> [:cat :int] :int])
  (defn plus1 [x] (inc x))
  (plus1 (plus1 0)))

(comment
  (def state {:nested {:a 1
                       :extra
                       {:b 2}}})
  (def source (reagent/atom state))

  (def field-a (ratom/cursor source [:nested :a]))
  (def field-b (ratom/cursor source [:nested :extra :b]))
  (def combined (ratom/reaction #js {:a @field-a
                                     :b @field-b}))

  (def reactive-js-obj (ratom/make-reaction
                        (fn []
                          (prn "combined changed")
                          @combined)
                        :auto-run true))

  (do (prn @field-a)
      (prn @field-b)
      (prn @combined)
      (prn @reactive-js-obj))

  (def snapshot-a @combined)

  (identical? snapshot-a @combined)

  (let []

    (swap! source assoc :nested {:a 2
                                 :extra
                                 {:b 3}})))
