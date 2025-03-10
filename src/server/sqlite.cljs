(ns server.sqlite
  (:require
   ["bun:sqlite" :as bun-sqlite]))

(defprotocol Driver
  (connect [self])
  (execute [self query-string params])
  (transaction [self tx-fn]))

(defrecord BunSQLiteDriver [db-config]
  Driver
  (connect [self]
    (let [connection (new bun-sqlite/Database (:name db-config))]
      (tap> self)
      (assoc self :connection connection)))
  (execute [self query-string params]
    (let [prepared-statement (.query (:connection self) query-string)
          results (.apply (.-all prepared-statement)
                          prepared-statement
                          params)]
      results))
  (transaction [self tx-fn]
    (let [transaction (.transaction (:connection self) (fn [tx-db] (tx-fn tx-db)))]
      (transaction self))))

(defn open
  [db-config]
  (-> db-config ->BunSQLiteDriver connect))
