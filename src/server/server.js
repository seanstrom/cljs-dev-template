import * as server from '@cljs-dist/server/server.main.js'

function main () {
  server.main()
  setInterval(() => {}, 1 << 30)
}

main()
