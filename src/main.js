import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 全局错误处理 - 抑制ResizeObserver错误
const originalError = console.error
console.error = (...args) => {
  if (args[0] && args[0].includes && args[0].includes('ResizeObserver loop limit exceeded')) {
    return
  }
  originalError.apply(console, args)
}

// 全局错误处理器
window.addEventListener('error', (event) => {
  if (event.message && event.message.includes('ResizeObserver loop limit exceeded')) {
    event.preventDefault()
    return false
  }
})

const app = createApp(App)

app.use(store)
app.use(router)
app.use(ElementPlus)

app.mount('#app')