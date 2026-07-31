// sockjs-client 在浏览器环境依赖 global，提供 polyfill
if (typeof window !== 'undefined' && typeof (window as any).global === 'undefined') {
  ;(window as any).global = window
}

import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  return {
    app,
    pinia
  }
}
