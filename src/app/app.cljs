(ns app.app
  (:require
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!] :rename {<p! await}]
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   ["lodash.shuffle" :as shuffle]
   ["@spotify/web-api-ts-sdk" :refer [SpotifyApi, AuthorizationCodeWithPKCEStrategy]]))

(defn load-spotify-playback-sdk []
  (let [script-element (js/document.createElement "script")]
    (set! (.-src script-element) "https://sdk.scdn.co/spotify-player.js")
    (set! (.-async script-element) true)
    (.append js/window.document.body script-element)))

(defn load-spotify-device-api [{:keys [access-token]}]
  (load-spotify-playback-sdk)
  (js/Promise. (fn [resolve _reject]
                 (set! (.-onSpotifyWebPlaybackSDKReady js/window)
                       (fn []
                         (let [player (js/window.Spotify.Player.
                                       #js {:name "Web Playback SDK"
                                            :getOAuthToken (fn [cb] (cb access-token))
                                            :volume 0.5})]
                           (set! (.-player js/window) player)
                           (.addListener player "ready" (fn [^js event]
                                                          (let [device-id (.-device_id event)
                                                                event #js {:detail #js {:deviceId device-id}}]
                                                            (js/dispatchEvent
                                                             (js/CustomEvent. "device-ready" event))
                                                            (resolve device-id))))
                           (.addListener player "not_ready" (fn [^js event]
                                                              (let [event #js {:detail #js {:deviceId (.-device_id event)}}]
                                                                (js/dispatchEvent
                                                                 (js/CustomEvent. "device-not-ready" event)))))
                           (.addListener player "player_state_changed" (fn [^js state]
                                                                         (let [event #js{:detail #js{:state state}}]
                                                                           (js/dispatchEvent
                                                                            (js/CustomEvent. "device-player-state" event)))))
                           (.addEventListener js/window "device-ready" (fn [event]
                                                                         (.. event -detail -deviceId)))
                           (.connect player)))))))

(defn get-playlist [^js spotify playlist-id]
  (.. spotify -playlists
      (getPlaylist playlist-id)))

(defn play-device-tracks [^js spotify device-id tracks]
  (.. spotify -player
      (startResumePlayback device-id nil tracks)))

(defn get-playlist-tracks [^js playlist]
  (.map (.. playlist -tracks -items)
        (fn [^js item]
          (.. item -track -uri))))

(defn play-songs [^js spotify device-id playlist-id]
  (go
    (->> (await (get-playlist spotify playlist-id))
         get-playlist-tracks
         shuffle
         (play-device-tracks spotify device-id))))

(defn get-access-token [^js spotify]
  (-> (.getAccessToken spotify)
      (.then (fn [^js token] (.-access_token token)))))

(defn find-discover-weekly [^js spotify]
  (-> (.search spotify "Discover Weekly" #js["playlist"])
      (.then (fn [^js response]
               (.find
                (.-items (.-playlists response))
                (fn [^js item]
                  (and (= (.-name item) "Discover Weekly")
                       (= (.-display_name (.-owner item)) "Spotify"))))))))

(defn ^:export boot [^js env]
  (let [implicitGrantStrategy (AuthorizationCodeWithPKCEStrategy.
                               (.. env -VITE_SPOTIFY_CLIENT_ID)
                               (.. env -VITE_REDIRECT_TARGET)
                               #js[; NOTE: Needed for both Search API and Web Playback SDK
                                   "user-read-email"
                                   "user-read-private"

                                   ; NOTE: Needed for Search API
                                   "user-library-read"
                                   "playlist-read-private"

                                   ; NOTE: Needed for the Web Playback SDK to work.
                                   "streaming"
                                   "user-read-playback-state"
                                   "user-modify-playback-state"])
        spotify (SpotifyApi. implicitGrantStrategy)]
    (go
      (await (.authenticate spotify))
      (let [access-token (await (get-access-token spotify))
            device-id (await (load-spotify-device-api {:access-token access-token}))
            playlist (await (find-discover-weekly spotify))]
        (when (and playlist device-id)
          (play-songs spotify device-id (.-id playlist)))))))

(defn ^:export render []
  (let [change-me "Hello"

        node (js/document.getElementById "root")
        text-node (js/document.createTextNode change-me)]
    (.appendChild node text-node))

  (println "[main]: render"))

(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
