(ns server.server
  (:require
   ["uuid" :refer [v7] :rename {v7 uuid-v7}]
   [server.sqlite :as sqlite]
   [clojure.string]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [honey.sql :as sql]
   [walkable.core :as walkable]
   [malli.core :as m :refer [=>] :rename {=> sigf}]))

(defn ^:export main [timeout]
  (prn "Hello World!")
  (js/setInterval #() timeout))

(defn ^:dev/after-load reload! []
  (prn "Reload CLJS"))

(comment
  (sigf plus1 [:=> [:cat :int] :int])
  (defn plus1 [x] (inc x))
  (plus1 (plus1 0)))

;; Utils

(defn |>
  [arg & fns]
  (if (= 1 (count fns))
    ((first fns) arg)
    (|> arg (apply comp (reverse fns)))))

(comment
  (-> [{:items [{:id 1} {:id 2}]}
       {:items [{:id 3} {:id 4}]}]
      (|> (partial mapcat :items)
          (partial map :id))))

(defn kebab-case->snake-case
  [attribute]
  (-> attribute
      name
      (clojure.string/replace #"-(\w)"
                              #(str "_" (second %1)))))

(comment
  (kebab-case->snake-case :test-case))

;; Decoders

(defn decode-js
  [^js js-obj]
  (js->clj js-obj :keywordize-keys true))

(comment
  (decode-js #js{"idea/id" 1
                 "idea/text" "hello"}))

(defn decode-index
  "Decode the index of a JS object with a custom decoder.
   Note that the index is accessed using the JS array index operator `[]`.

   For example:
   The CLJS code `(decode-index identity #js{:id 1} (name :id))` is the same as
   the JS code `{id: 1}['id']`."
  [decoder index js-obj]
  (-> (aget js-obj index)
      decoder))

(comment
  (decode-index identity 0 (to-array [:a :b :c]))
  (decode-index identity "text" #js{"text" "hello world"}))

(defn format-attribute-name
  [name-fn attribute-key]
  (let [attribute-name (name-fn attribute-key)]
    (cond
      (string? attribute-name) attribute-name
      (keyword? attribute-name) (-> attribute-name symbol str)
      (nil? attribute-name) (-> attribute-key symbol str)
      :else attribute-name)))

(comment
  (format-attribute-name identity :id)
  (format-attribute-name identity "id")
  (format-attribute-name identity :idea/id)
  (format-attribute-name identity "idea/id")
  (format-attribute-name {:idea/id "ideaId"} :idea/id)
  (format-attribute-name {:idea/id :ideaId} :idea/id)
  (format-attribute-name {:idea/id :ideaId} :idea/text))

(defn decode-entity
  "Decode a JS object into Entity data based on the desired attributes.
   
   Note that a custom decoder can be used to decode all entity-attribute values.
   The custom decoder can also be defined as a map so that each entity-attribute
   value can be decoded based on the attribute key.
   
   Additionally, an attribute
   rename function can be provided to override the lookup name for an attribute
   when decoding the attribute value."
  ([decoder attributes rename-fn ^js js-obj]
   (|> [{} (if (map? attributes)
             (keys attributes)
             attributes)]
       (partial apply reduce
                (fn [entity-data attribute-key]
                  (assoc entity-data
                         attribute-key
                         (decode-index (if (map? decoder)
                                         (get decoder attribute-key identity)
                                         decoder)
                                       (format-attribute-name (if (map? attributes)
                                                                (comp rename-fn attributes)
                                                                rename-fn)
                                                              attribute-key)
                                       js-obj))))))

  ([decoder attributes ^js js-obj]
   (decode-entity decoder attributes identity js-obj))
  ([attributes ^js js-obj]
   (decode-entity identity attributes js-obj)))

(comment
  (decode-entity [:idea/id]
                 #js{"idea/id" 1
                     "idea/text" "hello"})
  (decode-entity {:idea/id "id"}
                 #js{"id" 1
                     "idea/text" "hello"})
  (decode-entity identity
                 [:idea/id]
                 #js{"idea/id" 1
                     "idea/text" "hello"})
  (decode-entity identity
                 {:idea/id "id"}
                 #js{"id" 1
                     "idea/text" "hello"})
  (decode-entity str
                 [:idea/id]
                 #js{"idea/id" 1
                     "idea/text" "hello"})
  (decode-entity str
                 {:idea/id "id"}
                 #js{"id" 1
                     "idea/text" "hello"})
  (decode-entity {:idea/id str
                  :idea/text vec}
                 [:idea/id :idea/text]
                 #js{"idea/id" 1
                     "idea/text" "hello"})
  (decode-entity {:idea/id str
                  :idea/text vec}
                 {:idea/id "id"
                  :idea/text "text"}
                 #js{"id" 1
                     "text" "hello"})
  (decode-entity identity
                 [:idea/id :idea/text]
                 {:idea/id :ideaId}
                 #js{"ideaId" 1
                     "idea/text" "hello"})
  (decode-entity identity
                 {:idea/id :idea-id
                  :idea/text "idea/text"}
                 {:idea-id :ideaId}
                 #js{"ideaId" 1
                     "idea/text" "hello"})
  (decode-entity str
                 [:idea/id :idea/text]
                 {:idea/id :id}
                 #js{"id" 1
                     "idea/text" "hello"})
  (decode-entity str
                 {:idea/id :idea-id
                  :idea/text "idea/text"}
                 {:idea-id :ideaId}
                 #js{"ideaId" 1
                     "idea/text" "hello"})
  (decode-entity {:idea/id str
                  :idea/text vec}
                 [:idea/id :idea/text]
                 {:idea/id :id}
                 #js{"id" 1
                     "idea/text" "hello"})
  (decode-entity {:idea/id str
                  :idea/text vec}
                 {:idea/id :idea-id
                  :idea/text "idea/text"}
                 {:idea-id :ideaId}
                 #js{"ideaId" 1
                     "idea/text" "hello"}))

