import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task";
import * as app from '#cljs/app/app.main.js'
import { Elm } from "./Main.elm"
import './app.css'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  console.log('test')
  const app = Elm.Main.init({ node: document.getElementById("root") })
  app.ports.outbox?.subscribe((msg) => {
    console.log("msg", msg)
  })

  app.ports.inbox.send("Hello!")

  ConcurrentTask.register({
    tasks: {},
    ports: {
      send: app.ports.sendPortTask,
      receive: app.ports.receivePortTask,
    },
  });
}
