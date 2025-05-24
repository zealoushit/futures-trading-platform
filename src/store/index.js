import { createStore } from 'vuex'
import { tradeAPI, marketAPI, tradingAPI } from '@/services/api'
import wsService from '@/services/websocket'
import { API_CONFIG } from '@/config/api'

export default createStore({
  state: {
    user: null,
    marketData: {},
    selectedSymbols: [],
    contracts: [],
    orders: [],
    positions: [],
    funds: null,
    trades: [],
    wsConnected: false
  },

  getters: {
    isLoggedIn: state => !!state.user,
    marketDataList: state => Object.values(state.marketData),
    wsConnectionState: state => state.wsConnected
  },

  mutations: {
    SET_USER(state, user) {
      state.user = user
    },
    SET_MARKET_DATA(state, data) {
      state.marketData = {
        ...state.marketData,
        [data.symbol]: data
      }
    },
    SET_SELECTED_SYMBOLS(state, symbols) {
      state.selectedSymbols = symbols
    },
    CLEAR_MARKET_DATA(state) {
      state.marketData = {}
    },
    SET_CONTRACTS(state, contracts) {
      state.contracts = contracts
    },
    SET_ORDERS(state, orders) {
      state.orders = orders
    },
    UPDATE_ORDER(state, order) {
      const index = state.orders.findIndex(o => o.id === order.id)
      if (index !== -1) {
        state.orders.splice(index, 1, order)
      } else {
        state.orders.push(order)
      }
    },
    SET_POSITIONS(state, positions) {
      state.positions = positions
    },
    UPDATE_POSITION(state, position) {
      const index = state.positions.findIndex(p => p.symbol === position.symbol)
      if (index !== -1) {
        state.positions.splice(index, 1, position)
      } else {
        state.positions.push(position)
      }
    },
    SET_FUNDS(state, funds) {
      state.funds = funds
    },
    SET_TRADES(state, trades) {
      state.trades = trades
    },
    ADD_TRADE(state, trade) {
      state.trades.unshift(trade)
    },
    SET_WS_CONNECTED(state, connected) {
      state.wsConnected = connected
    }
  },

  actions: {
    // 登录
    async login({ commit, dispatch }, credentials) {
      try {
        // 先进行交易系统登录
        const tradingResponse = await tradingAPI.login()

        // 再进行行情系统登录
        const marketResponse = await marketAPI.login()

        // 创建用户对象
        const user = {
          id: 'user_001',
          name: credentials?.username || '交易用户',
          tradingStatus: tradingResponse.success,
          marketStatus: marketResponse.success,
          loginTime: new Date().toISOString()
        }

        commit('SET_USER', user)
        localStorage.setItem('user', JSON.stringify(user))

        // 登录成功后连接WebSocket
        await dispatch('connectWebSocket')

        // 登录成功后自动订阅交易数据
        if (wsService.getConnectionState()) {
          wsService.subscribeTradeData()
          console.log('已自动订阅交易数据')
        }

        return user
      } catch (error) {
        throw new Error(error.message || '登录失败')
      }
    },

    // 登出
    async logout({ commit, dispatch }) {
      try {
        // 交易系统登出
        await tradingAPI.logout()
      } catch (error) {
        console.error('登出API调用失败:', error)
      } finally {
        commit('SET_USER', null)
        localStorage.removeItem('user')

        // 断开WebSocket连接
        dispatch('disconnectWebSocket')
      }
    },

    // 初始化store
    async initializeStore({ commit, dispatch }) {
      // 从localStorage恢复用户会话
      const user = localStorage.getItem('user')
      if (user) {
        commit('SET_USER', JSON.parse(user))
        // 如果有用户信息，尝试连接WebSocket
        await dispatch('connectWebSocket')
      }
    },

    // 连接WebSocket
    async connectWebSocket({ commit }) {
      try {
        await wsService.connect()
        commit('SET_WS_CONNECTED', true)

        // 注册WebSocket事件监听
        wsService.on('market_data', (data) => {
          console.log('收到行情数据:', data)

          // 转换API数据格式到前端格式
          const marketData = {
            symbol: data.instrumentId,
            price: data.lastPrice || 0,
            change: (data.lastPrice || 0) - (data.preClosePrice || 0),
            changePercent: data.preClosePrice > 0 ?
              (((data.lastPrice - data.preClosePrice) / data.preClosePrice) * 100).toFixed(2) : '0.00',
            open: data.openPrice || 0,
            high: data.highestPrice || 0,
            low: data.lowestPrice || 0,
            volume: data.volume || 0,
            amount: data.turnover || 0,
            timestamp: Date.now(),
            // 添加更多字段以支持完整的行情显示
            preClosePrice: data.preClosePrice || 0,
            upperLimitPrice: data.upperLimitPrice || 0,
            lowerLimitPrice: data.lowerLimitPrice || 0,
            openInterest: data.openInterest || 0,
            updateTime: data.updateTime || new Date().toLocaleTimeString(),
            // 买卖盘数据
            bidPrice1: data.bidPrice1 || 0,
            bidVolume1: data.bidVolume1 || 0,
            askPrice1: data.askPrice1 || 0,
            askVolume1: data.askVolume1 || 0,
            bidPrice2: data.bidPrice2 || 0,
            bidVolume2: data.bidVolume2 || 0,
            askPrice2: data.askPrice2 || 0,
            askVolume2: data.askVolume2 || 0,
            bidPrice3: data.bidPrice3 || 0,
            bidVolume3: data.bidVolume3 || 0,
            askPrice3: data.askPrice3 || 0,
            askVolume3: data.askVolume3 || 0
          }
          commit('SET_MARKET_DATA', marketData)
        })

        wsService.on('trade_data', (data) => {
          // 转换成交数据格式
          const tradeData = {
            time: new Date(data.tradeTime).toLocaleTimeString(),
            symbol: data.instrumentId,
            direction: data.direction === '0' ? 'buy' : 'sell',
            price: data.price,
            quantity: data.volume,
            amount: (data.price * data.volume).toFixed(2),
            status: '已成交'
          }
          commit('ADD_TRADE', tradeData)
        })

        wsService.on('order_update', (data) => {
          // 转换订单数据格式
          const orderData = {
            id: data.orderRef,
            time: new Date(data.insertTime).toLocaleTimeString(),
            symbol: data.instrumentId,
            direction: data.direction === '0' ? 'buy' : 'sell',
            price: data.limitPrice,
            quantity: data.volumeTotalOriginal,
            filled: data.volumeTraded,
            status: data.orderStatus in API_CONFIG.ORDER_STATUS ?
                   API_CONFIG.ORDER_STATUS[data.orderStatus] : '未知状态'
          }
          commit('UPDATE_ORDER', orderData)
        })

        wsService.on('connection', (data) => {
          console.log('连接状态更新:', data)
        })

        wsService.on('login_status', (data) => {
          console.log('登录状态更新:', data)
        })

        console.log('WebSocket连接成功')
      } catch (error) {
        console.error('WebSocket连接失败:', error)
        commit('SET_WS_CONNECTED', false)
      }
    },

    // 断开WebSocket
    disconnectWebSocket({ commit }) {
      wsService.disconnect()
      commit('SET_WS_CONNECTED', false)
    },

    // 更新行情数据
    updateMarketData({ commit }, data) {
      commit('SET_MARKET_DATA', data)
    },

    // 选择合约
    async selectSymbols({ commit }, symbols) {
      commit('SET_SELECTED_SYMBOLS', symbols)

      // 先调用行情登录API
      try {
        await marketAPI.login()
        console.log('行情系统登录成功')
      } catch (error) {
        console.warn('行情系统登录失败:', error)
      }

      // 调用订阅API
      try {
        await marketAPI.subscribe(symbols)
        console.log('行情订阅API调用成功:', symbols)
      } catch (error) {
        console.warn('行情订阅API调用失败:', error)
      }

      // 订阅WebSocket行情数据
      if (wsService.getConnectionState()) {
        wsService.subscribeMarketData(symbols)
        console.log('WebSocket行情订阅成功:', symbols)
      }
    },

    // 获取合约列表
    async fetchContracts({ commit }) {
      try {
        // 由于API文档中没有专门的合约列表接口，我们使用模拟数据
        // 实际使用时可以调用 tradingAPI.getInstrument() 获取特定合约信息
        const contracts = [
          // 默认订阅的合约
          { symbol: 'rb2405', name: '螺纹钢2405', exchange: 'SHFE', category: '黑色金属' },
          { symbol: 'cu2405', name: '沪铜2405', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'al2405', name: '沪铝2405', exchange: 'SHFE', category: '有色金属' },
          // 其他常用合约
          { symbol: 'CU2401', name: '沪铜2401', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'AL2401', name: '沪铝2401', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'RB2401', name: '螺纹钢2401', exchange: 'SHFE', category: '黑色金属' },
          { symbol: 'AG2401', name: '沪银2401', exchange: 'SHFE', category: '贵金属' },
          { symbol: 'AU2401', name: '沪金2401', exchange: 'SHFE', category: '贵金属' },
          { symbol: 'ZN2405', name: '沪锌2405', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'PB2405', name: '沪铅2405', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'NI2405', name: '沪镍2405', exchange: 'SHFE', category: '有色金属' },
          { symbol: 'SN2405', name: '沪锡2405', exchange: 'SHFE', category: '有色金属' }
        ]
        commit('SET_CONTRACTS', contracts)
        return contracts
      } catch (error) {
        throw new Error(error.message || '获取合约列表失败')
      }
    },

    // 获取订单列表
    async fetchOrders({ commit }, params = {}) {
      try {
        const response = await tradeAPI.getOrders(params)
        const orders = response.data || response
        commit('SET_ORDERS', orders)
        return orders
      } catch (error) {
        throw new Error(error.message || '获取订单列表失败')
      }
    },

    // 获取持仓列表
    async fetchPositions({ commit }) {
      try {
        const response = await tradeAPI.getPositions()
        const positions = response.data || response
        commit('SET_POSITIONS', positions)
        return positions
      } catch (error) {
        throw new Error(error.message || '获取持仓列表失败')
      }
    },

    // 获取资金信息
    async fetchFunds({ commit }) {
      try {
        const response = await tradeAPI.getFunds()
        const funds = response.data || response
        commit('SET_FUNDS', funds)
        return funds
      } catch (error) {
        throw new Error(error.message || '获取资金信息失败')
      }
    },

    // 下单
    async placeOrder({ dispatch }, order) {
      try {
        const response = await tradeAPI.placeOrder(order)
        // 下单成功后刷新订单列表
        await dispatch('fetchOrders')
        return response.data || response
      } catch (error) {
        throw new Error(error.message || '下单失败')
      }
    },

    // 撤单
    async cancelOrder({ dispatch }, orderId) {
      try {
        const response = await tradeAPI.cancelOrder(orderId)
        // 撤单成功后刷新订单列表
        await dispatch('fetchOrders')
        return response.data || response
      } catch (error) {
        throw new Error(error.message || '撤单失败')
      }
    }
  }
})