import { describe, expect, it, vi } from 'vitest'

vi.mock('./auth', () => ({
  getToken: vi.fn(() => null),
  clearToken: vi.fn(),
}))

describe('http helpers', () => {
  it('将字符串错误响应包装为 message 对象', async () => {
    const { normalizeErrorResponseData } = await import('./http')
    expect(normalizeErrorResponseData('boom')).toEqual({ message: 'boom' })
    expect(normalizeErrorResponseData({ message: 'x' })).toEqual({ message: 'x' })
  })

  it('当目标路径是后台路径时生成登录跳转地址', async () => {
    const { buildLoginRedirectTarget } = await import('./http')
    const url = {
      pathname: '/admin/items',
      search: '?page=2',
      hash: '#top',
    }
    expect(buildLoginRedirectTarget(url)).toBe('/login?redirect=%2Fadmin%2Fitems%3Fpage%3D2%23top')
  })

  it('非受保护路径不生成登录跳转地址', async () => {
    const { buildLoginRedirectTarget } = await import('./http')
    expect(buildLoginRedirectTarget({ pathname: '/shares', search: '', hash: '' })).toBe('')
  })

  it('识别分享门禁 403 错误', async () => {
    const { shouldHandleShareGate403 } = await import('./http')
    expect(shouldHandleShareGate403('/shares', 'Share gate verify required')).toBe(true)
    expect(shouldHandleShareGate403('/items', 'Share gate verify required')).toBe(false)
    expect(shouldHandleShareGate403('/shares', 'Forbidden')).toBe(false)
  })

  it('构造分享验证跳转地址', async () => {
    const { buildShareVerifyRedirectTarget } = await import('./http')
    const url = {
      pathname: '/shares/1',
      search: '?tab=comments',
      hash: '#list',
    }
    expect(buildShareVerifyRedirectTarget(url)).toBe('/shares/verify?redirect=%2Fshares%2F1%3Ftab%3Dcomments%23list')
  })

  it('已经处于验证页时不重复构造验证跳转', async () => {
    const { buildShareVerifyRedirectTarget } = await import('./http')
    const url = {
      pathname: '/shares/verify',
      search: '?redirect=%2Fshares',
      hash: '',
    }
    expect(buildShareVerifyRedirectTarget(url)).toBe('')
  })

  it('登录跳转参数会拒绝跨站地址', async () => {
    const base = 'https://campus.example'
    const resolveSafeRedirect = (target) => {
      if (typeof target !== 'string' || !target.trim()) {
        return '/'
      }

      try {
        const url = new URL(target, base)
        if (url.origin !== base || !url.pathname.startsWith('/')) {
          return '/'
        }
        return `${url.pathname}${url.search}${url.hash}`
      } catch {
        return '/'
      }
    }

    expect(resolveSafeRedirect('https://evil.example/phish')).toBe('/')
    expect(resolveSafeRedirect('//evil.example/phish')).toBe('/')
  })

  it('登录跳转参数保留站内相对路径', async () => {
    const base = 'https://campus.example'
    const resolveSafeRedirect = (target) => {
      if (typeof target !== 'string' || !target.trim()) {
        return '/'
      }

      try {
        const url = new URL(target, base)
        if (url.origin !== base || !url.pathname.startsWith('/')) {
          return '/'
        }
        return `${url.pathname}${url.search}${url.hash}`
      } catch {
        return '/'
      }
    }

    expect(resolveSafeRedirect('/orders?page=2#top')).toBe('/orders?page=2#top')
  })

  it('全局正则多次解析评论内容时不会串状态', () => {
    const COMMENT_IMG_RE = /\[\[img:([^\]]+)\]\]/g
    const parseCommentContent = (raw) => {
      const s = String(raw || '')
      const images = []
      for (const match of s.matchAll(COMMENT_IMG_RE)) {
        const url = (match[1] || '').trim()
        if (url) images.push(url)
      }
      const text = s.replaceAll(COMMENT_IMG_RE, '').trim()
      return { text, images }
    }

    expect(parseCommentContent('[[img:https://a.png]] 第一条')).toEqual({
      text: '第一条',
      images: ['https://a.png'],
    })
    expect(parseCommentContent('[[img:https://b.png]] 第二条')).toEqual({
      text: '第二条',
      images: ['https://b.png'],
    })
  })
})
