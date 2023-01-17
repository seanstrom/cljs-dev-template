(ns app.main
  (:require
   [clojure.set]
   [clojure.string]
   [goog.dom :as gdom]
   [reagent.core :as r]
   [reagent.dom :as dom]
   ["react" :refer [StrictMode]]
   ["reactflow" :as reactflow :refer [Background Controls ReactFlow useNodesState, useEdgesState]]))

(defonce rf-background (r/adapt-react-class Background))
(defonce rf-main (r/adapt-react-class ReactFlow))
(defonce rf-controls (r/adapt-react-class Controls))
(defonce strict-mode (r/adapt-react-class StrictMode))

(defonce graphInput [[:a :b] [:a :c] [:c :b] [:a :e]
                     [:b :e] [:a :d] [:b :d] [:c :e]
                     [:d :e] [:c :f] [:d :f]])

(defonce nodePositions
  {:a {:x 109 :y 428}
   :b {:x 309 :y 805}
   :c {:x 226 :y 91}
   :d {:x 759 :y 388}
   :e {:x 744 :y 695}
   :f {:x 629 :y 70}})

(defn classes [& names]
  clojure.string/join (map name (filter identity names)))

(defn makeNodeId [node]
  (str "node-" node))

(defn makeEdgeId [edge]
  (str "edge-" edge))

(defn formatNode [node]
  {:id (makeNodeId node)
   :position (nodePositions node)
   :data {:label (str node)}})

(defn formatEdge
  ([edge] (formatEdge edge false))
  ([edge isVisited?]
   {:id (makeEdgeId edge)
    :source (makeNodeId (first edge))
    :target (makeNodeId (second edge))
    :className (classes :edge (when isVisited? :visited))}))

(defn makeGraph [input]
  (reduce (fn [[table nodes edges] [origin destination]]
            [(if (= origin destination) table
                 (merge-with clojure.set/union table
                             (assoc {} origin #{destination})
                             (assoc {} destination #{origin})))
             (conj nodes origin destination)
             (conj edges #{origin destination})])
          [{} #{} #{}]
          input))

(defn makeProps [graph]
  (let [[_ nodes edges] graph
        formattedNodes (map formatNode nodes)
        formattedEdges (map formatEdge edges)]
    [formattedNodes formattedEdges]))

(defonce graphData (makeGraph graphInput))
(defonce initialData (makeProps graphData))

;; Search

(defn makeSearchItem
  ([node] [node [] #{}])
  ([node path visited] [node path visited]))

(defn makeNextItems [graph item]
  (let
   [[table _ allEdges] graph
    [node path visited] item
    [nextPath nextVisited]
    (cond
      (empty? path)
      [(conj path [node]) visited]

      (= 1 (count (peek path)))
      [(conj (pop path) (conj (peek path) node))
       (conj visited (set (conj (peek path) node)))]

      :else
      (let [prevStep (peek path)
            step [(second prevStep) node]
            edge (set step)]
        [(conj path step) (conj visited edge)]))]

    (->> (get table node)
         (map #(makeSearchItem % nextPath nextVisited))
         (filter (fn [[node path visited]]
                   (let [possibleStep (hash-set (->> path peek second) node)
                         possibleVisited (conj visited possibleStep)]
                     (and (not (contains? visited possibleStep))
                          (if (and (= (ffirst path) node)
                                   (not= possibleVisited allEdges))
                            false
                            true))))))))

(defn initSearch [graph]
  (let [[_ allNodes _] graph]
    {:steps 0
     :graph graph
     :results []
     :pile (into [] (map makeSearchItem allNodes))}))

(defn cycle? [graph path visited]
  (let
   [[_ _ allEdges] graph
    originNode (ffirst path)
    finalNode (->> path peek second)]
    (and (= originNode finalNode) (= visited allEdges))))

(defn updateSearch [{:keys [graph pile steps results] :as state}]
  (cond
    (empty? pile)
    state

    (not-empty results)
    state

    :else
    (let
     [item (peek pile)
      [node path visited] item
      [_ currentNode] (peek path)
      nextEdge (hash-set currentNode node)
      nextPath (conj path [currentNode node])
      nextVisited (conj visited nextEdge)]

      (cond
        (contains? visited nextEdge)
        (conj state
              {:steps (inc steps)
               :pile (pop pile)})

        (cycle? graph nextPath nextVisited)
        (conj state
              {:steps (inc steps)
               :results (conj results nextPath)
               :pile (conj (pop pile)
                           [(first (peek pile)) nextPath nextVisited])})

        :else
        (conj state
              {:steps (inc steps)
               :pile (into (pop pile) (makeNextItems graph item))})))))

(defn fillPile [{:keys [graph pile] :as state}]
  (loop [input pile
         output []]
    (cond
      (empty? input)
      (conj state {:pile output})

      :else
      (let
       [item (first input)
        [_ path _] item]
        (if (= 0 (count path))
          (recur
           (subvec input 1)
           (into output (makeNextItems graph item)))
          (recur
           (into (vec (rest input)) (makeNextItems graph item))
           output))))))

;; View

(defn main-ui []
  (let
   [[initialNodes initialEdges] initialData
    searchState (r/atom (fillPile (initSearch graphData)))

    updateEdges
    (fn [searchState setEdges]
      (let
       [state (updateSearch @searchState)
        [_ _ allEdges] (state :graph)
        [_ _ visited] (peek (state :pile))
        nextEdges (map #(formatEdge % (contains? visited %)) allEdges)]
        (setEdges (clj->js nextEdges))
        (println (clj->js (state :pile)))
        (reset! searchState state)))]
    (fn []
      (let [[nodes _ onNodesChange] (useNodesState (clj->js initialNodes))
            [edges setEdges onEdgesChange] (useEdgesState (clj->js initialEdges))]
        [:div {:style {:height "100%"}}
         [:div.search
          [:div.buttons
           [:button.button {:onClick #(updateEdges searchState setEdges)} "Next Step"]]]
         [rf-main {:nodes nodes :edges edges :onNodesChange onNodesChange :onEdgesChange onEdgesChange}
          [rf-background]
          [rf-controls]]]))))

(defn ^:export render []
  (println "[main]: render")
  (let
   [nodeId "root"
    element [strict-mode [:f> main-ui]]]
    (dom/render element (gdom/getElement nodeId))))
