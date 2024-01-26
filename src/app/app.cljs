(ns app.app
  (:require
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!] :rename {<p! await}]
   [malli.core :as m :refer [=>] :rename {=> sigf}]
   [reagent.core :as r]
   [reagent.ratom :as ra]
   [reagent.dom.client :as rdom-client]
   [goog.dom :as gdom]
   ["@spotify/web-api-ts-sdk" :refer [SpotifyApi, AuthorizationCodeWithPKCEStrategy]]
   ["~src/app/icons.js" :as icons]))

(defonce app-state (ra/atom {:paused true}))

(defn load-spotify-playback-sdk []
  (let [script-element (js/document.createElement "script")]
    (set! (.-src script-element) "https://sdk.scdn.co/spotify-player.js")
    (set! (.-async script-element) true)
    (.append js/window.document.body script-element)))

(defn connect-spotify-player [access-token]
  (js/Promise.
   (fn [resolve _reject]
     (let [player (js/window.Spotify.Player.
                   #js {:name "Web Playback SDK"
                        :getOAuthToken (fn [cb] (cb access-token))
                        :volume 0.5})]
       (set! (.-player js/window) player)
       (.addListener player "ready"
                     (fn [^js event]
                       (resolve (.-device_id event))))
       (.addListener player "not_ready"
                     (fn [^js _event]))
       (.addListener player "player_state_changed"
                     (fn [^js _state]))
       (.connect player)))))

(defn load-spotify-device-api [{:keys [access-token]}]
  (js/Promise.
   (fn [resolve _reject]
     (set! (.-onSpotifyWebPlaybackSDKReady js/window)
           #(go (->> (await (connect-spotify-player access-token))
                     resolve)))
     (load-spotify-playback-sdk))))

(defn get-playlist [^js spotify playlist-id]
  (.. spotify -playlists
      (getPlaylist playlist-id)))

(defn play-device-tracks [^js spotify device-id tracks]
  (.. spotify -player
      (startResumePlayback device-id nil (clj->js tracks))))

(defn get-playlist-tracks [^js playlist]
  (js->clj
   (.map (.. playlist -tracks -items)
         (fn [^js item]
           (.. item -track -uri)))))

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
          (await (play-songs spotify device-id (.-id playlist))))))))


(defn set-paused [^js state]
  (swap! app-state assoc :paused (.-paused state)))

(defn toggle-play []
  (go (->> (await (js/window.player.togglePlay))
           set-paused)))

(defn pause-player []
  (go (await (js/window.player.pause))
      (swap! app-state assoc :paused true)))

(defn resume-player []
  (go (await (js/window.player.resume))
      (swap! app-state assoc :paused false)))

(defn app [state]
  [:div {:class "flex gap-2 justify-center items-center flex-1"}
   [:button {:class "btn"
             :on-click #(js/window.player.previousTrack)}
    [:> icons/SkipBackwardIcon]]
   (if @(ra/cursor state [:paused])
     [:button {:class "btn"
               :on-click #(resume-player)}
      [:> icons/PlayIcon]]
     [:button {:class "btn"
               :on-click #(pause-player)}
      [:> icons/PauseIcon]])

   [:button {:class "btn"
             :on-click #(js/window.player.nextTrack)}
    [:> icons/SkipForwardIcon]]])

(defonce app-root (rdom-client/create-root
                   (gdom/getElement "root")))

(defn ^:export render []
  (rdom-client/render app-root [app app-state]))


(defn ^:dev/after-load start []
  (render))

(comment
  (sigf plus1 [:=> [:cat :int] :string])
  (defn plus1 [x] (inc x))

  (plus1 (plus1 0)))
