import path from 'path'
import { defineConfig } from "vite"
import { plugin as elmPlugin } from "vite-plugin-elm"

export default defineConfig({
  plugins: [elmPlugin()],
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
