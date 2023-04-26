import path from 'path'
import { defineConfig } from "vite"

export default defineConfig({
  resolve:{
    alias:{
      '@cljs' : path.resolve(__dirname, './cljs-dist')
    },
  },
  server: {
    watch: {
      ignored: ["**/cljs-dist/**/cljs-runtime/**"]
    },
  },
})
