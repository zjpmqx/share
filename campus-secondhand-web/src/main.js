import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { ensureEvents } from './services/events'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)

app.config.warnHandler = (msg, instance, trace) => {
  console.warn('[vue-warn]', msg, trace)
  console.warn('[vue-warn-stack]', new Error().stack)
}

app.config.errorHandler = (err, instance, info) => {
  console.error('[vue-error]', info, err)
}

window.addEventListener('error', (e) => {
  console.error('[window-error]', e.error || e.message)
})

window.addEventListener('unhandledrejection', (e) => {
  console.error('[unhandledrejection]', e.reason)
})

if (import.meta?.env?.DEV) {
  ensureEvents()
}

app.use(router).mount('#app')
