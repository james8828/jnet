import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      // 代理标注服务 /anno 请求
      '/anno': {
        target: 'http://localhost:9005',
        changeOrigin: true
      },
      // 代理业务服务 /biz 请求
      '/biz': {
        target: 'http://localhost:9203',
        changeOrigin: true
      }
    }
  }
})
