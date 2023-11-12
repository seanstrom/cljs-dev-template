import * as app from '#cljs/app/app.main.js'
import './app.css'

if (import.meta.hot) {
  import.meta.hot.accept()
}

app.init()

window.onload = () => {
  app.render()
}
