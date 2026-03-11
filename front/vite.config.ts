import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      // 设置 `@` 指向 `src` 目录
      '@': path.resolve(__dirname, './src'),
      '@constants': path.resolve('src/constants'),
      '@assets': path.resolve('src/assets'),
      '@comps': path.resolve('src/components'),
      '@utils': path.resolve('src/utils'),
      '@config': path.resolve('src/config'),
      '@router': path.resolve('src/router'),
      '@store': path.resolve('src/store'),
      '@enums': path.resolve('src/enums'),
      '@views': path.resolve('src/views'),
      '@api': path.resolve('src/api'),
      '@types': path.resolve('src/types')
    },
    //extensions: [".ts", ".js", ".vue", ".json", ".mjs"],
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  define: {
    'process.env': {
      API_BASE_URL: '/', //设置接口代理base path
      AUTH_SERVICE_URL: '/oauth2-service',
      SYSTEM_SERVICE_URL: '/system',
      IMAGE_SERVICE_URL: '/imageService',
      NODE_ENV : 'development'
    }
  },
  plugins: [vue()],
  server: {
    port: 4000, // 服务端口号
    open: true, // 服务启动时是否自动打开浏览器
    cors: true, // 允许跨域
    proxy: {
      '/oauth2-service': {
        target: 'http://127.0.0.1:9001', // 后端开发环境服务地址
        changeOrigin: true
      },
      '/anno-service': {
        target: 'http://127.0.0.1:9001', // 后端开发环境服务地址
        changeOrigin: true
      },
      '/image-service': {
        target: 'http://127.0.0.1:9001', // 后端开发环境服务地址
        changeOrigin: true
      },
      '/system': {
        target: 'http://127.0.0.1:9001', // 后端开发环境服务地址
        changeOrigin: true
      }
    }
  }
})
