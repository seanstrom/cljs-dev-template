import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task";
import * as app from '#cljs/app/app.main.js'
import { Elm } from "./Main.elm"
import './app.css'
import { boot } from './spotify.js'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  console.log('test')
  const elm = Elm.Main.init({ node: document.getElementById("root") })
  elm.ports.outbox?.subscribe((msg) => {
    console.log("msg", msg)
  })

  elm.ports.inbox.send("Hello!")

  ConcurrentTask.register({
    tasks: {
      "boot": (args) => app.boot(import.meta.env)
    },
    ports: {
      send: elm.ports.run,
      receive: elm.ports.track,
    },
  });

  // app.render()
  // app.boot(import.meta.env)
  // boot(import.meta.env)
}
