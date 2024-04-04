(ns app.app
  (:require
   ["atomico" :as atomico :refer [c html css useProp]]
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [reagent.dom.client :as rdom-client]
   [goog.dom :as gdom]
   ["react" :as react]))

(defn format-time [seconds]
  (let [hours (int (/ seconds 3600))
        minutes (int (/ (mod seconds 3600) 60))
        seconds (mod seconds 60)
        formatted-minutes (if (< minutes 10)
                            (str "0" minutes)
                            minutes)
        formatted-seconds (if (< seconds 10)
                            (str "0" seconds)
                            seconds)]
    (str hours ":" formatted-minutes ":" formatted-seconds)))

(defn format-money [amount]
  (let [dollars (int amount)
        cents (int (* (mod amount 1) 100))
        formatted-cents (if (< cents 10)
                          (str "0" cents)
                          cents)]
    (str "£" dollars "." formatted-cents)))

(defn app-clock [app-state dispatch]
  (let [counted-seconds (ratom/cursor app-state [:clock-state :counted-seconds])
        wages-per-second (ratom/cursor app-state [:clock-state :wages-per-second])
        funds-raised (ratom/reaction
                      (format-money (* @wages-per-second @counted-seconds)))]
    [:div
     [:h1 "Hey there Sean 👋"]
     [:h2 "This much time has passed: " (format-time @counted-seconds)]
     [:h2 "And this is how much that we've earned: " @funds-raised]
     [:button {:on-click #(dispatch [:start-clock])} "Start"]
     [:button {:on-click #(dispatch [:stop-clock])} "Stop"]
     [:button {:on-click #(dispatch [:reset-clock])} "Reset"]]))

(def clock-state-default {:status :idle
                          :timer nil
                          :wages-per-second 0.01005
                          :counted-seconds 0})

(defonce app-state (r/atom {:clock-state clock-state-default}))

(defonce app-root (rdom-client/create-root
                   (gdom/getElement "root")))

(defn clock-dispatch [message clock-state]
  (let [[message-id] message]
    (case message-id
      :start-clock (swap! clock-state
                          (fn update [state]
                            (merge state
                                   {:status :running
                                    :timer (js/setInterval
                                            (fn []
                                              (let [counted-seconds (ratom/cursor clock-state [:counted-seconds])]
                                                (set! (.. js/document -title)
                                                      (str (format-money (* (:wages-per-second state) @counted-seconds))
                                                           " raised so far"))
                                                (swap! counted-seconds inc)))
                                            1000)})))
      :stop-clock (swap! clock-state merge
                         {:timer (js/clearInterval (:timer @clock-state))
                          :status :idle})
      :reset-clock (reset! (ratom/cursor clock-state [:counted-seconds]) 0))))

(defn app-dispatch [message]
  (let [[message-id] message]
    (when (contains? #{:start-clock :stop-clock :reset-clock} message-id)
      (clock-dispatch message (ratom/cursor app-state [:clock-state])))))

;; ---

(def use-ref react/useRef)

(defn use-ref-atom
  [value]
  (let [ref (use-ref (atom value))]
    (.-current ^js ref)))

(defn get-js-deps
  [deps]
  (if deps
    (if (empty? deps)
      #js [true]
      (let [prev-state (use-ref-atom {:value false :deps nil})
            prev-deps  (:deps @prev-state)
            prev-value (:value @prev-state)]
        (if (and (not (nil? prev-deps)) (not= (count deps) (count prev-deps)))
          (throw (js/Error. "Hooks can't have a different number of dependencies across re-renders"))
          (if (not= deps prev-deps)
            (let [new-value (not prev-value)]
              (reset! prev-state {:value new-value
                                  :deps  deps})
              #js [new-value])
            #js [prev-value]))))
    js/undefined))

(defn use-layout-effect
  ([handler]
   (use-layout-effect handler nil))
  ([handler deps]
   (react/useLayoutEffect
    #(let [ret (handler)] (if (fn? ret) ret js/undefined))
    (get-js-deps deps))))

(defn use-effect
  {:deprecated
   "use-mount or use-unmount should be used, more here https://github.com/status-im/status-mobile/blob/develop/doc/ui-guidelines.md#effects"}
  ([handler]
   (use-effect handler nil))
  ([handler deps]
   (react/useEffect
    #(let [ret (handler)] (if (fn? ret) ret js/undefined))
    (get-js-deps deps))))

(defn use-mount
  [handler]
  (use-effect handler []))

(defn use-unmount
  [handler]
  (use-mount (fn [] handler)))

(defn use-callback
  ([handler]
   (use-callback handler []))
  ([handler deps]
   (react/useCallback handler (get-js-deps deps))))

(defn use-memo
  [handler deps]
  (react/useMemo handler (get-js-deps deps)))

(defn sample-ratom!
  [[ref ratom]]
  (let [next-state @ratom]
    (swap! ref
           (fn [_previous-state]
             next-state))
    next-state))

(defn make-sample
  [ratom]
  (let [ref (atom nil)]
    {:ref ref
     :sub (ratom/track sample-ratom! [ref ratom])}))

(defn |> [arg f]
  (f arg))

(defn make-state-samples
  [state-ratom dispatch]
  (let [live-sample (make-sample state-ratom)
        snapshot-sample (make-sample (:sub live-sample))
        factory (memoize
                 (fn [callback]
                   (js/console.log "init action callback")
                   (fn [event]
                     (callback (let [state @(:ref live-sample)
                                     snapshot @(:ref snapshot-sample)]
                                 {:state state
                                  :snapshot snapshot
                                  :dispatch (partial dispatch state-ratom)})
                               event))))]
    {:live live-sample
     :snapshot snapshot-sample
     :factory factory}))

(defn use-bind
  [state-ratom dispatch]
  (let [{:keys [live snapshot factory]}
        (use-memo (fn []
                   (make-state-samples state-ratom dispatch))
                 [state-ratom dispatch])]
    (use-layout-effect
     (fn []
       @(:sub snapshot)
       #(do (prn "layout cleanup"))))
    (use-unmount
     (fn []
       #(do (ratom/dispose! (:sub snapshot))
            (ratom/dispose! (:sub live)))))
    factory))

(defn on-click-component
  ;; Comment: 
  ;; * We always expect the event handlers to receive:
  ;;     * readable state
  ;;     * app dispatch
  ;;     * and their related event.
  [{:keys [state snapshot dispatch]} event]

  (js/console.log "on-click snap" (clj->js snapshot))
  (js/console.log "on-click state" (clj->js state))
  (js/console.log "on-click event" event)
  (when (= 0 (:count state))
    (js/console.log "init"))
  (dispatch [:increment]))

(defn example-component [state dispatch]
  (let [bind (use-bind state dispatch)]
    [:div
     [:button {:on-click (bind on-click-component)}
      "button"]
     (:count @state)]))

(defn example-dispatch [state message]
  (let [[message-id] message]
    (js/console.log "message" message-id)
    (when (= message-id :increment)
      (swap! state update-in [:count] inc))))

(defonce example-state (r/atom {:count 0}))

;; ---

(def functional-compiler (r/create-compiler {:function-components true}))

(r/set-default-compiler! functional-compiler)

(defn ^:export render []
  (println "[main]: render" @app-state)
  (rdom-client/render app-root [example-component example-state example-dispatch]))

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

;; (.define js/customElements "my-component" (c component))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
