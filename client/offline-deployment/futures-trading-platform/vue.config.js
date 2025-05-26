const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,

  // Electron 配置
  pluginOptions: {
    electronBuilder: {
      // 指定主进程文件
      mainProcessFile: 'src/background.js',
      // 指定预加载脚本文件
      preload: 'src/preload.js',
      // 或者使用 preload 数组形式
      // preload: ['src/preload.js'],

      // 构建配置
      builderOptions: {
        appId: 'com.futures.trading.platform',
        productName: '期货交易平台',
        directories: {
          output: 'dist_electron'
        },
        files: [
          'dist_electron/**/*'
        ],
        win: {
          target: 'nsis',
          icon: 'public/favicon.ico'
        },
        nsis: {
          oneClick: false,
          allowToChangeInstallationDirectory: true
        }
      }
    }
  }
})
