import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task";
// import * as app from '#cljs/app/app.main.js'
// import { Elm } from "./Main.elm"
import Main from "./Main.elm"
import './app.css'
import { boot, resumePlayer } from './spotify.js'

import { add_one } from "./app.gleam"

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  console.log('test', add_one)

  const elm = Main.init({
    // const elm = Elm.Main.init({
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
      "boot": (args) => {
        const { spotify, deviceId } = boot(import.meta.env)
        return deviceId
      },
      "resumePlayer": (args) => {
        resumePlayer()
      },
      "sendBackendMsg": async (msg) => {
        const response = await fetch("http://localhost:3100/backendMsg", {
          method: "POST",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            requestId: crypto.randomUUID(),
            content: msg
          }),
        })
        const payload = await response.json()
        return payload.content
      }
    },
    ports: {
      send: elm.ports.run,
      receive: elm.ports.track,
    },
  });
}

window.resumePlayer = resumePlayer
