import axios from 'axios'
import { getToken, clearToken } from './auth'

const env = import.meta?.env || {}

export function normalizeErrorResponseData(data) {
  if (typeof data === 'string' && data.trim()) {
    return { message: data }
  }
  return data
}

export function shouldRedirectToLoginPath(pathname = '') {
  const currentPath = String(pathname || '')
  return currentPath.startsWith('/admin') || currentPath.startsWith('/publish') || currentPath.startsWith('/my-items') || currentPath.startsWith('/orders') || currentPath.startsWith('/profile') || currentPath.startsWith('/shares/publish')
}

export function buildLoginRedirectTarget(locationLike) {
  const pathname = String(locationLike?.pathname || '')
  const search = String(locationLike?.search || '')
  const hash = String(locationLike?.hash || '')
  const currentPath = `${pathname}${search}${hash}`
  if (!shouldRedirectToLoginPath(pathname) || pathname.startsWith('/login')) {
    return ''
  }
  return `/login?redirect=${encodeURIComponent(currentPath)}`
}

export function shouldHandleShareGate403(path = '', message = '') {
  return String(path || '').startsWith('/shares') && String(message || '').includes('Share gate verify required')
}

export function buildShareVerifyRedirectTarget(locationLike) {
  const pathname = String(locationLike?.pathname || '')
  if (pathname.startsWith('/shares/verify')) {
    return ''
  }
  const search = String(locationLike?.search || '')
  const hash = String(locationLike?.hash || '')
  const redirect = `${pathname}${search}${hash}`
  return `/shares/verify?redirect=${encodeURIComponent(redirect)}`
}

export const http = axios.create({
  baseURL: env.VITE_API_BASE || '/api',
  timeout: 0,
  withCredentials: true,
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
      error.response.data = normalizeErrorResponseData(error?.response?.data)
    } catch {
    }

    if (error?.response?.status === 401) {
      clearToken()

      try {
        if (typeof window !== 'undefined') {
          const redirectTarget = buildLoginRedirectTarget(window.location)
          if (redirectTarget) {
            window.location.href = redirectTarget
          }
        }
      } catch {
      }
    }

    if (error?.response?.status === 403) {
      try {
        const path = String(error?.config?.url || '')
        const msg = String(error?.response?.data?.message || '')
        if (shouldHandleShareGate403(path, msg)) {
          if (typeof window !== 'undefined') {
            const redirectTarget = buildShareVerifyRedirectTarget(window.location)
            if (redirectTarget) {
              window.location.href = redirectTarget
            }
          }
        }
      } catch {
      }
    }

    return Promise.reject(error)
  },
)
