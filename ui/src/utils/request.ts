import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 响应数据结构定义
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

// 分页数据结构
export interface PageData<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 创建axios实例
 * 
 * 注意：不设置 baseURL，因为项目采用多服务架构
 * - 业务服务：/biz/api/v1/*
 * - 标注服务：/anno/api/v1/*
 * 每个 API 模块自行管理完整路径，通过 Vite 代理转发
 */
const service: AxiosInstance = axios.create({
  baseURL: '', // 不设置基础URL，由各API模块自行指定完整路径
  timeout: 30000, // 请求超时时间30秒
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

/**
 * 请求拦截器
 */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 从localStorage获取token并添加到请求头
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 开发环境下打印请求信息
    if (import.meta.env.DEV) {
      console.log('[Request]', config.method?.toUpperCase(), config.url, config.params || config.data)
    }
    
    return config
  },
  (error) => {
    console.error('[Request Error]', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    
    // 开发环境下打印响应信息
    if (import.meta.env.DEV) {
      console.log('[Response]', response.config.url, res)
    }
    
    // 根据后端返回的code判断请求是否成功
    if (res.code === 10000 || res.code === 200) {
      return res.data
    } else {
      // 业务错误处理
      ElMessage.error(res.msg || '请求失败')
      
      // 特定错误码处理
      if (res.code === 401) {
        // Token过期或未授权，跳转到登录页
        localStorage.removeItem('access_token')
        window.location.href = '/login'
      } else if (res.code === 403) {
        ElMessage.error('没有权限访问')
      } else if (res.code === 404) {
        ElMessage.error('请求的资源不存在')
      }
      
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  (error) => {
    console.error('[Response Error]', error)
    
    // HTTP错误处理
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 400:
          ElMessage.error('请求参数错误')
          break
        case 401:
          ElMessage.error('未授权，请重新登录')
          localStorage.removeItem('access_token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求地址不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 502:
          ElMessage.error('网关错误')
          break
        case 503:
          ElMessage.error('服务不可用')
          break
        case 504:
          ElMessage.error('网关超时')
          break
        default:
          ElMessage.error(`连接错误${status}`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    
    return Promise.reject(error)
  }
)

/**
 * 通用请求方法
 */
class Request {
  /**
   * GET请求
   */
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config)
  }

  /**
   * POST请求
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  }

  /**
   * PUT请求
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  }

  /**
   * DELETE请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config)
  }

  /**
   * 上传文件
   */
  upload<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }

  /**
   * 下载文件
   */
  download(url: string, config?: AxiosRequestConfig): Promise<Blob> {
    return service.get(url, {
      ...config,
      responseType: 'blob'
    })
  }
}

export default new Request()
