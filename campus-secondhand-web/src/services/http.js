import axios from 'axios'
import { getToken, clearToken, getShareGateToken, clearShareGateToken } from './auth'

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

  try {
    const path = String(config?.url || '')
    const isSharesApi = path.startsWith('/shares')
    if (isSharesApi) {
      const shareGateToken = getShareGateToken()
      if (shareGateToken) {
        config.headers = config.headers || {}
        config.headers['X-Share-Gate-Token'] = shareGateToken
      }
    }
  } catch {
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

      try {
        const path = String(error?.config?.url || '')
        if (path.startsWith('/shares')) {
          clearShareGateToken()
        }
      } catch {
      }

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

    if (error?.response?.status === 403) {
      try {
        const path = String(error?.config?.url || '')
        const msg = String(error?.response?.data?.message || '')
        if (path.startsWith('/shares') && msg.includes('Share gate verify required')) {
          clearShareGateToken()
          if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/shares/verify')) {
            const redirect = window.location.pathname + window.location.search + window.location.hash
            window.location.href = `/shares/verify?redirect=${encodeURIComponent(redirect)}`
          }
        }
      } catch {
      }
    }

    return Promise.reject(error)
  },
)
