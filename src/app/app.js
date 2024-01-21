import * as app from '#cljs/app/app.main.js'
import './app.css'
import { boot } from './spotify.js'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  app.render()
  // app.boot(import.meta.env)
  boot(import.meta.env)
}
