import * as app from '#cljs/app/app.main.js'
import { Elm } from "./Main.elm"
import './app.css'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  Elm.Main.init({ node: document.getElementById("root") })
  // app.render()
}
