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
  // Cloudflare Tunnel + HTTP/2 下并发大请求更容易触发 ERR_HTTP2_PROTOCOL_ERROR
  // 默认用更保守的参数，确保稳定；需要更快可在 options 里提高。
  const isBrowser = typeof window !== 'undefined'
  const host = isBrowser ? window.location.hostname : ''
  const isLocalhost = host === 'localhost' || host === '127.0.0.1'
  const isHttps = isBrowser ? window.location.protocol === 'https:' : false
  const conservative = isHttps && !isLocalhost

  // 524（代理超时）经常是因为“单个分片请求持续时间太长”（上行带宽低、并发抢占等）。
  // 所以 https 线上环境默认分片更小 + 并发更低，确保每个 chunk 尽量在代理超时窗口内完成。
  const chunkSize = Number.isFinite(options?.chunkSize)
    ? options.chunkSize
    : conservative
      ? 1 * 1024 * 1024
      : 16 * 1024 * 1024

  const chunkThreshold = Number.isFinite(options?.chunkThreshold)
    ? options.chunkThreshold
    : conservative
      ? 1 * 1024 * 1024
      : 64 * 1024 * 1024

  const concurrency = Number.isFinite(options?.concurrency) ? options.concurrency : conservative ? 1 : 4
  const retries = Number.isFinite(options?.retries) ? options.retries : 5
  const timeoutMs = Number.isFinite(options?.timeoutMs) ? options.timeoutMs : 10 * 60 * 1000
  const onProgress = typeof options?.onProgress === 'function' ? options.onProgress : null

  if (!file) {
    throw new Error('file is required')
  }

  if (file.size <= chunkThreshold) {
    const form = new FormData()
    form.append('file', file)
    const { data } = await postWithRetry(
      '/files/upload',
      form,
      {
        // 不要手动设置 multipart Content-Type，让浏览器自动带 boundary
        timeout: timeoutMs,
        onUploadProgress: (e) => {
          if (!onProgress) return
          const total = e?.total || file.size || 0
          const loaded = Math.min(e?.loaded || 0, total)
          const pct = total > 0 ? Math.floor((loaded / total) * 100) : 0
          onProgress(pct)
        },
      },
      { retries },
    )
    if (onProgress) onProgress(100)
    return data
  }

  const initPayload = {
    filename: file.name || 'file',
    size: file.size,
    chunkSize,
    contentType: file.type || 'application/octet-stream',
  }
  const initResp = await postWithRetry('/files/upload/init', initPayload, { timeout: timeoutMs }, { retries })
  const uploadId = initResp?.data?.data?.uploadId
  const totalChunks = initResp?.data?.data?.totalChunks
  const serverChunkSize = initResp?.data?.data?.chunkSize || chunkSize

  if (!uploadId || !totalChunks) {
    throw new Error('上传初始化失败')
  }

  let totalLoaded = 0
  const loadedByIndex = new Array(totalChunks).fill(0)

  const updateProgress = (index, chunkLoaded) => {
    const prev = loadedByIndex[index] || 0

    // 注意：分片失败重试时，浏览器的 onUploadProgress 会从 0 重新开始，
    // 如果直接写入 next，会导致整体进度回退。
    // 这里使用“单调不减”的 per-chunk 进度，让 UI 不回跳。
    const next = Math.max(prev, Math.max(0, chunkLoaded || 0))
    loadedByIndex[index] = next

    totalLoaded = Math.min(file.size, Math.max(0, totalLoaded + (next - prev)))
    if (!onProgress) return
    const pct = file.size > 0 ? Math.floor((totalLoaded / file.size) * 100) : 0
    onProgress(pct)
  }

  const uploadOne = async (i) => {
    const start = i * serverChunkSize
    const end = Math.min(file.size, start + serverChunkSize)
    const blob = file.slice(start, end)

    const form = new FormData()
    form.append('uploadId', uploadId)
    form.append('index', String(i))
    form.append('file', blob, file.name)

    await postWithRetry(
      '/files/upload/chunk',
      form,
      {
        // 不要手动设置 multipart Content-Type，让浏览器自动带 boundary
        timeout: timeoutMs,
        onUploadProgress: (e) => {
          const chunkTotal = e?.total || (end - start) || 0
          const chunkLoaded = Math.min(e?.loaded || 0, chunkTotal)
          updateProgress(i, chunkLoaded)
        },
      },
      { retries },
    )

    updateProgress(i, end - start)
  }

  let nextIndex = 0
  const worker = async () => {
    while (true) {
      const i = nextIndex
      nextIndex += 1
      if (i >= totalChunks) return
      await uploadOne(i)
    }
  }

  const workers = Array.from(
    { length: Math.max(1, Math.min(concurrency, totalChunks)) },
    () => worker(),
  )
  await Promise.all(workers)

  const completeResp = await postWithRetry(
    '/files/upload/complete',
    null,
    {
      params: { uploadId },
      timeout: timeoutMs,
      // 允许 202（处理中）作为正常响应，后续会轮询 result
      validateStatus: (status) => status >= 200 && status < 300,
    },
    { retries },
  )

  const tryExtractUrl = (resp) => {
    const url = resp?.data?.data
    return typeof url === 'string' ? url : ''
  }

  let url = tryExtractUrl(completeResp)
  const looksLikeUploadUrl = (u) => typeof u === 'string' && u.startsWith('/api/uploads/')

  if (!looksLikeUploadUrl(url)) {
    // 后端大文件会异步合并：complete 先返回 202 + PROCESSING，前端轮询 result 获取最终 url
    const deadline = Date.now() + Math.max(10_000, timeoutMs)
    let delay = 1000
    while (Date.now() < deadline) {
      await sleep(delay)
      delay = Math.min(Math.floor(delay * 1.5), 5000)

      const r = await http.get('/files/upload/result', {
        params: { uploadId },
        timeout: timeoutMs,
        validateStatus: (status) => status >= 200 && status < 300,
      })
      url = tryExtractUrl(r)
      if (looksLikeUploadUrl(url)) break
    }
  }

  if (!looksLikeUploadUrl(url)) {
    throw new Error('上传合并中或失败：未获取到最终地址，请稍后重试')
  }

  if (onProgress) onProgress(100)
  return { ...(completeResp?.data || {}), data: url }
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
