(ns app.compiler
  (:require
   [hicada.compiler :refer [compile]]))

(defmacro compile-hiccup
  [body]
  (compile body
           {:create-element 'create-element}
           (merge hicada.compiler/default-handlers
                  {:f> (fn [_ f props & children]
                         [f props children])
                   :<> (fn [_ attrs & children]
                            (if (map? attrs)
                              ['Fragment attrs children]
                              ['Fragment {} (cons attrs children)]))})))
