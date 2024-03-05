import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task";
import * as app from '#cljs/app/app.main.js'
import { Elm } from "./Main.elm"
import './app.css'
import { boot, resumePlayer } from './spotify.js'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  const elm = Elm.Main.init({
    node: document.getElementById("root")
  })

  elm.ports.outbox?.subscribe((msg) => {
    switch (msg.tag) {
      case "action:resume":
        return window.player.resume()
      case "action:pause":
        return window.player.pause()
      case "action:nextTrack":
        return window.player.nextTrack()
      case "action:previousTrack":
        return window.player.previousTrack()
      default:
        return console.log("Oops unknown messsage: ", msg)
    }
  })

  elm.ports.inbox.send("Hello!")

  ConcurrentTask.register({
    tasks: {
      "boot": (args) => console.log("boot") || app.boot(import.meta.env),
      "resumePlayer": (args) => console.log("hel") || resumePlayer(),
    },
    ports: {
      send: elm.ports.run,
      receive: elm.ports.track,
    },
  });
}

window.resumePlayer = resumePlayer
