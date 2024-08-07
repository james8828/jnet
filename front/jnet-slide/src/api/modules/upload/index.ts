import axios from 'axios'
// 创建 axios 请求实例
export const bigUploadRequest = axios.create({
    baseURL: "", // 基础请求地址
    timeout: 10000, // 请求超时设置
    withCredentials: false // 跨域请求是否需要携带 cookie
})
