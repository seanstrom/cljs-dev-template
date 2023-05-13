(ns dev.preload
  {:dev/always true}
  (:require
   [malli.dev.cljs :as dev]))

(defn main []
  (println "malli dev start")
  (dev/start!))

(main)
