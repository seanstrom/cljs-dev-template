(ns app.app
  (:require-macros
   [app.compiler :refer [compile-hiccup read-fn-def]])
  (:require
   ["~src/app/app.gleam" :refer [add_one]]
   ["atomico" :as atomico :refer [c html css useProp]]
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [reagent.dom.client :as rdom-client]
   [goog.dom :as gdom]
   ["react" :as react :refer [createElement Fragment]]))

(defn create-element
  ([f args]
   (apply f args))
  ([type config-js & child-or-children]
   (if (nil? config-js)
     (create-element (if (fn? type) type createElement) (conj nil child-or-children))
     (create-element
      (if (fn? type) type createElement)
      (cond-> child-or-children
        :always (conj config-js)
        (not (fn? type)) (conj type))))))

(js/console.log "add_one" add_one)

;; --

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

(def use-state react/useState)

(defn use-callback
  ([handler]
   (use-callback handler []))
  ([handler deps]
   (react/useCallback handler (get-js-deps deps))))

(defn use-memo
  [handler deps]
  (react/useMemo handler (get-js-deps deps)))

;; --

(def ^:private lookup-sentinel (js-obj))

(defn memo
  "Returns a memoized version of a referentially transparent function. The
  memoized version of the function keeps a cache of the mapping from arguments
  to results and, when calls with the same arguments are repeated often, has
  higher performance at the expense of higher memory use."
  [mem f]
  (fn [& args]
    (js/console.log "lookup")
    (let [v (get @mem args lookup-sentinel)]
      (if (identical? v lookup-sentinel)
        (let [ret (apply f args)]
          (js/console.log "swap")
          (swap! mem assoc args ret)
          ret)
        v))))

;; --

(defn |> [arg f]
  (f arg))

(defn <| [f arg]
  (f arg))

;; --

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

(defn make-state-handler-factory
  [{:keys [sub dispatch storage sample]}]
  (memo storage
        (fn [callback]
          (js/console.log "init action callback")
          (fn [event]
            (callback (let [state @(:ref sample)]
                        {:state state
                         :dispatch (partial dispatch sub)})
                      event)))))

(defn use-bind-sub
  [dispatch sub]
  (let [storage (use-memo #(atom {}) [sub dispatch])
        live-sample (use-memo #(make-sample sub) [sub])
        factory (use-memo #(make-state-handler-factory
                            {:sub sub
                             :dispatch dispatch
                             :storage storage
                             :sample live-sample})
                          [sub dispatch storage live-sample])]
    (use-unmount
     (fn []
       #(do (ratom/dispose! (:sub live-sample))
            (prn "dispose storage" storage))))
    factory))

