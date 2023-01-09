(ns app.main
  (:require
   [goog.dom :as gdom]
   [reagent.core :as r]
   [reagent.dom :as dom]
   ["react" :refer [StrictMode]]
   [clojure.string :refer [join]]))

(defonce strict-mode (r/adapt-react-class StrictMode))

(defonce nextTodoId (r/atom 0))
(defonce todos (r/atom (sorted-map)))
(defonce todosFilterState (r/atom :all))

(defonce allFilter #(not (nil? %)))
(defonce activeFilter #(not (:completed %)))
(defonce todosFilters
  {:all allFilter
   :active activeFilter
   :completed :completed})

(defn extractProps [reservedKeys data]
  (let
   [reservedProps (select-keys data reservedKeys)
    props ((partial apply dissoc data) reservedKeys)]
    [reservedProps props]))

(defn styled [element styles]
  (let
   [formatStyle #(->> % (filter identity) (map name) (join ".") keyword)]
    (if (keyword? styles)
      (formatStyle [element styles])
      (formatStyle (cons element styles)))))

(defn Text [data & texts]
  (let
   [element :span
    reservedKeys [:styles :text]]
    (if (map? data)
      (let [[{:keys [styles text]} props] (extractProps reservedKeys data)]
        (into [(styled element styles) props text] texts))
      (into [element data] texts))))

(defn Button [data & children]
  (let
   [element :button.button
    reservedKeys [:styles :text]]
    (if (map? data)
      (let [[{:keys [styles text]} props] (extractProps reservedKeys data)]
        (into [(styled element styles) props text] children))
      (into [element data] children))))

(defn TextInput [{:keys [styles] :as options}]
  (let
   [reservedProps [:styles]
    props ((partial apply dissoc options) reservedProps)
    mergedProps (conj props {:type "text"})]
    [(styled :input.input styles) mergedProps]))

(defn Icon [{:keys [styles icon] :as options}]
  (let
   [reservedProps [:styles :icon]
    props ((partial apply dissoc options) reservedProps)
    iconName (keyword (str "fa-" (name icon)))
    iconText [(styled :i.fas iconName)]]
    [Text (conj {:styles (styled :icon styles) :text iconText} props)]))

(defn Title [data & children]
  (let
   [titleStyle :title
    reservedKeys [:styles]]
    (if (map? data)
      (let [[{:keys [styles]} props] (extractProps reservedKeys data)
            mergedStyles (styled titleStyle styles)
            mergedProps (conj {:styles mergedStyles} props)]
        (into [Text mergedProps] children))
      (into [Text {:styles titleStyle} data] children))))

(defn TopControls [{:keys [onAddTodo onDoubleCheckTodos]}]
  (let
   [todoInput (r/atom "")
    addTodo (fn [] (onAddTodo @todoInput) (reset! todoInput ""))
    updateInput (fn [state event] (reset! state (-> event .-target .-value)))
    editText (partial updateInput todoInput)
    handleEnterKey (fn [event] (when (= "Enter" (.-key event)) (addTodo)))]
    (fn []
      [:div.field.has-addons
       [:div.control
        [Button {:onClick onDoubleCheckTodos}
         [Icon {:icon :check-double}]]]
       [:div.control.is-expanded
        [TextInput {:value @todoInput
                    :placeholder "What needs to be done?"
                    :onChange editText
                    :onKeyDown handleEnterKey}]]
       [:div.control
        [Button {:onClick addTodo}
         [Icon {:icon :plus}]]]])))

(defn FilterControls [{:keys [todosCount todosFilterState onSetFilter]}]
  (let
   [setFilterAll #(onSetFilter :all)
    setFilterActive #(onSetFilter :active)
    setFilterCompleted #(onSetFilter :completed)
    selectedFilter #(when (= % todosFilterState) :is-info)
    todo-status [Text
                 [Text {:styles :has-text-weight-bold} todosCount]
                 [Text " remaining todos"]]
    todo-filters [(styled :div [:field :has-addons :is-align-self-center])
                  [:p.control
                   [Button {:styles (selectedFilter :all)
                            :onClick setFilterAll
                            :text "All"}]]
                  [:p.control
                   [Button {:styles (selectedFilter :active)
                            :onClick setFilterActive
                            :text "Active"}]]
                  [:p.control
                   [Button {:styles (selectedFilter :completed)
                            :onClick setFilterCompleted
                            :text "Completed"}]]]]
    [(styled :div [:mt-3
                   :px-2
                   :is-flex
                   :is-align-items-center
                   :is-justify-content-space-between])
     todo-status
     todo-filters]))

(defn TodoItem [{:keys [id onEditTodo onRemoveTodo onCompleteTodo]}]
  (let
   [editState (r/atom false)
    lockTodo #(reset! editState false)
    unlockTodo #(reset! editState true)
    removeTodo (partial onRemoveTodo id)
    completeTodo (partial onCompleteTodo id)
    editTodo #(onEditTodo id (->> % .-target .-value))]
    (fn [{:keys [todo]}]
      [:div.field.has-addons
       [:div.control
        [Button {:onClick completeTodo}
         [Icon {:icon :check
                :styles (if (todo :completed)
                          :has-text-success
                          :has-text-grey-light)}]]]
       [:div.control.is-expanded
        [TextInput {:styles (when (todo :completed) :has-text-grey-light)
                    :value (todo :content)
                    :onBlur lockTodo
                    :onDoubleClick (when (not (todo :completed)) unlockTodo)
                    :onChange (when @editState editTodo)
                    :readOnly (not @editState)}]]
       [:div.control
        [Button {:onClick removeTodo}
         [Icon {:icon :xmark :styles :has-text-danger}]]]])))

(defn TodoList [{:keys [todos nextTodoId todosFilterState]}]
  (let
   [addTodo
    (fn [todo]
      (swap! todos #(assoc @todos @nextTodoId {:content todo :completed false}))
      (swap! nextTodoId inc))

    editTodo
    (fn [todoId content]
      (let [todo (get @todos todoId)
            updatedTodo (assoc todo :content content)]
        (swap! todos #(assoc @todos todoId updatedTodo))))

    removeTodo
    (fn [todoId]
      (swap! todos #(dissoc @todos todoId)))

    markTodo
    (fn [todo state]
      (assoc todo :completed state))

    completeTodo
    (fn [todoId]
      (let [todo (get @todos todoId)
            updatedTodo (markTodo todo (not (todo :completed)))]
        (swap! todos #(assoc @todos todoId updatedTodo))))

    doubleCheckTodos
    (fn []
      (if (every? #(->> % second :completed) @todos)
        (let
         [updatedTodos (map (fn [[id todo]] [id (markTodo todo false)]) @todos)]
          (reset! todos updatedTodos))
        (let
         [updatedTodos (map (fn [[id todo]] [id (markTodo todo true)]) @todos)]
          (reset! todos updatedTodos))))

    setFilter
    (fn [filterState]
      (reset! todosFilterState filterState))]

    (fn []
      (let
       [activeFilter (todosFilters :active)
        filterFunc (todosFilters @todosFilterState)
        filteredTodos (filter #(->> % second filterFunc) @todos)
        todosCount (count (filter #(->> % second activeFilter) @todos))]
        [:div.box
         [TopControls {:onAddTodo addTodo
                       :onDoubleCheckTodos doubleCheckTodos}]
         [:div.is-grouped
          (doall
           (for [[id todo] filteredTodos]
             ^{:key id}
             [TodoItem
              {:id id
               :todo todo
               :onEditTodo editTodo
               :onRemoveTodo removeTodo
               :onCompleteTodo completeTodo}]))]
         [FilterControls
          {:onSetFilter setFilter
           :todosCount todosCount
           :todosFilterState @todosFilterState}]]))))

(defn App [todos nextTodoId todosFilterState]
  [(styled :div [:container
                 :is-flex
                 :is-flex-grow-1
                 :is-flex-direction-column
                 :has-background-light
                 :p-6])
   [Title {:styles :has-text-centered.is-size-1 :text "Todos"}]
   [TodoList {:todos todos
              :nextTodoId nextTodoId
              :todosFilterState todosFilterState}]])

(defn ^:export render []
  (println "[main]: render")
  (let
   [nodeId "root"
    element [strict-mode [App todos nextTodoId todosFilterState]]]
    (dom/render element (gdom/getElement nodeId))))
