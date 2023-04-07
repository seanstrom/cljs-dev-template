import * as app from '../cljs-dist/app.main.js'

if (process.env.NODE_ENV !== "production") {
  require("../cljs-dist/cljs-runtime/shadow.cljs.devtools.client.browser.js")
}

window.onload = () => {
  app.render()
  console.clear = () => {}
}

if (module.hot) {
  module.hot.accept(() => {
    app.render()
  })
}
