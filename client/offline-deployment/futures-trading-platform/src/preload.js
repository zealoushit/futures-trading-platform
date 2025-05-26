import { contextBridge, ipcRenderer } from 'electron'

// 暴露安全的API给渲染进程
contextBridge.exposeInMainWorld('electronAPI', {
  // 获取期货合约列表
  getFuturesList: () => ipcRenderer.invoke('get-futures-list'),
  
  // 订阅行情数据
  subscribeMarketData: (symbols, callback) => {
    // 创建MessageChannel用于高性能数据传输
    const { port1, port2 } = new MessageChannel()
    
    // 设置接收消息的处理函数
    port1.onmessage = (event) => {
      callback(event.data)
    }
    
    // 发送订阅请求和port2到主进程
    ipcRenderer.postMessage('subscribe-market-data', { symbols }, [port2])
    
    // 返回取消订阅的函数
    return () => {
      port1.close()
    }
  }
})