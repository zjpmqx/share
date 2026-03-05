import { http } from './http'

function stripTrailingSlash(v) {
  return String(v || '').replace(/\/+$/, '')
}

function isLocalHost(host) {
  const h = String(host || '').toLowerCase()
  return h === 'localhost' || h === '127.0.0.1' || h.startsWith('192.168.') || h.startsWith('10.') || h.startsWith('172.')
}

function resolveUploadOrigin() {
  const fromEnv = stripTrailingSlash(import.meta.env.VITE_UPLOAD_ORIGIN)
  if (fromEnv) return fromEnv

  if (!import.meta.env.DEV || typeof window === 'undefined') return ''

  const host = window.location.hostname
  if (!isLocalHost(host)) return ''

  const protocol = window.location.protocol || 'http:'
  return `${protocol}//${host}:18081`
}

function normalizeAssetUrl(url) {
  if (!url || typeof url !== 'string') return url

  const raw = url.trim()
  const uploadOrigin = resolveUploadOrigin()

  if (raw.startsWith('/')) {
    if (uploadOrigin && raw.startsWith('/api/uploads/')) {
      return `${uploadOrigin}${raw}`
    }
    return raw
  }

  try {
    const u = new URL(raw)
    const host = (u.hostname || '').toLowerCase()
    if (isLocalHost(host)) {
      const localPath = `${u.pathname}${u.search}${u.hash}`
      if (uploadOrigin && localPath.startsWith('/api/uploads/')) {
        return `${uploadOrigin}${localPath}`
      }
      return localPath
    }
  } catch {
    return raw
  }

  return raw
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

export async function verifyShareGate(payload) {
  const { data } = await http.post('/auth/share-gate/verify', payload)
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

const CHUNK_SIZE = 8 * 1024 * 1024
const CHUNK_UPLOAD_TIMEOUT = 180 * 1000
const CHUNK_MAX_RETRIES = 3
const CONCURRENT_CHUNKS = 4

export async function uploadFile(file, options = {}) {
  const onProgress = typeof options?.onProgress === 'function' ? options.onProgress : null
  const timeoutMs = Number.isFinite(options?.timeoutMs) ? options.timeoutMs : 10 * 60 * 1000
  
  if (!file) {
    throw new Error('file is required')
  }

  const fileSize = file.size
  const fileName = file.name
  
  if (onProgress) {
    onProgress(1)
  }
  
  if (fileSize <= 4 * 1024 * 1024) {
    return uploadFileSimple(file, onProgress, timeoutMs)
  }
  
  return uploadFileChunked(file, onProgress, timeoutMs)
}

async function uploadFileSimple(file, onProgress, timeoutMs) {
  const form = new FormData()
  form.append('file', file)
  
  let lastProgress = 0
  
  try {
    const { data } = await http.post('/files/upload', form, {
      timeout: timeoutMs,
      headers: {
        'Content-Type': undefined,
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress) {
          const loaded = progressEvent.loaded || 0
          const total = progressEvent.total || file.size || 0
          if (total > 0) {
            const percentCompleted = Math.min(Math.round((loaded * 100) / total), 99)
            if (percentCompleted > lastProgress) {
              lastProgress = percentCompleted
              onProgress(percentCompleted)
            }
          }
        }
      },
    })
    
    if (onProgress) {
      onProgress(100)
    }
    
    return data
  } catch (error) {
    console.error('uploadFile 失败:', error)
    throw error
  }
}

async function uploadFileChunked(file, onProgress, timeoutMs) {
  const fileSize = file.size
  const fileName = file.name
  const chunkSize = CHUNK_SIZE
  const totalChunks = Math.ceil(fileSize / chunkSize)
  
  let uploadId = null
  
  try {
    if (onProgress) {
      onProgress(2)
    }
    
    const initResp = await http.post('/files/upload/init', {
      filename: fileName,
      size: fileSize,
      chunkSize: chunkSize,
      contentType: file.type || 'application/octet-stream',
    })
    
    if (initResp?.data?.code !== 0) {
      throw new Error(initResp?.data?.message || '初始化上传失败')
    }
    
    uploadId = initResp.data.data?.uploadId
    if (!uploadId) {
      throw new Error('初始化上传失败：未获取 uploadId')
    }
    
    const serverChunkSize = initResp.data.data?.chunkSize || chunkSize
    const serverTotalChunks = initResp.data.data?.totalChunks || totalChunks
    
    const progressTracker = {
      chunks: new Array(serverTotalChunks).fill(0),
      uploadedBytes: 0,
      updateProgress(chunkIndex, loaded) {
        const safeLoaded = Math.max(0, Number(loaded) || 0)
        const prevLoaded = this.chunks[chunkIndex] || 0
        this.chunks[chunkIndex] = safeLoaded
        this.uploadedBytes += (safeLoaded - prevLoaded)
        const percentCompleted = Math.min(Math.round((this.uploadedBytes * 100) / fileSize), 99)
        if (onProgress) {
          onProgress(percentCompleted)
        }
      },
      markCompleted(chunkIndex, chunkSize) {
        const safeChunkSize = Math.max(0, Number(chunkSize) || 0)
        const prevLoaded = this.chunks[chunkIndex] || 0
        this.chunks[chunkIndex] = safeChunkSize
        this.uploadedBytes += (safeChunkSize - prevLoaded)
        const percentCompleted = Math.min(Math.round((this.uploadedBytes * 100) / fileSize), 99)
        if (onProgress) {
          onProgress(percentCompleted)
        }
      }
    }
    
    async function uploadSingleChunk(index) {
      const start = index * serverChunkSize
      const end = Math.min(start + serverChunkSize, fileSize)
      const chunk = file.slice(start, end)
      const chunkLen = end - start
      
      for (let retry = 0; retry < CHUNK_MAX_RETRIES; retry++) {
        try {
          const chunkForm = new FormData()
          chunkForm.append('uploadId', uploadId)
          chunkForm.append('index', String(index))
          chunkForm.append('file', chunk, fileName + '.part' + index)
          
          await http.post('/files/upload/chunk', chunkForm, {
            timeout: CHUNK_UPLOAD_TIMEOUT,
            headers: {
              'Content-Type': undefined,
            },
            onUploadProgress: (progressEvent) => {
              progressTracker.updateProgress(index, progressEvent.loaded || 0)
            },
          })
          
          progressTracker.markCompleted(index, chunkLen)
          return { index, success: true }
        } catch (e) {
          if (retry === CHUNK_MAX_RETRIES - 1) {
            return { index, success: false, error: e }
          }
          await sleep(1000 * (retry + 1))
        }
      }
      
      return { index, success: false, error: new Error('上传失败') }
    }
    
    const results = []
    for (let i = 0; i < serverTotalChunks; i += CONCURRENT_CHUNKS) {
      const batch = []
      for (let j = i; j < Math.min(i + CONCURRENT_CHUNKS, serverTotalChunks); j++) {
        batch.push(uploadSingleChunk(j))
      }
      
      const batchResults = await Promise.all(batch)
      results.push(...batchResults)
      
      const failedChunk = batchResults.find(r => !r.success)
      if (failedChunk) {
        throw failedChunk.error || new Error(`分片 ${failedChunk.index} 上传失败`)
      }
    }
    
    if (onProgress) {
      onProgress(98)
    }
    
    let completeResp = null
    try {
      completeResp = await http.post('/files/upload/complete', null, {
        params: { uploadId },
        timeout: 90 * 1000,
      })
    } catch (e) {
      if (e?.code === 'ECONNABORTED' || e?.response?.status === 524) {
        return await pollUploadResult(uploadId, onProgress)
      }
      throw e
    }
    
    if (completeResp?.status === 202 || 
        completeResp?.data?.data === 'PROCESSING' ||
        (completeResp?.data?.code === 0 && !completeResp?.data?.data)) {
      return await pollUploadResult(uploadId, onProgress)
    }
    
    if (completeResp?.data?.code !== 0) {
      throw new Error(completeResp?.data?.message || '合并文件失败')
    }
    
    const url = completeResp.data.data
    if (!url || typeof url !== 'string') {
      throw new Error('上传失败：未返回有效地址')
    }
    
    if (onProgress) {
      onProgress(100)
    }
    
    return completeResp.data
  } catch (error) {
    console.error('分片上传失败:', error)
    throw error
  }
}

async function pollUploadResult(uploadId, onProgress, maxAttempts = 120, intervalMs = 1500) {
  if (onProgress) {
    onProgress(99)
  }
  
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const resp = await http.get('/files/upload/result', {
        params: { uploadId },
        timeout: 30 * 1000,
      })
      
      if (resp?.status === 200 && resp?.data?.code === 0) {
        const url = resp.data.data
        if (url && typeof url === 'string' && url.startsWith('/')) {
          if (onProgress) {
            onProgress(100)
          }
          return resp.data
        }
      }
      
      if (resp?.status === 202) {
        if (onProgress && i % 10 === 0) {
          onProgress(99)
        }
      }
    } catch (e) {
      console.warn('轮询上传结果:', e?.message || e)
    }
    
    await sleep(intervalMs)
  }
  
  throw new Error('上传超时：文件合并时间过长，请稍后刷新页面查看')
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
