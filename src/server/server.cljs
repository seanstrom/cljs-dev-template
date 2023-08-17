(ns server.server
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require
   [hiccups.runtime]
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   ["fastify" :as fastify]))

(defn health []
  #js{:success true})

(defn homepage []
  [:html
   [:head
    [:title "Hello from ClojureScript!"]
    [:script {:src "https://unpkg.com/htmx.org@1.9.2"}]]
   [:body
    [:h1 "Hello from ClojureScript!"]
    [:button {:hx-get "/health"
              :hx-trigger "click"
              :hx-target "#health"
              :hx-swap "innerHTML"}
     "Check Health"]
    [:label "Current Health: "]
    [:span {:id "health"}]]])

(defn router [^js app]
  (.get app "/"
        (fn [_ reply]
          (.type reply "text/html")
          (.send reply (html (homepage)))))
  (.get app "/health"
        (fn [_ reply]
          (.type reply "application/json")
          (.send reply (js/JSON.stringify (health)))))
  app)

(defonce server (volatile! nil))

(defn start-server []
  (let [app (fastify/default #js{:logger true})]
    (.listen (router app) #js{:port 8000} (fn [_err]
                                            (js/console.log "Server started", _err)
                                            (vreset! server app)))))

(defn ^:dev/after-load start! []
  (js/console.warn "Starting server")
  (start-server))

(defn ^:dev/before-load-async stop! [done]
  (js/console.warn "Stopping server")
  (when-some [svr @server]
    (.close svr
            (fn [err]
              (js/console.warn "Server stopped" err)
              (done)))))

(defn ^:export main []
  (start-server)
  (prn "Hello World!"))

(defn ^:dev/after-load reload! []
  (prn "Reload CLJS"))

(comment
  (sigf plus1 [:=> [:cat :int] :int])
  (defn plus1 [x] (inc x))
  (plus1 (plus1 0)))
