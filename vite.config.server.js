import path from 'path'
import { defineConfig } from 'vite'
import { VitePluginNode } from 'vite-plugin-node'
import gleam from "vite-gleam"
import { plugin as elmPlugin } from "vite-plugin-elm"
// import elmPlugin from "vite-plugin-elm-watch"

export default defineConfig({
  // ...vite configures
  server: {
    // vite server configs, for details see [vite doc](https://vitejs.dev/config/#server-host)
    port: 3100,
    hmr: true,
  },
  plugins: [
     gleam(),
     elmPlugin(),

    ...VitePluginNode({
      // Nodejs native Request adapter
      // currently this plugin support 'express', 'nest', 'koa' and 'fastify' out of box,
      // you can also pass a function if you are using other frameworks, see Custom Adapter section
      adapter: 'express',

      // tell the plugin where is your project entry
      appPath: './src/server/server.js',

      // Optional, default: 'viteNodeApp'
      // the name of named export of you app from the appPath file
      exportName: 'app',

      // Optional, default: false
      // if you want to init your app on boot, set this to true
      initAppOnBoot: true,
    })
  ],
  resolve: {
    alias: {
      "#elm": path.resolve(__dirname, "elm-dist/"),
    },
  },
})
