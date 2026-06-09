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

      // 1. HTTP 接口代理 (保持现状)
      '/biz': {
        target: 'http://localhost:9203',
        changeOrigin: true,
      },

      // 2. WebSocket 代理 (新增)
      '/ws': {
        target: 'ws://localhost:9203', // 注意这里是 ws://
        changeOrigin: true,
        ws: true, // 关键：开启 WebSocket 代理支持
        rewrite: (path) => path.replace(/^\/ws/, '/ws') // 保持路径不变，或者根据后端实际路径调整
      }
    }
  }
})
