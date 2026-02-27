// Axios 封装
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data

    if (code === 200) {
      return data
    }

    // 业务错误
    return Promise.reject(new Error(message || '请求失败'))
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/admin/login'
    }
    return Promise.reject(error)
  }
)

// 封装请求方法
export interface RequestConfig {
  params?: Record<string, unknown>
  headers?: Record<string, string>
}

export default {
  get: <T>(url: string, config?: RequestConfig): Promise<T> =>
    request.get(url, config) as Promise<T>,
  post: <T>(url: string, data?: unknown, config?: RequestConfig): Promise<T> =>
    request.post(url, data, config) as Promise<T>,
  put: <T>(url: string, data?: unknown, config?: RequestConfig): Promise<T> =>
    request.put(url, data, config) as Promise<T>,
  delete: <T>(url: string, config?: RequestConfig): Promise<T> =>
    request.delete(url, config) as Promise<T>,
}
