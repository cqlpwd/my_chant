import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import basicSsl from '@vitejs/plugin-basic-ssl'
import path from 'path'

export default defineConfig({
  plugins: [uni(), basicSsl()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: true, // 允许局域网设备通过 IP 访问（如 http://10.116.22.160:3000）
    https: true, // 自签名 HTTPS，手机访问 https://10.116.22.160:3000 才能拿到摄像头/麦克风权限
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://10.116.22.160:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://10.116.22.160:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://10.116.22.160:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
