import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@fontsource-variable/inter'
import * as Icons from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/theme.css'

const app = createApp(App)

// 注册全部图标
for (const [name, comp] of Object.entries(Icons)) {
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误捕获：仅在控制台记录，不再注入 DOM 横幅（避免误报干扰正常操作）
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', err, info)
}
window.addEventListener('error', (e) => {
  console.error('[Window Error]', e.message)
})
window.addEventListener('unhandledrejection', (e) => {
  const msg = (e.reason && (e.reason.message || e.reason)) || String(e.reason)
  console.error('[Unhandled Rejection]', msg)
})

app.mount('#app')
