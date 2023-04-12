(ns server.server)

(defn ^:export main []
  (prn "Hello World!"))

(defn ^:dev/after-load reload! []
  (prn "Reload CLJS"))
