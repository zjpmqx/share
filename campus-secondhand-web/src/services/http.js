import axios from 'axios'
import { getToken, clearToken } from './auth'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 0,
  transformResponse: [
    (data) => {
      if (typeof data !== 'string') return data
      const s = data.trim()
      if (!s) return data
      try {
        return JSON.parse(s)
      } catch {
        return data
      }
    },
  ],
  // 确保上传进度能正确工作
  maxRedirects: 5,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    try {
      const d = error?.response?.data
      if (typeof d === 'string' && d.trim()) {
        error.response.data = { message: d }
      }
    } catch {
    }

    if (error?.response?.status === 401) {
      clearToken()

      // 避免出现“token 失效但页面仍停留在受保护页，导致上传/请求反复报错”的体验
      try {
        const isBrowser = typeof window !== 'undefined'
        if (isBrowser) {
          const path = window.location.pathname + window.location.search + window.location.hash
          if (!window.location.pathname.startsWith('/login')) {
            window.location.href = `/login?redirect=${encodeURIComponent(path)}`
          }
        }
      } catch {
      }
    }

    return Promise.reject(error)
  },
)
