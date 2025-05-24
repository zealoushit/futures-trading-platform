'use strict'

import { app, protocol, BrowserWindow, ipcMain } from 'electron'
import { createProtocol } from 'vue-cli-plugin-electron-builder/lib'
import installExtension, { VUEJS3_DEVTOOLS } from 'electron-devtools-installer'
import path from 'path'

const isDevelopment = process.env.NODE_ENV !== 'production'

// 保持window对象的全局引用，避免JavaScript对象被垃圾回收时窗口关闭
let win

protocol.registerSchemesAsPrivileged([
  { scheme: 'app', privileges: { secure: true, standard: true } }
])

async function createWindow() {
  // 创建浏览器窗口
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      // 使用插件的preload
      preload: isDevelopment
        ? path.join(__dirname, 'preload.js')
        : path.join(__dirname, '../preload.js'),
      // 是否集成Node
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  if (process.env.WEBPACK_DEV_SERVER_URL) {
    // 如果在开发环境，加载开发服务器URL
    await win.loadURL(process.env.WEBPACK_DEV_SERVER_URL)
    if (!process.env.IS_TEST) win.webContents.openDevTools()
  } else {
    createProtocol('app')
    // 在生产环境下加载index.html
    win.loadURL('app://./index.html')
  }
}

// 当所有窗口都被关闭时退出应用
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})

// 应用初始化完成时调用
app.on('ready', async () => {
  if (isDevelopment && !process.env.IS_TEST) {
    try {
      await installExtension(VUEJS3_DEVTOOLS)
    } catch (e) {
      console.error('Vue Devtools failed to install:', e.toString())
    }
  }
  createWindow()
})

// 设置行情数据模拟推送
setupMarketDataSimulation()

function setupMarketDataSimulation() {
  // 模拟合约数据
  const futuresContracts = [
    { symbol: 'IF2109', name: '沪深300期指', exchange: 'CFFEX' },
    { symbol: 'IC2109', name: '中证500期指', exchange: 'CFFEX' },
    { symbol: 'IH2109', name: '上证50期指', exchange: 'CFFEX' },
    { symbol: 'CU2109', name: '铜期货', exchange: 'SHFE' },
    { symbol: 'AL2109', name: '铝期货', exchange: 'SHFE' },
    { symbol: 'ZN2109', name: '锌期货', exchange: 'SHFE' },
    { symbol: 'AU2112', name: '黄金期货', exchange: 'SHFE' },
    { symbol: 'RB2110', name: '螺纹钢期货', exchange: 'SHFE' },
    { symbol: 'M2109', name: '豆粕期货', exchange: 'DCE' },
    { symbol: 'C2109', name: '玉米期货', exchange: 'DCE' }
  ]

  // 处理行情订阅请求
  ipcMain.handle('get-futures-list', () => {
    return futuresContracts
  })

  // 使用MessageChannel处理行情数据推送
  ipcMain.on('subscribe-market-data', (event, { symbols }) => {
    const [port] = event.ports

    // 为每个合约生成初始价格
    const priceMap = {}
    symbols.forEach(symbol => {
      // 根据合约类型设置不同的价格范围
      let basePrice = 0
      if (symbol.startsWith('IF')) basePrice = 5000
      else if (symbol.startsWith('IC')) basePrice = 6500
      else if (symbol.startsWith('IH')) basePrice = 3500
      else if (symbol.startsWith('CU')) basePrice = 70000
      else if (symbol.startsWith('AL')) basePrice = 20000
      else if (symbol.startsWith('ZN')) basePrice = 23000
      else if (symbol.startsWith('AU')) basePrice = 370
      else if (symbol.startsWith('RB')) basePrice = 5500
      else if (symbol.startsWith('M')) basePrice = 3800
      else if (symbol.startsWith('C')) basePrice = 2700
      else basePrice = 1000 + Math.random() * 9000

      priceMap[symbol] = basePrice
    })

    // 模拟行情数据推送
    const interval = setInterval(() => {
      symbols.forEach(symbol => {
        // 模拟价格波动 (±0.5%)
        const priceChange = priceMap[symbol] * (Math.random() * 0.01 - 0.005)
        const newPrice = priceMap[symbol] + priceChange
        priceMap[symbol] = newPrice

        // 发送行情数据
        port.postMessage({
          symbol,
          price: newPrice,
          open: newPrice * (1 - Math.random() * 0.01),
          high: newPrice * (1 + Math.random() * 0.005),
          low: newPrice * (1 - Math.random() * 0.005),
          close: newPrice,
          change: priceChange / priceMap[symbol] * 100,
          volume: Math.floor(Math.random() * 10000),
          amount: Math.floor(Math.random() * 100000000),
          timestamp: Date.now()
        })
      })
    }, 1000) // 每秒更新一次

    // 清理函数
    port.on('close', () => clearInterval(interval))
  })
}

// 在开发模式下，根据父进程的请求退出
if (isDevelopment) {
  if (process.platform === 'win32') {
    process.on('message', (data) => {
      if (data === 'graceful-exit') {
        app.quit()
      }
    })
  } else {
    process.on('SIGTERM', () => {
      app.quit()
    })
  }
}

