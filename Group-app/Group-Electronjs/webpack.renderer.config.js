const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const getConfig = require('./scripts/load-config');

const config = getConfig();

module.exports = {
  mode: process.env.NODE_ENV === 'production' ? 'production' : 'development',
  entry: './renderer/index.tsx',
  target: 'web', // changed from 'electron-renderer' because contextIsolation is true
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
    ],
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development'),
      '__API_BASE__': JSON.stringify(config.API_BASE),
      '__SIGNAL_BASE__': JSON.stringify(config.API_BASE || ''),
      '__TCP_HOST__': JSON.stringify(config.TCP_HOST),
      '__TCP_PORT__': JSON.stringify(config.TCP_PORT),
      '__DEV_MODE__': process.env.NODE_ENV !== 'production'
    }),
    new HtmlWebpackPlugin({
      template: './renderer/index.html',
    }),
  ],
  output: {
    filename: 'renderer.js',
    path: path.resolve(__dirname, 'dist'),
  },
};
