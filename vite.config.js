import { defineConfig } from "vite"

export default defineConfig({
  server: {
    watch: {
      ignored: ['**/cljs-dist/**'],
    },
  },
})
