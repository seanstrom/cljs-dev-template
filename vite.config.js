import path from 'path'
import gleam from "vite-gleam"
import { defineConfig } from "vite"

export default defineConfig({
  plugins: [gleam()],
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
