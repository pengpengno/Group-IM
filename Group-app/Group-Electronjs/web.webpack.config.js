const path = require('path');
const webpack = require('webpack');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const getConfig = require('./scripts/load-config');

const isProduction = process.env.NODE_ENV === 'production';
const config = getConfig();

module.exports = {
  mode: isProduction ? 'production' : 'development',
  entry: './renderer/index.tsx',
  output: {
    path: path.resolve(__dirname, 'dist-web'),
    filename: isProduction ? 'bundle.[contenthash].js' : 'bundle.js',
    publicPath: '/',
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.js', '.jsx'],
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: {
          loader: 'ts-loader',
          options: {
            configFile: 'tsconfig.web.json',
          },
        },
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
    ],
  },
  plugins: [
    new CleanWebpackPlugin(),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development'),
      '__API_BASE__': JSON.stringify(''),
      '__SIGNAL_BASE__': JSON.stringify(''),
      '__TCP_HOST__': JSON.stringify(config.TCP_HOST || ''),
      '__TCP_PORT__': JSON.stringify(config.TCP_PORT || '8088'),
      '__DEV_MODE__': !isProduction
    }),
    new HtmlWebpackPlugin({
      template: './renderer/index.html',
      filename: 'index.html',
    })
  ],
  devServer: {
    port: 3000,
    historyApiFallback: true,
    open: true,
    hot: true,
    client: {
      webSocketURL: {
        pathname: '/webpack-ws'
      }
    },
    webSocketServer: {
      type: 'ws',
      options: {
        path: '/webpack-ws'
      }
    },
    proxy: {
      '/api': {
        target: config.API_BASE || 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/ws': {
        target: config.API_BASE || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        ws: true
      }
    }
  },
};
