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
      "~js": path.resolve(__dirname, "src/"),
    },
  },
})
