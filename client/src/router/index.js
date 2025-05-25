import { createRouter, createWebHashHistory } from 'vue-router'
import store from '../store'
import LoginView from '../views/LoginView.vue'
import MarketView from '../views/MarketView.vue'
import TradeView from '../views/TradeView.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { requiresAuth: false }
  },
  {
    path: '/market',
    name: 'Market',
    component: MarketView,
    meta: { requiresAuth: true }
  },
  {
    path: '/trade',
    name: 'Trade',
    component: TradeView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 初始化store（从localStorage恢复用户会话）
  store.dispatch('initializeStore')

  const isLoggedIn = store.getters.isLoggedIn
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  if (requiresAuth && !isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next('/login')
  } else if (to.path === '/login' && isLoggedIn) {
    // 已登录用户访问登录页，跳转到市场页
    next('/market')
  } else {
    next()
  }
})

export default router
