// WebSocket服务模块 - 使用STOMP over SockJS
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_CONFIG } from '@/config/api'

class WebSocketService {
  constructor() {
    this.client = null
    this.url = API_CONFIG.WS_URL
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = API_CONFIG.RECONNECT.MAX_ATTEMPTS
    this.reconnectInterval = API_CONFIG.RECONNECT.INTERVAL
    this.callbacks = new Map()
    this.subscriptions = new Map()
    this.isConnected = false
  }

  // 连接WebSocket
  connect() {
    return new Promise((resolve, reject) => {
      try {
        // 创建STOMP客户端
        this.client = new Client({
          webSocketFactory: () => new SockJS(this.url),
          connectHeaders: {},
          debug: (str) => {
            console.log('STOMP Debug:', str)
          },
          reconnectDelay: this.reconnectInterval,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        })

        // 连接成功回调
        this.client.onConnect = (frame) => {
          console.log('STOMP连接已建立:', frame)
          this.isConnected = true
          this.reconnectAttempts = 0

          // 订阅连接状态主题
          this.subscribeToTopic(API_CONFIG.WS_TOPICS.CONNECTION, (message) => {
            this.triggerCallback('connection', JSON.parse(message.body))
          })

          // 订阅行情连接状态主题
          this.subscribeToTopic(API_CONFIG.WS_TOPICS.MARKET_CONNECTION, (message) => {
            this.triggerCallback('market_connection', JSON.parse(message.body))
          })

          resolve()
        }

        // 连接错误回调
        this.client.onStompError = (frame) => {
          console.error('STOMP连接错误:', frame.headers['message'])
          console.error('详细信息:', frame.body)
          this.isConnected = false
          reject(new Error(frame.headers['message']))
        }

        // WebSocket错误回调
        this.client.onWebSocketError = (error) => {
          console.error('WebSocket错误:', error)
          this.isConnected = false
          reject(error)
        }

        // 连接关闭回调
        this.client.onDisconnect = (frame) => {
          console.log('STOMP连接已关闭:', frame)
          this.isConnected = false
          this.clearSubscriptions()
        }

        // 激活连接
        this.client.activate()
      } catch (error) {
        reject(error)
      }
    })
  }

  // 断开连接
  disconnect() {
    this.clearSubscriptions()
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.isConnected = false
  }

  // 订阅主题
  subscribeToTopic(topic, callback) {
    if (this.isConnected && this.client) {
      const subscription = this.client.subscribe(topic, callback)
      this.subscriptions.set(topic, subscription)
      return subscription
    } else {
      console.warn('STOMP客户端未连接，无法订阅主题:', topic)
      return null
    }
  }

  // 取消订阅主题
  unsubscribeFromTopic(topic) {
    const subscription = this.subscriptions.get(topic)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(topic)
    }
  }

  // 清除所有订阅
  clearSubscriptions() {
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe()
    })
    this.subscriptions.clear()
  }

  // 发送消息到指定目的地
  sendMessage(destination, body) {
    if (this.isConnected && this.client) {
      this.client.publish({
        destination: destination,
        body: JSON.stringify(body)
      })
    } else {
      console.warn('STOMP客户端未连接，无法发送消息')
    }
  }

  // 触发回调
  triggerCallback(type, data) {
    const callbacks = this.callbacks.get(type) || []
    callbacks.forEach(callback => {
      try {
        callback(data)
      } catch (error) {
        console.error('回调执行错误:', error)
      }
    })
  }

  // 注册回调
  on(type, callback) {
    if (!this.callbacks.has(type)) {
      this.callbacks.set(type, [])
    }
    this.callbacks.get(type).push(callback)
  }

  // 移除回调
  off(type, callback) {
    const callbacks = this.callbacks.get(type) || []
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  }

  // 订阅行情数据
  subscribeMarketData(instruments) {
    // 订阅通用行情数据主题
    this.subscribeToTopic(API_CONFIG.WS_TOPICS.MARKET_DATA, (message) => {
      const data = JSON.parse(message.body)
      this.triggerCallback('market_data', data)
    })

    // 订阅特定合约的行情数据
    instruments.forEach(instrument => {
      const topic = `${API_CONFIG.WS_TOPICS.MARKET_DATA_SPECIFIC}${instrument}`
      this.subscribeToTopic(topic, (message) => {
        const data = JSON.parse(message.body)
        this.triggerCallback('market_data', data)
      })
    })
  }

  // 取消订阅行情数据
  unsubscribeMarketData(instruments) {
    // 取消订阅通用行情数据主题
    this.unsubscribeFromTopic(API_CONFIG.WS_TOPICS.MARKET_DATA)

    // 取消订阅特定合约的行情数据
    instruments.forEach(instrument => {
      const topic = `${API_CONFIG.WS_TOPICS.MARKET_DATA_SPECIFIC}${instrument}`
      this.unsubscribeFromTopic(topic)
    })
  }

  // 订阅交易数据
  subscribeTradeData() {
    // 订阅订单更新
    this.subscribeToTopic(API_CONFIG.WS_TOPICS.ORDERS, (message) => {
      const data = JSON.parse(message.body)
      this.triggerCallback('order_update', data)
    })

    // 订阅成交数据
    this.subscribeToTopic(API_CONFIG.WS_TOPICS.TRADES, (message) => {
      const data = JSON.parse(message.body)
      this.triggerCallback('trade_data', data)
    })

    // 订阅登录状态
    this.subscribeToTopic(API_CONFIG.WS_TOPICS.LOGIN, (message) => {
      const data = JSON.parse(message.body)
      this.triggerCallback('login_status', data)
    })
  }

  // 取消订阅交易数据
  unsubscribeTradeData() {
    this.unsubscribeFromTopic(API_CONFIG.WS_TOPICS.ORDERS)
    this.unsubscribeFromTopic(API_CONFIG.WS_TOPICS.TRADES)
    this.unsubscribeFromTopic(API_CONFIG.WS_TOPICS.LOGIN)
  }

  // 兼容性方法 - 保持与原有代码的兼容性
  subscribeMarket(symbols) {
    this.subscribeMarketData(symbols)
  }

  unsubscribeMarket(symbols) {
    this.unsubscribeMarketData(symbols)
  }

  subscribeTrade() {
    this.subscribeTradeData()
  }

  // 获取连接状态
  getConnectionState() {
    return this.isConnected
  }
}

// 创建单例实例
const wsService = new WebSocketService()

export default wsService
