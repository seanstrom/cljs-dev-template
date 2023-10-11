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

### Run ESbuild
```
npm run app-js-esbuild
```

### Look at ESbuild console output

You should see an warning message like:
```
▲ [WARNING] Source maps with "sections" are not supported [sections-in-source-map]

    cljs-dist/app/app.main.js.map:1:34:
      1 │ {"version":3,"file":"app.main.js","sections":[{"offset":{"line":3,"column":0},"map":{"version":3,"file":"app.main.js","li...
        ╵                                   ~~~~~~~~~~

  The source map "cljs-dist/app/app.main.js.map" was referenced by the file
  "cljs-dist/app/app.main.js" here:

    cljs-dist/app/app.main.js:5763:21:
      5763 │ //# sourceMappingURL=app.main.js.map
           ╵                      ~~~~~~~~~~~~~~~
```