(defn use-bind-data
  [dispatch data]
  (let [storage (use-memo #(atom {}) [dispatch])
        state-ratom (use-memo #(ratom/atom data) [dispatch])
        snapshot-sample (use-memo #(make-sample state-ratom) [dispatch])
        factory (use-memo #(make-state-handler-factory
                            {:sub state-ratom
                             :dispatch dispatch
                             :storage storage
                             :sample snapshot-sample})
                          [state-ratom dispatch storage snapshot-sample])]
    (use-effect #_use-layout-effect
     (fn []
       (print "effect")
       (swap! state-ratom (fn [_] data))
       js/undefined)
                [data])
    (use-unmount
     (fn []
       #(do (ratom/dispose! (:sub snapshot-sample))
            (prn "dispose storage" storage))))
    factory))

;; --

(defn button-component
  [{:keys [onClick]} text]
  (js/console.log "render" text)
  (compile-hiccup
   [:button {:on-click onClick} text]))

;;

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

(defn start-clock
  [{:keys [dispatch _state]} _event]
  (dispatch [:start-clock]))

(defn stop-clock
  [{:keys [dispatch _state]} _event]
  (dispatch [:stop-clock]))

(defn reset-clock
  [{:keys [dispatch _state]} _event]
  (dispatch [:reset-clock]))

(defn buttons-component [{:keys [dispatch state-sub]}]
  (print "render" "buttons")
  (let [action (use-bind-sub dispatch state-sub)]
    (compile-hiccup [:div {:test "foo"}
                                     [button-component
                                      {:on-click (<| action start-clock)}
                                      "Start"]
                                     [button-component
                                      {:on-click (<| action stop-clock)}
                                      "Stop"]
                                     [button-component
                                      {:on-click (<| action reset-clock)}
                                      "Reset"]])))

(defn controls-component
  [{:keys [dispatch state-sub]}]
  (print "render" "controls component")
  [buttons-component {:dispatch dispatch
                      :state-sub state-sub}])

(defn app-clock [{:keys [dispatch state-sub]}]
  (let [counted-seconds  (ratom/cursor state-sub [:clock-state :counted-seconds])
        wages-per-second (ratom/cursor state-sub [:clock-state :wages-per-second])
        funds-raised     (ratom/reaction
                          (format-money (* @wages-per-second @counted-seconds)))]
    [:div
     [:h1 "Hey there Sean 👋"]
     [:h2 "This much time has passed: " (format-time @counted-seconds)]
     [:h2 "And this is how much that we've earned: " @funds-raised]
     [controls-component {:dispatch dispatch
                          :state-sub state-sub}]]))

(def clock-state-default {:status :idle
                          :timer nil
                          :wages-per-second 0.01
                          :counted-seconds 0})

(defonce app-state (r/atom {:clock-state clock-state-default}))

(defonce app-root (rdom-client/create-root
                   (gdom/getElement "root")))

(defn clock-dispatch [clock-state-sub message]
  (let [[message-id] message]
    (case message-id
      :start-clock (swap! clock-state-sub
                          (fn update [state]
                            (merge state
                                   {:status :running
                                    :timer (js/setInterval
                                            (fn []
                                              (let [counted-seconds (ratom/cursor clock-state-sub [:counted-seconds])]
                                                (set! (.. js/document -title)
                                                      (str (format-money (* (:wages-per-second state) @counted-seconds))
                                                           " raised so far"))
                                                (swap! counted-seconds inc)))
                                            1000)})))
      :stop-clock (swap! clock-state-sub merge
                         {:timer (js/clearInterval (:timer @clock-state-sub))
                          :status :idle})
      :reset-clock (swap! clock-state-sub
                          (fn [clock-state]
                            (assoc clock-state
                                   :counted-seconds 0
                                   :wages-per-second (:wages-per-second clock-state-default)))))))

(defn app-dispatch [_app-state-sub message]
  (js/console.log "message" message)

  (print "before" @app-state)

  (let [[message-id] message]
    (when (contains? #{:start-clock :stop-clock :reset-clock} message-id)
      (clock-dispatch (ratom/cursor app-state [:clock-state]) message)))

  (print "after" @app-state))

;; ---

(defn on-click-component
  ;; Comment: 
  ;; * We always expect the event handlers to receive:
  ;;     * readable state
  ;;     * app dispatch
  ;;     * and their related event.
  [{:keys [dispatch state]} event]
  (js/console.log "on-click state" (clj->js state))
  (js/console.log "on-click event" event)
  (when (= 0 (:counter state))
    (js/console.log "init"))
  (|> inc
      (:set-counter state))
  #_(dispatch [:increment]))

(defn example-component [dispatch state-ratom]
  (js/console.log "render" "example component")
  (let [bind (use-bind-sub dispatch state-ratom)
        [counter set-counter] (use-state 0)
        bind-state (use-bind-data dispatch
                                  {:counter counter
                                   :set-counter set-counter})
        click-handler (bind-state on-click-component)
        on-click (use-callback (fn [event]
                                ;;  (dispatch state [:increment])
                                ;;  (dispatch state [:increment])
                                ;;  (dispatch state-ratom [:increment])
                                 (|> event click-handler))
                               [])]
    [:div
     [button-component {:on-click on-click}
      "button one"]
     [button-component {:on-click on-click}
      "button two"]
     counter]))

(defn example-dispatch [state-sub message]
  (let [[message-id] message]
    (js/console.log "message" message-id)
    (when (= message-id :increment)
      (swap! state-sub update-in [:counter] inc))))

(defonce example-state (ratom/atom {:counter 0}))

;; ---

(def functional-compiler (r/create-compiler {:function-components true}))

(r/set-default-compiler! functional-compiler)

(defn ^:export render []
  (println "[main]: render" @example-state)

  (rdom-client/render app-root [app-clock {:dispatch app-dispatch
                                           :state-sub app-state}])
  #_(rdom-client/render app-root [example-component example-dispatch example-state]))

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
  (js/console.log "after")
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
