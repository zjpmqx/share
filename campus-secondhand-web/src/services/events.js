let source = null
let retryTimer = null
let retryMs = 1000
const itemApprovedListeners = new Set()

export function ensureEvents() {
  if (source) return

  const connect = () => {
    if (source) return
    source = new EventSource('/api/events/stream')

    source.addEventListener('open', () => {
      retryMs = 1000
    })

    source.addEventListener('item-approved', (e) => {
      let payload = null
      try {
        payload = e?.data ? JSON.parse(e.data) : null
      } catch {
        payload = null
      }

      for (const fn of itemApprovedListeners) {
        try {
          fn(payload)
        } catch {
        }
      }
    })

    source.addEventListener('error', () => {
      try {
        source?.close()
      } catch {
      }
      source = null
      if (retryTimer) return
      retryTimer = window.setTimeout(() => {
        retryTimer = null
        retryMs = Math.min(Math.floor(retryMs * 1.5), 15000)
        connect()
      }, retryMs)
    })
  }

  connect()
}

export function onItemApproved(listener) {
  itemApprovedListeners.add(listener)
  return () => itemApprovedListeners.delete(listener)
}
