import { createStore } from 'vuex'

export default createStore({
  state: {
    user: null,
    marketData: {},
    selectedSymbols: []
  },

  getters: {
    isLoggedIn: state => !!state.user,
    marketDataList: state => Object.values(state.marketData)
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
    }
  },

  actions: {
    login({ commit }, credentials) {
      // 模拟登录，实际应用中应该调用API
      return new Promise((resolve) => {
        setTimeout(() => {
          // 假设登录成功
          const user = {
            id: 1,
            name: credentials.username,
            token: 'mock-token-' + Date.now()
          }
          commit('SET_USER', user)
          localStorage.setItem('user', JSON.stringify(user))
          resolve(user)
        }, 500)
      })
    },

    logout({ commit }) {
      commit('SET_USER', null)
      localStorage.removeItem('user')
    },

    initializeStore({ commit }) {
      // 从localStorage恢复用户会话
      const user = localStorage.getItem('user')
      if (user) {
        commit('SET_USER', JSON.parse(user))
      }
    },

    updateMarketData({ commit }, data) {
      commit('SET_MARKET_DATA', data)
    },

    selectSymbols({ commit }, symbols) {
      commit('SET_SELECTED_SYMBOLS', symbols)
      // 不清空市场数据，保留已有数据
    }
  }
})