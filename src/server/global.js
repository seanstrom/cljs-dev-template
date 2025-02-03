console.log("GLOBAL")

globalThis.scope = globalThis

import.meta.hot = true
import.meta.ENV = { test: true }
globalThis.window = globalThis

const memory = globalThis.__memory__ ||= {}

memory.count ||= 2

export default memory