(defn decode-array-lazy-helper
  [decoder array start-index end-index]
  (if (empty? array)
    (sequence nil)
    (let [item (decode-index decoder start-index array)]
      (if (= start-index end-index)
        (list item)
        (lazy-seq (cons item
                        (decode-array-lazy-helper decoder
                                                  array
                                                  (inc start-index)
                                                  end-index)))))))

(comment
  (decode-array-lazy-helper identity (to-array [1 2 3]) 0 2)
  (realized? (decode-array-lazy-helper identity (to-array [1 2 3]) 0 2)))

(defn decode-array-eager-helper
  [decoder array start-index end-index]
  (loop [current-index end-index
         result (sequence nil)]
    (let [item (decode-index decoder current-index array)
          next-result (cons item result)]
      (if (= current-index start-index)
        next-result
        (recur (dec current-index)
               next-result)))))

(comment
  (decode-array-eager-helper identity (to-array [1 2 3]) 0 2))

(defn base-decode-array
  ([driver decoder array & params]
   (let [start-index (or (first params) 0)
         end-index (or (second params) (-> array count dec))]
     (driver decoder array start-index end-index)))
  ([driver array]
   (base-decode-array driver identity array)))

(comment
  (base-decode-array decode-array-lazy-helper (to-array [1 2 3]))
  (base-decode-array decode-array-lazy-helper identity (to-array [1 2 3]))
  (base-decode-array decode-array-lazy-helper identity (to-array [1 2 3]) 1)
  (base-decode-array decode-array-lazy-helper identity (to-array [1 2 3]) 1 1)
  (realized? (base-decode-array decode-array-lazy-helper (to-array [1 2 3])))

  (base-decode-array decode-array-eager-helper (to-array [1 2 3]))
  (base-decode-array decode-array-eager-helper identity (to-array [1 2 3]))
  (base-decode-array decode-array-eager-helper identity (to-array [1 2 3]) 1)
  (base-decode-array decode-array-eager-helper identity (to-array [1 2 3]) 1 1))

(def decode-array-lazy (partial base-decode-array decode-array-lazy-helper))

(comment
  (decode-array-lazy (to-array [1 2 3]))
  (decode-array-lazy identity (to-array [1 2 3]))
  (decode-array-lazy identity (to-array [1 2 3]) 1)
  (decode-array-lazy identity (to-array [1 2 3]) 1 1)
  (realized? (decode-array-lazy (to-array [1 2 3]))))

(def decode-array-eager (partial base-decode-array decode-array-eager-helper))

(comment
  (decode-array-eager (to-array [1 2 3]))
  (decode-array-eager identity (to-array [1 2 3]))
  (decode-array-eager identity (to-array [1 2 3]) 1)
  (decode-array-eager identity (to-array [1 2 3]) 1 1))

(defn decode-array
  [& args]
  (let [mode (first args)
        decode-array-default decode-array-lazy]
    (if (keyword? mode)
      (apply (case mode
               :lazy decode-array-lazy
               :eager decode-array-eager
               decode-array-default)
             (rest args))
      (apply decode-array-default args))))

(comment
  (decode-array (to-array [1 2 3]))
  (realized? (decode-array (to-array [1 2 3])))

  (decode-array :lazy (to-array [1 2 3]))
  (realized? (decode-array :lazy (to-array [1 2 3])))

  (decode-array :eager (to-array [1 2 3])))

(defn decode-idea
  [^js js-obj]
  (-> {:idea/id :id
       :idea/text :text}
      (decode-entity js-obj)))

