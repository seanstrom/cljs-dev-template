import * as app from '#cljs/app/app.main.js'
import { Elm } from "./App.elm"
import './app.css'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  Elm.App.init({ node: document.getElementById("root") })
  // app.render()
}
