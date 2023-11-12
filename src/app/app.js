import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task";
import * as app from '#cljs/app/app.main.js'
import { Elm } from "./Main.elm"
import './app.css'

import { add_one } from "./app.gleam"

if (import.meta.hot) {
  import.meta.hot.accept()
}

app.init()

window.onload = () => {
  console.log('test', add_one)
  const app = Elm.Main.init({ node: document.getElementById("root") })
  app.ports.outbox?.subscribe((msg) => {
    console.log("msg", msg)
  })

  app.ports.inbox.send("Hello!")

  ConcurrentTask.register({
    tasks: {},
    ports: {
      send: app.ports.run,
      receive: app.ports.track,
    },
  });
}
