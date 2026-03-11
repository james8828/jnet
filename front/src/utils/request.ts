import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import router from '@/router' // 引入路由实例

// 创建 axios 实例
const serviceAxios = axios.create({
    baseURL: '', // 基础请求地址
    timeout: 10000,
    withCredentials: false,
})

// 请求拦截器
serviceAxios.interceptors.request.use(
    (config) => {
        const access_token = localStorage.getItem('access_token')
        const token_type = localStorage.getItem('token_type')

        if (access_token && token_type) {
            config.headers.Authorization = `${token_type} ${access_token}`
        }
        //config.headers.Authorization = `Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxNzYsInVzZXJfa2V5IjoiM2E0NjA2OWYtODM3ZS00OTQ2LWIxNTMtNzhkNzg0OWZkY2E3IiwidXNlcm5hbWUiOiJidXpoaWRhbyJ9.fm-ZBxuZ5Cj8L4wBmUmOxtEhghtSewsoEwRX_rcr9RdkyZokN328KC81Iu1bokZnNwyApAfw8BGK1rIJw-z1bQ`
        // 固定 Content-Type
        if (!config.headers?.['Content-Type']) {
            config.headers['Content-Type'] = 'application/json'
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 错误码提示映射
const ERROR_MESSAGES: Record<number, string> = {
    302: '接口重定向了！',
    400: '参数不正确！',
    401: '您未登录，或者登录已经超时，请先登录！',
    403: '您没有权限操作！',
    404: '请求地址出错！',
    408: '请求超时！',
    409: '系统已存在相同数据！',
    500: '服务器内部错误！',
    501: '服务未实现！',
    502: '网关错误！',
    503: '服务不可用！',
    504: '服务暂时无法访问，请稍后再试！',
    505: 'HTTP 版本不受支持！'
}

// 响应拦截器
serviceAxios.interceptors.response.use(
    (res) => {
        // 拦截业务状态码
        const { code, msg } = res.data
        if (code === 500) {
            ElMessage.error({
                message: msg || '服务器内部错误',
                duration: 3000,
                type: 'error'
            })

            return Promise.reject(new Error(msg || '服务器内部错误'))
        }

        return res.data
    },
    (error) => {
        let message = '未知错误，请联系管理员！'

        if (error.response) {
            const status = error.response.status
            message = ERROR_MESSAGES[error.response.status] || message

            // 如果是 401，执行登出逻辑
            if (status === 401) {
                ElMessage.error({
                    message: '身份验证失败，请重新登录',
                    duration: 1500,
                    type: 'error'
                })

                // 清除 token
                localStorage.removeItem('access_token')
                localStorage.removeItem('token_type')
                localStorage.removeItem('expires_in')

                // 跳转登录页
                router.push({ name: 'Login' }) // 确保你的登录页面路由名为 Login 或替换为 '/login'
                return Promise.reject(new Error('未授权'))
            }


            // 使用 Element Plus 提示错误
            ElMessage.error({
                message,
                duration: 3000,
                type: 'error'
            })

            // 可选：开发环境下显示更详细的错误通知
            if (process.env.NODE_ENV === 'development') {
                ElNotification.error({
                    title: '网络错误',
                    message: `状态码：${error.response.status}, URL: ${error.response.config.url}`,
                    duration: 5000
                })
            }
        } else if (error.request) {
            message = '网络异常，请检查您的网络连接。'

            ElMessage.error({
                message,
                duration: 3000,
                type: 'error'
            })
        }

        return Promise.reject(message)
    }
)

export default serviceAxios
