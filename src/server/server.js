import * as server from '@cljs/server/server.main.js'

function main () {
  server.main()
  setInterval(() => {}, 1 << 30)
}

main()
