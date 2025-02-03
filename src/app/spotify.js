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

export async function resumePlayer() {
  await window.player.resume()
  return new Promise((resolve, _reject) => {
    setTimeout(async () => {
      const state = await window.player.getCurrentState()
      console.log("is paused", state.paused)
      resolve(state.paused)
    }, 1000)
  })
}

async function playSongs(spotify, deviceId, playlist) {
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
  const playlistId = "37i9dQZEVXcD8aCW1Jk6NB";

  if (deviceId && playlistId) {
    // https://open.spotify.com/embed/playlist/37i9dQZEVXcD8aCW1Jk6NB
    const embedHtmlRequest = await fetch(`http://localhost:3100/spotifyEmbed?playlistId=${playlistId}`);
    const data = await embedHtmlRequest.json()
    const embedHtml = data.html

    const parser = new DOMParser();
    const doc = parser.parseFromString(embedHtml, "text/html");
    const element = doc.getElementById("__NEXT_DATA__");
    const embedProps = JSON.parse(element.text);
    const embedEntity = embedProps.props.pageProps.state.data.entity;

    const playlistEmbed = {
      tracks: {
        items: embedEntity.trackList.map(track => ({ track }))
      },
    };
    await playSongs(spotify, deviceId, playlistEmbed)
  }

  return { deviceId, spotify }
}
