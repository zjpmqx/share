import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const tunnelHost = process.env.VITE_TUNNEL_HOST || process.env.VITE_PUBLIC_HOST
const tunnelClientPort = Number(process.env.VITE_HMR_CLIENT_PORT || 443)
const disableHmr =
  process.env.VITE_DISABLE_HMR === '1' ||
  String(process.env.VITE_DISABLE_HMR || '').toLowerCase() === 'true'

function cacheHeadersPlugin() {
  const applyHeaders = (middlewares) => {
    middlewares.use((req, res, next) => {
      const url = req?.url || ''
      const path = url.split('?')[0]

      const isAsset =
        path.startsWith('/assets/') ||
        path.endsWith('.js') ||
        path.endsWith('.css') ||
        path.endsWith('.png') ||
        path.endsWith('.jpg') ||
        path.endsWith('.jpeg') ||
        path.endsWith('.gif') ||
        path.endsWith('.webp') ||
        path.endsWith('.svg') ||
        path.endsWith('.ico') ||
        path.endsWith('.woff') ||
        path.endsWith('.woff2') ||
        path.endsWith('.ttf')

      if (isAsset) {
        res.setHeader('Cache-Control', 'public, max-age=31536000, immutable')
      } else if (path === '/' || path.endsWith('.html')) {
        res.setHeader('Cache-Control', 'no-cache')
      }

      next()
    })
  }

  return {
    name: 'cache-headers-plugin',
    configurePreviewServer(server) {
      applyHeaders(server.middlewares)
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), cacheHeadersPlugin()],
  resolve: {
    dedupe: ['vue'],
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    allowedHosts: ['localhost', '127.0.0.1', 'all.zjpnb.dpdns.org', 'www.zjpnb.dpdns.org'],
    headers: tunnelHost ? {} : { 'Cache-Control': 'no-store' },
    hmr: disableHmr
      ? false
      : tunnelHost
        ? {
            host: tunnelHost,
            protocol: 'wss',
            clientPort: tunnelClientPort,
          }
        : true,
    proxy: {
      '/api': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
      },
      '/api/uploads': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
      },
    },
  },
  preview: {
    host: true,
    port: 5173,
    strictPort: true,
    allowedHosts: ['localhost', '127.0.0.1', 'all.zjpnb.dpdns.org', 'www.zjpnb.dpdns.org'],
    proxy: {
      '/api': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
      },
      '/api/uploads': {
        target: 'http://localhost:18081',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
      },
    },
  },
})
