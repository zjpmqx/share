const TOKEN_KEY = 'campus_trade_token'
const ROLE_KEY = 'campus_trade_role'

function notifyAuthChanged() {
  try {
    window.dispatchEvent(new Event('auth-changed'))
  } catch {
  }
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
  notifyAuthChanged()
}

export function setRole(role) {
  localStorage.setItem(ROLE_KEY, role)
  notifyAuthChanged()
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getRole() {
  return localStorage.getItem(ROLE_KEY)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  notifyAuthChanged()
}
