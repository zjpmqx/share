import { http } from './http'

function normalizeAssetUrl(url) {
  if (!url || typeof url !== 'string') return url

  if (url.startsWith('/')) return url

  try {
    const u = new URL(url)
    const host = (u.hostname || '').toLowerCase()
    if (
      host === 'localhost' ||
      host === '127.0.0.1' ||
      host.startsWith('192.168.') ||
      host.startsWith('10.') ||
      host.startsWith('172.')
    ) {
      return `${u.pathname}${u.search}${u.hash}`
    }
  } catch {
    return url
  }

  return url
}

function normalizeItem(it) {
  if (!it) return it
  if (it.coverImageUrl) it.coverImageUrl = normalizeAssetUrl(it.coverImageUrl)
  return it
}

function normalizeShare(it) {
  if (!it) return it
  if (it.mediaUrl) it.mediaUrl = normalizeAssetUrl(it.mediaUrl)
  return it
}

export async function register(payload) {
  const { data } = await http.post('/auth/register', payload)
  return data
}

export async function login(payload) {
  const { data } = await http.post('/auth/login', payload)
  return data
}

export async function me() {
  const { data } = await http.get('/auth/me')
  return data
}

export async function onlinePing() {
  const { data } = await http.post('/online/ping')
  return data
}

export async function onlineCount() {
  const { data } = await http.get('/online/count')
  return data
}

export async function updateAvatar(payload) {
  const { data } = await http.put('/user/avatar', payload)
  return data
}

export async function listItems(params) {
  const { data } = await http.get('/items', { params })
  if (Array.isArray(data?.data)) {
    data.data = data.data.map(normalizeItem)
  }
  return data
}

export async function listMyItems(params) {
  const { data } = await http.get('/items/my', { params })
  if (Array.isArray(data?.data)) {
    data.data = data.data.map(normalizeItem)
  }
  return data
}

export async function getItem(id) {
  const { data } = await http.get(`/items/${id}`)
  if (data?.data) {
    data.data = normalizeItem(data.data)
  }
  return data
}

export async function createItem(payload) {
  const { data } = await http.post('/items', payload)
  return data
}

export async function updateItem(id, payload) {
  const { data } = await http.put(`/items/${id}`, payload)
  return data
}

export async function deleteItem(id) {
  const { data } = await http.delete(`/items/${id}`)
  return data
}

export async function offShelfItem(id) {
  const { data } = await http.post(`/items/${id}/off-shelf`)
  return data
}

export async function onShelfItem(id) {
  const { data } = await http.post(`/items/${id}/on-shelf`)
  return data
}

export async function listMessages(itemId) {
  const { data } = await http.get(`/items/${itemId}/messages`)
  return data
}

export async function postMessage(itemId, payload) {
  const { data } = await http.post(`/items/${itemId}/messages`, payload)
  return data
}

export async function createOrder(payload) {
  const { data } = await http.post('/orders', payload)
  return data
}

export async function myBuyOrders() {
  const { data } = await http.get('/orders/my/buy')
  return data
}

export async function mySellOrders() {
  const { data } = await http.get('/orders/my/sell')
  return data
}

export async function payOrder(id) {
  const { data } = await http.post(`/orders/${id}/pay`)
  return data
}

export async function shipOrder(id) {
  const { data } = await http.post(`/orders/${id}/ship`)
  return data
}

export async function confirmOrder(id) {
  const { data } = await http.post(`/orders/${id}/confirm`)
  return data
}

export async function cancelOrder(id) {
  const { data } = await http.post(`/orders/${id}/cancel`)
  return data
}

export async function adminPendingItems(params) {
  const { data } = await http.get('/admin/items/pending', { params })
  return data
}

export async function adminAuditItem(id, payload) {
  const { data } = await http.post(`/admin/items/${id}/audit`, payload)
  return data
}

export async function adminListItems(params) {
  const { data } = await http.get('/admin/items', { params })
  return data
}

export async function adminOffShelfItem(id) {
  const { data } = await http.post(`/admin/items/${id}/off-shelf`)
  return data
}

export async function adminDeleteItem(id) {
  const { data } = await http.delete(`/admin/items/${id}`)
  return data
}

export async function adminOffShelfAll(confirm) {
  const { data } = await http.post('/admin/items/off-shelf-all', null, { params: { confirm } })
  return data
}

export async function adminDeleteAllItems(confirm) {
  const { data } = await http.delete('/admin/items', { params: { confirm } })
  return data
}

export async function adminListUsers(params) {
  const { data } = await http.get('/admin/users', { params })
  return data
}

export async function adminDeleteShare(id) {
  const { data } = await http.delete(`/admin/shares/${id}`)
  return data
}

async function sleep(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function postWithRetry(url, body, config, retryOptions = {}) {
  const retries = Number.isFinite(retryOptions?.retries) ? retryOptions.retries : 4
  const baseDelayMs = Number.isFinite(retryOptions?.baseDelayMs) ? retryOptions.baseDelayMs : 500

  let lastErr = null
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      return await http.post(url, body, config)
    } catch (e) {
      lastErr = e
      const status = e?.response?.status
      const isNetwork = !e?.response
      const retryableStatus = status === 429 || (status >= 500 && status <= 599)
      const shouldRetry = attempt < retries && (isNetwork || retryableStatus)
      if (!shouldRetry) break

      const delay = Math.min(baseDelayMs * Math.pow(2, attempt), 8000)
      await sleep(delay)
    }
  }
  throw lastErr
}

export async function uploadFile(file, options = {}) {
  console.log('uploadFile 开始', file.name, file.size)
  
  const onProgress = typeof options?.onProgress === 'function' ? options.onProgress : null
  const timeoutMs = Number.isFinite(options?.timeoutMs) ? options.timeoutMs : 10 * 60 * 1000
  const retries = Number.isFinite(options?.retries) ? options.retries : 3
  
  if (!file) {
    throw new Error('file is required')
  }

  const form = new FormData()
  form.append('file', file)
  
  try {
    const { data } = await http.post('/files/upload', form, {
      timeout: timeoutMs,
      // 确保请求头正确
      headers: {
        'Content-Type': undefined, // 让 axios 自动设置
      },
      onUploadProgress: (progressEvent) => {
        console.log('上传进度事件:', progressEvent)
        if (onProgress && progressEvent.total && progressEvent.total > 0) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          console.log('上传进度计算:', percentCompleted + '%', 'loaded:', progressEvent.loaded, 'total:', progressEvent.total)
          onProgress(percentCompleted)
        }
      },
    })
    
    if (onProgress) onProgress(100)
    console.log('uploadFile 完成', data)
    return data
  } catch (error) {
    console.error('uploadFile 失败:', error)
    throw error
  }
}

export async function listShares(params) {
  const { data } = await http.get('/shares', { params })
  if (Array.isArray(data?.data)) {
    data.data = data.data.map(normalizeShare)
  }
  return data
}

export async function getShare(id) {
  const { data } = await http.get(`/shares/${id}`)
  if (data?.data) {
    data.data = normalizeShare(data.data)
  }
  return data
}

export async function createShare(payload) {
  const { data } = await http.post('/shares', payload)
  return data
}

export async function listShareComments(shareId) {
  const { data } = await http.get(`/shares/${shareId}/comments`)
  return data
}

export async function postShareComment(shareId, payload) {
  const { data } = await http.post(`/shares/${shareId}/comments`, payload)
  return data
}
