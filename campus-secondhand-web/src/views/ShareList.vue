<script setup>
import { onMounted, ref } from 'vue'
import { listShares } from '../services/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const errorMsg = ref('')
const shares = ref([])
const searchKeyword = ref('')
const filterType = ref('')

const mediaTypes = [
  { value: '', label: '全部类型' },
  { value: 'NONE', label: '纯文字' },
  { value: 'IMAGE', label: '图片' },
  { value: 'VIDEO', label: '视频' },
  { value: 'URL', label: '链接' },
  { value: 'FILE', label: '文件' }
]

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
    let next = (resp.data || []).map((it) => {
      const parts = parseShareContent(it?.content)
      return { ...it, _cover: parts.cover, _text: parts.text }
    })
    
    if (filterType.value) {
      next = next.filter(it => it.mediaType === filterType.value)
    }
    
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.toLowerCase()
      next = next.filter(it => 
        it.title.toLowerCase().includes(kw) || 
        it._text.toLowerCase().includes(kw)
      )
    }
    
    if (!isSameList(shares.value, next)) {
      shares.value = next
    }
  } catch (e) {
    shares.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
    ElMessage.error(errorMsg.value)
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

function getTypeIcon(type) {
  const t = (type || 'NONE').toUpperCase()
  switch (t) {
    case 'IMAGE': return 'Picture'
    case 'VIDEO': return 'VideoCamera'
    case 'URL': return 'Link'
    case 'FILE': return 'Document'
    default: return 'Document'
  }
}

function getTypeTagType(type) {
  const t = (type || 'NONE').toUpperCase()
  switch (t) {
    case 'IMAGE': return 'success'
    case 'VIDEO': return 'warning'
    case 'URL': return 'primary'
    case 'FILE': return 'danger'
    default: return 'info'
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <div class="title">
          <el-icon><ChatLineSquare /></el-icon>
          好物分享
        </div>
        <div class="sub">免费无私分享：图片 / 视频 / 链接，也可以在下方留言交流。</div>
      </div>

      <router-link class="publishBtn" to="/shares/publish">
        <el-icon><Plus /></el-icon>
        发布分享
      </router-link>
    </div>

    <el-card class="filterCard" shadow="never">
      <div class="filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索分享..."
          :prefix-icon="Search"
          clearable
          style="width: 280px;"
          @keyup.enter="load"
        />
        <el-select v-model="filterType" placeholder="选择类型" clearable style="width: 160px;">
          <el-option v-for="type in mediaTypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
      </div>
    </el-card>

    <el-skeleton v-if="loading && shares.length === 0" :rows="8" animated style="margin-top: 14px;" />

    <div v-else class="grid">
      <router-link v-for="it in shares" :key="it.id" class="card" :to="`/shares/${it.id}`">
        <el-card :body-style="{ padding: '0' }" class="cardInner" shadow="hover">
          <div class="media">
            <div v-if="coverPreview(it)" class="mediaContent">
              <el-image :src="coverPreview(it)" fit="cover" lazy />
            </div>
            <div v-else-if="previewKind(it) === 'image'" class="mediaContent">
              <el-image :src="it.mediaUrl" fit="cover" lazy />
            </div>
            <div v-else-if="previewKind(it) === 'video'" class="mediaContent">
              <video :src="it.mediaUrl" muted playsinline />
              <div class="videoOverlay">
                <el-icon class="playIcon"><VideoPlay /></el-icon>
              </div>
            </div>
            <div v-else-if="previewKind(it) === 'link'" class="linkBox">
              <el-icon class="linkIcon"><Link /></el-icon>
              <div class="linkText">{{ it.linkUrl }}</div>
            </div>
            <div v-else class="emptyMedia">
              <el-icon><Document /></el-icon>
            </div>
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
              <el-icon><Download /></el-icon>
              下载附件
            </a>
            
            <div class="meta">
              <el-tag size="small" :type="getTypeTagType(it.mediaType)">
                <el-icon><component :is="getTypeIcon(it.mediaType)" /></el-icon>
                {{ it.mediaType || 'NONE' }}
              </el-tag>
              <el-tag size="small" type="info">ID {{ it.id }}</el-tag>
            </div>
          </div>
        </el-card>
      </router-link>
    </div>

    <div v-if="!loading && shares.length === 0" class="empty">
      <el-empty description="暂无分享">
        <router-link to="/shares/publish">
          <el-button type="primary">去发布第一个分享</el-button>
        </router-link>
      </el-empty>
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
  flex-wrap: wrap;
}

.title {
  font-size: 22px;
  font-weight: 900;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.publishBtn {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 18px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.publishBtn:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

.filterCard {
  margin-bottom: 14px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
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
  
  .filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filters .el-input,
  .filters .el-select {
    width: 100% !important;
  }
}

.card {
  display: block;
  text-decoration: none;
  color: inherit;
}

.cardInner {
  height: 100%;
  transition: transform 0.15s ease;
}

.cardInner:hover {
  transform: translateY(-2px);
}

.media {
  height: 180px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.mediaContent {
  width: 100%;
  height: 100%;
}

.mediaContent :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.mediaContent video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.videoOverlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.playIcon {
  font-size: 48px;
  color: #fff;
}

.linkBox {
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.linkIcon {
  font-size: 40px;
  color: var(--primary);
}

.linkText {
  font-size: 12px;
  color: var(--muted);
  word-break: break-all;
  max-width: 100%;
}

.emptyMedia {
  color: var(--muted);
  font-size: 40px;
}

.body {
  padding: 14px;
}

.cardTitle {
  font-weight: 800;
  margin-bottom: 8px;
  line-height: 1.35;
  font-size: 15px;
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
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
}

.fileBtn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 6px 0 10px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-2);
  background: var(--surface-2);
  color: var(--primary);
  font-size: 12px;
  text-decoration: none;
  transition: all 0.15s ease;
}

.fileBtn:hover {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.empty {
  margin-top: 18px;
}
</style>
