// API服务模块
import axios from 'axios'
import { API_CONFIG } from '@/config/api'

// 创建axios实例
const api = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 添加token到请求头
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user.token) {
      config.headers.Authorization = `Bearer ${user.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    const data = response.data
    // 根据API文档的响应格式处理
    if (data.success === false) {
      throw new Error(data.message || '请求失败')
    }
    return data
  },
  error => {
    if (error.response) {
      // 服务器响应错误
      const { status, data } = error.response
      if (status === 401) {
        // 未授权，清除本地存储并跳转到登录页
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
      const message = data?.message || '请求失败'
      return Promise.reject(new Error(message))
    } else if (error.request) {
      // 网络错误
      return Promise.reject(new Error('网络连接失败'))
    } else {
      return Promise.reject(new Error('请求配置错误'))
    }
  }
)

// API接口定义 - 根据实际API文档调整

// 交易相关API
export const tradingAPI = {
  // 获取交易状态
  getStatus() {
    return api.get(API_CONFIG.ENDPOINTS.TRADING.STATUS)
  },

  // 用户登录
  login() {
    return api.post(API_CONFIG.ENDPOINTS.TRADING.LOGIN)
  },

  // 用户登出
  logout() {
    return api.post(API_CONFIG.ENDPOINTS.TRADING.LOGOUT)
  },

  // 下单
  placeOrder(order) {
    // 转换前端格式到API格式
    const apiOrder = {
      instrumentId: order.symbol,
      direction: order.direction === 'buy' ? '0' : '1',
      offsetFlag: order.offsetFlag || '0',
      price: order.price,
      volume: order.quantity
    }
    return api.post(API_CONFIG.ENDPOINTS.TRADING.ORDER, apiOrder)
  },

  // 撤单
  cancelOrder(orderInfo) {
    return api.post(API_CONFIG.ENDPOINTS.TRADING.CANCEL, orderInfo)
  },

  // 查询持仓
  getPositions(instrumentId = null) {
    const params = instrumentId ? { instrumentId } : {}
    return api.get(API_CONFIG.ENDPOINTS.TRADING.POSITION, { params })
  },

  // 查询资金账户
  getAccount() {
    return api.get(API_CONFIG.ENDPOINTS.TRADING.ACCOUNT)
  },

  // 查询报单
  getOrders(instrumentId = null) {
    const params = instrumentId ? { instrumentId } : {}
    return api.get(API_CONFIG.ENDPOINTS.TRADING.ORDERS, { params })
  },

  // 查询成交
  getTrades(instrumentId = null) {
    const params = instrumentId ? { instrumentId } : {}
    return api.get(API_CONFIG.ENDPOINTS.TRADING.TRADES, { params })
  },

  // 查询合约
  getInstrument(instrumentId) {
    return api.get(`${API_CONFIG.ENDPOINTS.TRADING.INSTRUMENT}/${instrumentId}`)
  },

  // 健康检查
  getHealth() {
    return api.get(API_CONFIG.ENDPOINTS.TRADING.HEALTH)
  },

  // 版本信息
  getVersion() {
    return api.get(API_CONFIG.ENDPOINTS.TRADING.VERSION)
  }
}

// 行情相关API
export const marketAPI = {
  // 获取行情状态
  getStatus() {
    return api.get(API_CONFIG.ENDPOINTS.MARKET.STATUS)
  },

  // 行情登录
  login() {
    return api.post(API_CONFIG.ENDPOINTS.MARKET.LOGIN)
  },

  // 订阅行情
  subscribe(instruments) {
    return api.post(API_CONFIG.ENDPOINTS.MARKET.SUBSCRIBE, { instruments })
  },

  // 退订行情
  unsubscribe(instruments) {
    return api.post(API_CONFIG.ENDPOINTS.MARKET.UNSUBSCRIBE, { instruments })
  },

  // 查询合约信息
  getInstrument(instrumentId) {
    return api.get(`${API_CONFIG.ENDPOINTS.MARKET.INSTRUMENT}/${instrumentId}`)
  },

  // 获取订阅列表
  getSubscriptions() {
    return api.get(API_CONFIG.ENDPOINTS.MARKET.SUBSCRIPTIONS)
  }
}

// 为了兼容性，保留原有的API别名
export const authAPI = {
  login: tradingAPI.login,
  logout: tradingAPI.logout,
  getUserInfo: tradingAPI.getStatus
}

export const tradeAPI = {
  placeOrder: tradingAPI.placeOrder,
  cancelOrder: tradingAPI.cancelOrder,
  getOrders: tradingAPI.getOrders,
  getPositions: tradingAPI.getPositions,
  getFunds: tradingAPI.getAccount
}

export const queryAPI = {
  getInvestorInfo: tradingAPI.getAccount,
  getInvestorFunds: tradingAPI.getAccount,
  getTrades: tradingAPI.getTrades
}

export default api
