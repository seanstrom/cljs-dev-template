import * as server from '#cljs/server/server.main.js'

export let app = server.main(1 << 30)

export default app
