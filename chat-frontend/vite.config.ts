import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'path'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://10.116.23.158:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://10.116.23.158:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://10.116.23.158:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
