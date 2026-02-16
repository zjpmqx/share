<script setup>
import { onMounted, ref } from 'vue'
import { listShares } from '../services/api'

const loading = ref(false)
const errorMsg = ref('')
const shares = ref([])

const COVER_RE = /\[\[cover:([^\]]+)\]\]/g

function parseShareContent(raw) {
  const s = String(raw || '')
  let cover = ''
  let m
  while ((m = COVER_RE.exec(s)) !== null) {
    const url = (m[1] || '').trim()
    if (url) cover = url
  }
  const text = s.replace(COVER_RE, '').trim()
  return { cover, text }
}

function isSameList(a, b) {
  if (!Array.isArray(a) || !Array.isArray(b)) return false
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i]?.id !== b[i]?.id) return false
  }
  return true
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await listShares({ page: 0, size: 30 })
    const next = (resp.data || []).map((it) => {
      const parts = parseShareContent(it?.content)
      return { ...it, _cover: parts.cover, _text: parts.text }
    })
    if (!isSameList(shares.value, next)) {
      shares.value = next
    }
  } catch (e) {
    shares.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function previewKind(it) {
  const t = (it?.mediaType || 'NONE').toUpperCase()
  if (t === 'IMAGE' && it.mediaUrl) return 'image'
  if (t === 'VIDEO' && it.mediaUrl) return 'video'
  if (t === 'FILE' && it.mediaUrl) return 'file'
  if (t === 'URL' && it.linkUrl) return 'link'
  return 'none'
}

function coverPreview(it) {
  const t = (it?.mediaType || 'NONE').toUpperCase()
  if (!it?._cover) return ''
  if (t === 'IMAGE' || t === 'VIDEO') return ''
  return it._cover
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <div class="title">好物分享</div>
        <div class="sub">免费无私分享：图片 / 视频 / 链接，也可以在下方留言交流。</div>
      </div>

      <RouterLink class="primary" to="/shares/publish">发布分享</RouterLink>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

    <div v-if="loading && shares.length === 0" class="muted">加载中...</div>

    <div v-else class="grid">
      <RouterLink v-for="it in shares" :key="it.id" class="card" :to="`/shares/${it.id}`">
        <div class="media">
          <img v-if="coverPreview(it)" :src="coverPreview(it)" alt="" loading="lazy" />
          <img v-else-if="previewKind(it) === 'image'" :src="it.mediaUrl" alt="" loading="lazy" />
          <video v-else-if="previewKind(it) === 'video'" :src="it.mediaUrl" muted playsinline />
          <div v-else-if="previewKind(it) === 'link'" class="linkBox">{{ it.linkUrl }}</div>
          <div v-else class="emptyMedia">暂无媒体</div>
        </div>

        <div class="body">
          <div class="cardTitle">{{ it.title }}</div>
          <div v-if="it._text" class="desc">{{ it._text }}</div>
          <a
            v-if="previewKind(it) === 'file'"
            class="fileBtn"
            :href="it.mediaUrl"
            target="_blank"
            rel="noopener noreferrer"
            :download="(it?.mediaUrl || '').split('/').pop() || 'attachment'"
            @click.stop
          >
            下载附件
          </a>
          <div class="meta">
            <span class="chip">{{ it.mediaType || 'NONE' }}</span>
            <span class="chip">ID {{ it.id }}</span>
          </div>
        </div>
      </RouterLink>
    </div>

    <div v-if="!loading && shares.length === 0" class="empty">
      <div class="emptyTitle">暂无分享</div>
      <div class="emptySub">去发布第一个好物分享吧。</div>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 6px 0 14px;
}

.title {
  font-size: 22px;
  font-weight: 900;
}

.sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.primary {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-decoration: none;
}

.error {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.18);
  border-radius: var(--radius);
  color: var(--danger);
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.card {
  display: block;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  box-shadow: var(--shadow-sm);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  border-color: var(--border);
}

.media {
  height: 180px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.media img,
.media video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.linkBox {
  padding: 10px;
  font-size: 12px;
  color: var(--muted);
  word-break: break-all;
}

.emptyMedia {
  color: var(--muted);
  font-size: 13px;
}

.body {
  padding: 12px;
}

.cardTitle {
  font-weight: 900;
  margin-bottom: 8px;
  line-height: 1.35;
}

.desc {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--muted);
}

.chip {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  padding: 4px 8px;
  border-radius: 999px;
}

.fileBtn {
  display: inline-block;
  margin: 6px 0 10px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-2);
  background: var(--surface-2);
  color: var(--primary);
  font-size: 12px;
  text-decoration: none;
}

.empty {
  margin-top: 18px;
  text-align: center;
  padding: 18px 12px;
  color: var(--muted);
}

.emptyTitle {
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
}

.emptySub {
  font-size: 13px;
}
</style>
