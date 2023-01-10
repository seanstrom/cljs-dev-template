import * as app from '../cljs-dist/app.main.js'

if (process.env.NODE_ENV !== "production") {
  require("../cljs-dist/shadow.cljs.devtools.client.browser.js")
}

window.onload = () => {
  app.render()
}

if (module.hot) {
  module.hot.accept(() => {
    app.render()
  })
}