(comment
  (decode-idea #js{:id 1
                   :text "hello world"}))

(defn decode-ideas
  [js-obj-seq]
  (transduce (map decode-idea)
             conj
             []
             js-obj-seq))

(comment
  (decode-ideas #js[#js{:id 1
                        :text "text"}
                    #js{:id 2
                        :text ""}]))

(defn make-transducer-step
  [f]
  (fn
    ([acc] acc)
    ([acc item] (f acc item))))

(comment
  (transduce (map decode-idea)
             (make-transducer-step
              (fn [acc item]
                (-> acc
                    (update :count inc)
                    (update :items assoc (:idea/id item) item))))
             {:count 0
              :items {}}
             (decode-array :lazy
                           identity
                           #js[#js{:id 1
                                   :text "text"}
                               #js{:id 2
                                   :text ""}])))

(defn decode-ideas-alt
  [js-obj-seq]
  (transduce (map decode-idea)
             (make-transducer-step
              (fn [acc item]
                (-> acc
                    (update :count inc)
                    (update :items assoc (:idea/id item) item))))
             {:count 0
              :items {}}
             js-obj-seq))

(comment
  (decode-ideas-alt
   (decode-array :lazy
                 identity
                 #js[#js{:id 1
                         :text "text"}
                     #js{:id 2
                         :text ""}])))

;; Database JSON Helpers

(defn encode-param
  [param]
  (cond (keyword? param) (name param)
        (vector? param) (str "'"
                             (->> (map name param)
                                  (clojure.string/join "."))
                             "'")
        :else (name param)))

(defn json-extract
  [json-field json-path]
  (let [[json-fn & params] [:json-extract json-field json-path]]
    [:raw
     (str (kebab-case->snake-case json-fn)
          "("
          (transduce (map encode-param)
                     (fn
                       ([acc]
                        (clojure.string/join ", " acc))
                       ([acc item]
                        (conj acc item)))
                     []
                     params)
          ")")]))

(comment
  (json-extract :doc [:$ :age]))

(defn json-lookup
  [json-field json-path]
  (let [[json-fn field value] [:->> json-field json-path]]
    [:raw
     (transduce (map encode-param)
                (fn
                  ([acc]
                   (clojure.string/join (str (name json-fn))
                                        acc))
                  ([acc item]
                   (conj acc item)))
                []
                [field value])]))

(comment
  (json-lookup :doc [:$ :name]))

;; Database Queries

(def db-config
  {:name "app.db"
   :location "dream/db"})

(defn db-query
  ([db decoder query]
   (let [[query-string & params] query]
     (decoder (sqlite/execute db query-string (to-array params)))))
  ([db query]
   ;; NOTE(seanstrom):
   ;; The default implementation uses `decode-array-lazy` which creates a lazy-seq
   ;; over the realized array of results. This allows the consumer of the query
   ;; to incrementally decode the results array without an intermediary seq.
   (db-query db decode-array-lazy query)))

(defn db-transaction
  [db tx-fn]
  (sqlite/transaction db (fn [tx] (tx-fn tx (uuid-v7)))))

(comment
  (-> (sqlite/open db-config)
      (db-query
       (sql/format {:select [:*]
                    :from [:idea]}))
      (decode-ideas))

  (sql/format {:select [[(json-lookup :doc [:$ :entity]) :entity]
                        [(json-lookup :doc [:$ :attribute]) :attribute]
                        [(json-lookup :doc [:$ :value]) :value]]
               :from :documents
               :where [:= (json-lookup :doc [:$ :entity]) 1001]})

  (-> (sqlite/open db-config)
      (db-query (sql/format {:select [[(json-lookup :doc [:$ :entity]) (-> :fact/entity symbol str)]
                                      [(json-lookup :doc [:$ :attribute]) (-> :fact/attribute symbol str)]
                                      [(json-lookup :doc [:$ :value]) (-> :fact/value symbol str)]]
                             :from :stuff
                             :where [:= (json-lookup :doc [:$ :entity]) 1001]}))
      (|> (partial map (partial decode-entity
                                [:fact/entity :fact/attribute :fact/value]))))

  (-> (sqlite/open db-config)
      (db-transaction (fn [tx-db tx-id]
                        (-> tx-db
                            (db-query
                             (sql/format {:insert-into [:tx]
                                          :values [{:tx-id tx-id}]})))
                        (-> tx-db
                            (db-query
                             (sql/format {:insert-into [:info]
                                          :values [{:entity 200
                                                    :attribute "favorite-movie"
                                                    :value "The Boy and the Heron"
                                                    :tx tx-id}
                                                   {:entity 200
                                                    :attribute "favorite-movie-studio"
                                                    :value "Studio Ghibli"
                                                    :tx tx-id}]})))))
      (.then print)
      (.catch print)))

