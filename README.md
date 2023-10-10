## Minimal Sourcemap Issue Reproduction

### Dependencies
* Clojure
* Java
* NPM

### Install JS Dependencies
```
npm install
```

### Build CLJS with Source Maps
```
npm run app-cljs-prod
```

### Run Vite Dev Server
```
npm run app-js-dev
```

### Visit localhost:4000/

### Look at Vite console output

You should see an error message like:
```
[vite] Internal server error: Cannot read properties of undefined (reading 'length')
      at loadAndTransform (file:///Users/seanstrom/Code/org-seanstrom/cljs-dev-template/node_modules/vite/dist/node/chunks/dep-79892de8.js:41126:63)
```

### Reasons for error?

* This could be caused by Vite's lack of support for "indexed" source maps (or source maps with "sections").
* It's possible to potentially flatten an indexed source map inside of Vite, maybe it's possible inside of Shadow-CLJS?
