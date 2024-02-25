const webpack = require("@nativescript/webpack");

module.exports = (env) => {
  webpack.init(env);

  // Learn how to customize:
  // https://docs.nativescript.org/webpack

  webpack.chainWebpack(config => {
    config.module
      .rule("elm")
      .test(/\.elm$/)
      .use('elm-webpack-loader')
      .loader('elm-webpack-loader')
      .options({
        debug: false,
        pathToElm: 'node_modules/.bin/elm'
      })
  })

  return webpack.resolveConfig();
};
