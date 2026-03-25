const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

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
    new HtmlWebpackPlugin({
      template: './renderer/index.html',
      favicon: './renderer/favicon.svg'
    }),
  ],
  output: {
    filename: 'renderer.js',
    path: path.resolve(__dirname, 'dist'),
  },
};