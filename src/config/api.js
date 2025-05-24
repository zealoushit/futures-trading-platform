// API配置文件
export const API_CONFIG = {
  // 基础URL - 根据您的Java服务实际地址修改
  BASE_URL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080',

  // WebSocket URL - 使用STOMP over SockJS (注意：SockJS使用HTTP协议，不是WS协议)
  WS_URL: process.env.VUE_APP_WS_URL || 'http://localhost:8080/ws',

  // 请求超时时间
  TIMEOUT: 10000,

  // API端点
  ENDPOINTS: {
    // 交易相关
    TRADING: {
      STATUS: '/api/trading/status',
      LOGIN: '/api/trading/login',
      LOGOUT: '/api/trading/logout',
      ORDER: '/api/trading/order',
      CANCEL: '/api/trading/cancel',
      POSITION: '/api/trading/position',
      ACCOUNT: '/api/trading/account',
      ORDERS: '/api/trading/orders',
      TRADES: '/api/trading/trades',
      INSTRUMENT: '/api/trading/instrument',
      HEALTH: '/api/trading/health',
      VERSION: '/api/trading/version'
    },

    // 行情相关
    MARKET: {
      STATUS: '/api/market/status',
      LOGIN: '/api/market/login',
      SUBSCRIBE: '/api/market/subscribe',
      UNSUBSCRIBE: '/api/market/unsubscribe',
      INSTRUMENT: '/api/market/instrument',
      SUBSCRIPTIONS: '/api/market/subscriptions'
    }
  },

  // WebSocket STOMP 主题
  WS_TOPICS: {
    // 交易相关主题
    CONNECTION: '/topic/connection',
    LOGIN: '/topic/login',
    ORDERS: '/topic/orders',
    TRADES: '/topic/trades',

    // 行情相关主题
    MARKET_CONNECTION: '/topic/market/connection',
    MARKET_DATA: '/topic/market/data',
    MARKET_DATA_SPECIFIC: '/topic/market/data/'  // 后面跟合约代码
  },

  // 重连配置
  RECONNECT: {
    MAX_ATTEMPTS: 5,
    INTERVAL: 3000
  },

  // 心跳配置
  HEARTBEAT: {
    INTERVAL: 30000
  },

  // 订单状态映射
  ORDER_STATUS: {
    '0': '全部成交',
    '1': '部分成交',
    '3': '未成交',
    '5': '已撤销'
  },

  // 买卖方向映射
  DIRECTION: {
    '0': 'buy',
    '1': 'sell'
  },

  // 开平标志映射
  OFFSET_FLAG: {
    '0': '开仓',
    '1': '平仓',
    '3': '平今'
  }
}

export default API_CONFIG
