(ns app.main)

(defn init []
  (println "[main]: init"))

(defn ^:dev/after-load reload []
  (println "[main]: reload"))
