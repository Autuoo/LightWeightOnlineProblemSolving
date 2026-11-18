import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { API_BASE } from './config'

export class ApiError extends Error {
  constructor(code, message) {
    super(message || '请求失败')
    this.name = 'ApiError'
    this.code = code
  }
}

const http = axios.create({
  baseURL: API_BASE,
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        throw new ApiError(body.code, body.message)
      }
      return body.data
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401) {
      const auth = useAuthStore()
      auth.clear()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new ApiError(body?.code ?? 40103, body?.message ?? '登录已过期'))
    }
    if (body && typeof body.code === 'number') {
      return Promise.reject(new ApiError(body.code, body.message))
    }
    if (!error.response) {
      return Promise.reject(new ApiError(0, `无法连接后端（${API_BASE}），请确认后端服务已启动`))
    }
    return Promise.reject(new ApiError(status, error.message || '请求失败'))
  }
)

export default http