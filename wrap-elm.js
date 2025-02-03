import fs, { watch } from "node:fs/promises"

import chokidar from "chokidar"

function wrapElmCode (code) {
  return `
function wrapper() {
  let output = {};
  (function () {
        ${code}
  }).call(output);
  return output.Elm;
}

export default wrapper;
  `;
}

async function main() {
  const inputFileName = process.argv[2]
  const outputFileName = process.argv[3]

  const watcher = chokidar.watch(inputFileName, {
    awaitWriteFinish: {
      stabilityThreshold: 2000,
      pollInterval: 100
    },
  })

  process.on("SIGINT", async () => {
    // close watcher when Ctrl-C is pressed
    console.log("Closing watcher...")
    await watcher.close()
    process.exit(0)
  })

  const compile = async () => {
    const code = await (await fs.readFile(inputFileName)).toString()
    const wrapped = wrapElmCode(code)
    await fs.writeFile(outputFileName, wrapped)
  }

  watcher.on("change", async () => {
    await compile()
  })

  await compile()
}

main()
