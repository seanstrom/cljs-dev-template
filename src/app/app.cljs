(ns app.app
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   [reagent.core :as r]
   [reagent.ratom :as ra]
   [reagent.dom.client :as rdom-client]
   [goog.dom :as gdom]))

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
  (let [counted-seconds (ra/cursor app-state [:clock-state :counted-seconds])
        wages-per-second (ra/cursor app-state [:clock-state :wages-per-second])
        funds-raised (ra/reaction
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
                          :wages-per-second 0.0067
                          :counted-seconds 0})

(defonce app-state (r/atom {:clock-state clock-state-default}))

(defonce app-root (rdom-client/create-root
                   (gdom/getElement "root")))

(defn clock-dispatch [message clock-state]
  (let [[message-id] message]
    (case message-id
      :start-clock (swap! clock-state merge
                          {:status :running
                           :timer (js/setInterval
                                   #(swap! (ra/cursor clock-state [:counted-seconds]) inc)
                                   1000)})
      :stop-clock (swap! clock-state merge
                         {:timer (js/clearInterval (:timer @clock-state))
                          :status :idle})
      :reset-clock (reset! (ra/cursor clock-state [:counted-seconds]) 0))))

(defn app-dispatch [message]
  (let [[message-id] message]
    (case message-id
      :start-clock (clock-dispatch message (ra/cursor app-state [:clock-state]))
      :stop-clock (clock-dispatch message (ra/cursor app-state [:clock-state]))
      :reset-clock (clock-dispatch message (ra/cursor app-state [:clock-state])))))

(defn ^:export render []
  (println "[main]: render" @app-state)
  (rdom-client/render app-root [app-clock app-state app-dispatch]))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