;; SQL Schema

(defn create-idea-table
  [db]
  (->> (sql/format
        {:create-table [:idea :if-not-exists]
         :with-columns [[:id :integer [:not nil] :primary-key]
                        [:text :text [:not nil]]]})
       (db-query db)))

(defn create-info-table
  [db]
  (->> (sql/format
        {:create-table [:info :if-not-exists]
         :with-columns [[:id :integer [:not nil] :primary-key]
                        [:entity :integer [:not nil]]
                        [:attribute :text [:not nil]]
                        [:value :any [:not nil]]
                        [:tx [:varchar 36]]
                        [:op :int [:not nil] [:default "true"]]]})
       (db-query db)))  

(defn create-document-table
  [db]
  (->> (sql/format
        {:create-table [:document :if-not-exists]
         :with-columns [[:id :integer [:not nil] :primary-key]
                        [:entity :integer [:not nil]]
                        [:doc :text [:not nil]]
                        [:tx [:varchar 36]]]})
       (db-query db)))

(defn create-blob-table
  [db]
  (->> (sql/format
        {:create-table [:blob :if-not-exists]
         :with-columns [[:id :integer [:not nil] :primary-key]
                        [:entity :integer [:not nil]]
                        [:data :blob [:not nil]]
                        [:tx [:varchar 36]]]})
       (db-query db)))

(defn create-tx-table
  [db]
  (->>
   (sql/format
    {:create-table [:tx :if-not-exists]
     :with-columns [[:id :integer [:not nil] :primary-key]
                    [:tx-id [:varchar 36] [:not nil]]
                    [:tx-time :timestamp [:not nil] [:default :current-timestamp]]]})
   (db-query db)))

(defn create-info-eavt-index
  [db]
  (->> (sql/format
        {:create-index [[:info-eavt-index :if-not-exists]
                        [:info :entity :attribute :value :tx]]})
       (db-query db)))

(defn create-info-avet-index
  [db]
  (->> (sql/format
        {:create-index [[:info-avet-index :if-not-exists]
                        [:info :attribute :value :entity :tx]]})
       (db-query db)))

(comment
  (-> (sqlite/open db-config)
      (create-idea-table))
  (-> (sqlite/open db-config)
      (create-info-table))
  (-> (sqlite/open db-config)
      (create-document-table))
  (-> (sqlite/open db-config)
      (create-blob-table))
  (-> (sqlite/open db-config)
      (create-tx-table)))

(comment
  (-> (sqlite/open db-config)
      (create-info-eavt-index))
  (-> (sqlite/open db-config)
      (create-info-avet-index)))

;; Walkable

(def registry
  [{:key :idea/list
    :type :root
    :table "idea"
    :output [:idea/id :idea/text]}
   {:key :stuff/list
    :type :root
    :table "stuff"
    :output [:stuff/id
             :stuff/entity
             :stuff/attribute
             :stuff/value]}
   {:key :info/list
    :type :root
    :table "info"
    :output [:info/id
             :info/entity
             :info/attribute
             :info/value]}])

(defn query-env
  [env query]
  (let [decode-item (partial decode-entity (:query env))
        decode-results (partial decode-array :lazy decode-item)]
    (db-query (:db env) decode-results query)))

(def walkable-parser
  (p/parser
   {::p/env {::p/reader [p/map-reader
                         pc/reader3
                         pc/open-ident-reader
                         p/env-placeholder-reader]}
    ::p/plugins [(pc/connect-plugin {::pc/register []})
                 (walkable/connect-plugin {:db-type :sqlite
                                           :registry registry
                                           :query-env query-env})
                 p/elide-special-outputs-plugin
                 p/error-handler-plugin]}))

(comment
  (walkable-parser {:db (sqlite/open db-config)}
                   [{:idea/list [:idea/text
                                 :idea/id]}])
  (walkable-parser {:db (sqlite/open db-config)}
                   [{:stuff/list [:stuff/id
                                  :stuff/entity
                                  :stuff/attribute
                                  :stuff/value]}])
  (walkable-parser {:db (sqlite/open db-config)}
                   [{:info/list [:info/id
                                 :info/entity
                                 :info/attribute
                                 :info/value]}]))

;; View

(defn view-info
  [info]
  (clojure.string/join " : " [(:info/entity info)
                              (:info/attribute info)
                              (:info/value info)]))

(defn kebab-case->camel-case
  [attribute]
  (-> attribute
      name
      (clojure.string/replace #"-(\w)"
                              #(clojure.string/upper-case (second %1)))))

(defn encode-props
  [props]
  (clj->js props {:keyword-fn kebab-case->camel-case}))

(comment
  (encode-props {:on-press identity}))
