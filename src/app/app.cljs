(ns app.app
  (:require
   [malli.core :as m :refer [=>] :rename {=> sigf}]))

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))

;; ---

(def instance-ref (atom nil))

(defn console-log [location size]
  (let [buffer (js/Uint8Array. (.. @instance-ref -exports -memory -buffer) location size)
        decoder (js/TextDecoder.)
        string (.decode decoder buffer)]
    (js/console.log string)))

(def imports
  #js {:env #js {:consoleLog console-log}})

(defn encode-string [wasm-module content]
  (let [exports (.. wasm-module -instance -exports)
        allocUint8 (.. exports -allocUint8)

        buffer (.encode (js/TextEncoder.) content)
        buffer-length (.-length buffer)
        offset-buffer-length (inc buffer-length)
        pointer (allocUint8 offset-buffer-length)
        slice (js/Uint8Array. (.. exports -memory -buffer)
                              pointer
                              offset-buffer-length)]
    (.set slice buffer)
    (aset slice (.-length buffer) 0)
    {:pointer pointer
     :length buffer-length}))

(defn mem-len [wasm-module pointer]
  (let [exports (.. wasm-module -instance -exports)
        memLen (.. exports -memLen)]
    (memLen pointer)))

(defn pull-string [wasm-module {:keys [pointer length]}]
  (let [exports (.. wasm-module -instance -exports)
        pullString (.. exports -pullString)]
    (let [next-pointer (pullString pointer)
          slice (js/Uint8Array. (.. exports -memory -buffer)
                                next-pointer
                                (mem-len wasm-module next-pointer))
          string (.decode (js/TextDecoder.) slice)]
      string)))

(defn ^:export init []
  (-> (js/fetch "app.wasm")
      (js/WebAssembly.instantiateStreaming imports)
      (.then (fn [wasm-module] (let [instance (.-instance wasm-module)
                                     exports (.-exports instance)]
                                 (js/console.log "wasm-module" wasm-module)
                                 (reset! instance-ref instance)
                                 (js/console.log "exports" exports)
                                 (js/console.log "pull-string" (->> (encode-string wasm-module "helllo")
                                                                    (pull-string wasm-module))))))))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
