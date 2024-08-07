import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'


// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      // 设置 `@` 指向 `src` 目录
      '@': path.resolve(__dirname, './src'),
      '@assets': path.resolve('src/assets'),
      '@comps': path.resolve('src/components'),
      '@utils': path.resolve('src/utils'),
      '@config': path.resolve('src/config'),
      '@router': path.resolve('src/router'),
      '@store': path.resolve('src/store'),
      '@enums': path.resolve('src/enums'),
      '@views': path.resolve('src/views'),
    },
    //extensions: [".ts", ".js", ".vue", ".json", ".mjs"],
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  plugins: [vue()],
  server: {
    port: 4000, // 服务端口号
    open: true, // 服务启动时是否自动打开浏览器
    cors: true, // 允许跨域
    proxy: {

      // 本地开发环境通过代理实现跨域，生产环境使用 nginx 转发
      '/image': {
        target: 'http://127.0.0.1:9004', // 后端开发环境服务地址
        // target: 'http://172.31.2.65:8080', // 后端测试环境服务地址
        changeOrigin: true
      },
      '/jnet': {
        target: 'http://127.0.0.1:7878', // 后端开发环境服务地址
        //target: 'http://127.0.0.1:8787', // 后端开发环境服务地址
        // target: 'http://172.31.2.65:8080', // 后端测试环境服务地址
        changeOrigin: true
      }
    }
  }
})
