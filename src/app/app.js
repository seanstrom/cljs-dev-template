import * as app from '#cljs/app/app.main.js'
import { example } from "./app.ts"
import './app.css'

if (import.meta.hot) {
  import.meta.hot.accept()
}

window.onload = () => {
  app.render()
  example()
}
