const path = require('path');
const fs = require('fs');
const yaml = require('yaml');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const { ProvidePlugin } = require('webpack');

const { port } = yaml.parse(fs.readFileSync(path.join(__dirname, 'config.yml'), {
  encoding: 'utf-8'
}));

/** @type {import('webpack').Configuration} */
module.exports = {
  entry: './manager/src/main/resources/web/src/index.tsx',
  output: {
    path: path.resolve(__dirname, 'manager', 'src', 'main', 'resources', 'web', 'dist'),
    publicPath: '/assets/',
    filename: 'main.js',
    chunkFilename: '[name].[contenthash].js',
    clean: true
  },
  devServer: {
    hot: false,
    devMiddleware: {
      writeToDisk: true
    },
    proxy: [{
      context: ['/api/ws'],
      target: `http://localhost:${port}`,
      ws: true
    }, {
      context: () => true,
      target: `http://localhost:${port}`
    }],
    client: {
      overlay: false
    }
  },
  resolve: {
    extensions: [
      '.js',
      '.jsx',
      '.ts',
      '.tsx',
      '.scss',
      '.sass'
    ]
  },
  module: {
    rules: [
      {
        test: /\.(j|t)sx?$/,
        exclude: /\.d\.ts$/,
        use: [
          {
            loader: 'babel-loader'
          }
        ]
      },
      {
        test: /\.css$/,
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          'postcss-loader'
        ]
      }
    ]
  },
  optimization: {
    minimizer: [
      new TerserPlugin(),
      new CssMinimizerPlugin()
    ],
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: '[name].[contenthash].css'
    }),
    new ProvidePlugin({
      React: 'react'
    })
  ]
};
