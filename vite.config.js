import path from 'path'
import { defineConfig } from "vite"
import { fileURLToPath, URL } from 'url'


export default defineConfig({
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
