import path from 'path'
import gleam from "vite-gleam"
import { defineConfig } from "vite"
import { plugin as elmPlugin } from "vite-plugin-elm"

export default defineConfig({
  plugins: [gleam(), elmPlugin()],
  server: {
    watch: {
      // ignored: ["**/cljs-dist/**/cljs-runtime/**"]
    },
  },
  resolve: {
    alias: {
      "~src": path.resolve(__dirname, "src/"),
    },
  },
})
