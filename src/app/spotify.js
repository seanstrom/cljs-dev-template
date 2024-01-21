import { SpotifyApi, AuthorizationCodeWithPKCEStrategy } from "@spotify/web-api-ts-sdk";
import shuffle from "lodash.shuffle";

function loadSpotifyDeviceAPI({ accessToken }) {
  const script = document.createElement("script");
  script.src = "https://sdk.scdn.co/spotify-player.js";
  script.async = true;
  window.document.body.append(script);

  return new Promise((resolve, _reject) => {
    window.onSpotifyWebPlaybackSDKReady = () => {
      const player = (window.player = new Spotify.Player({
        volume: 0.5,
        name: "Web Playback SDK",
        getOAuthToken: (cb) => cb(accessToken),
      }));

      player.addListener("ready", ({ device_id }) => {
        dispatchEvent(
          new CustomEvent("device-ready", {
            detail: { deviceId: device_id },
          })
        );
      });

      player.addListener("not_ready", ({ device_id }) => {
        dispatchEvent(
          new CustomEvent("device-not-ready", {
            detail: { deviceId: device_id },
          })
        );
      });

      player.addListener("player_state_changed", (state) => {
        dispatchEvent(
          new CustomEvent("device-player-state", {
            detail: { state },
          })
        );
      });

      window.addEventListener("device-ready", (event) => {
        resolve(event.detail?.deviceId);
      });

      player.connect();
    };
  });
}

async function playSongs(spotify, deviceId, playlistId) {
  const playlist = await spotify.playlists.getPlaylist(playlistId);
  const tracks = playlist.tracks.items.map((item) => item.track.uri);
  await spotify.player.startResumePlayback(
    deviceId,
    undefined,
    shuffle(tracks)
  );
}

export async function boot(env) {
  const implicitGrantStrategy = new AuthorizationCodeWithPKCEStrategy(
    env.VITE_SPOTIFY_CLIENT_ID,
    env.VITE_REDIRECT_TARGET,
    [
      // NOTE: Needed for both Search API and Web Playback SDK
      "user-read-email",
      "user-read-private",

      // NOTE: Needed for Search API
      "user-library-read",
      "playlist-read-private",

      // NOTE: Needed for the Web Playback SDK to work.
      "streaming",
      "user-read-playback-state",
      "user-modify-playback-state",
    ]
  );

  const spotify = new SpotifyApi(implicitGrantStrategy);
  await spotify.authenticate();

  const accessToken = await spotify
    .getAccessToken()
    .then((token) => token?.access_token);

  const deviceId = await loadSpotifyDeviceAPI({ accessToken });
  const response = await spotify.search("Discover Weekly", ["playlist"]);
  const playlist = response.playlists.items.find(
    (item) =>
      item.name === "Discover Weekly" && item.owner.display_name === "Spotify"
  );

  if (playlist && deviceId) {
    await playSongs(spotify, deviceId, playlist.id);
  }
}
