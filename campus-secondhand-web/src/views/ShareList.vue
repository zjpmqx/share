<script setup>
import {
  ChatLineSquare,
  Document,
  Download,
  Link,
  Picture,
  Plus,
  Search,
  VideoCamera,
  VideoPlay,
} from '@element-plus/icons-vue'
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
  { value: 'FILE', label: '文件' },
]

const COVER_RE = /\[\[cover:([^\]]+)\]\]/g

function parseShareContent(raw) {
  const s = String(raw || '')
  let cover = ''
  for (const match of s.matchAll(COVER_RE)) {
    const url = (match[1] || '').trim()
    if (url) cover = url
  }
  const text = s.replaceAll(COVER_RE, '').trim()
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
      next = next.filter((it) => it.mediaType === filterType.value)
    }

    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.toLowerCase()
      next = next.filter(
        (it) => it.title.toLowerCase().includes(kw) || it._text.toLowerCase().includes(kw),
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
    case 'IMAGE':
      return Picture
    case 'VIDEO':
      return VideoCamera
    case 'URL':
      return Link
    case 'FILE':
      return Document
    default:
      return Document
  }
}

function getTypeTagType(type) {
  const t = (type || 'NONE').toUpperCase()
  switch (t) {
    case 'IMAGE':
      return 'success'
    case 'VIDEO':
      return 'warning'
    case 'URL':
      return 'primary'
    case 'FILE':
      return 'danger'
    default:
      return 'info'
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="page shareListPage motion-enter-soft">
    <div class="hero glow-card motion-enter">
      <div class="heroSpark sparkLeft floaty"></div>
      <div class="heroSpark sparkRight pulse-dot"></div>
      <div class="heroMain">
        <div class="title">
          <el-icon><ChatLineSquare /></el-icon>
          好物分享
        </div>
        <div class="sub">发现同学们的精选分享：图文、视频、链接与实用附件。</div>
        <div class="heroBadges">
          <span class="badge">当前共 {{ shares.length }} 条</span>
          <span class="badge">支持多媒体</span>
          <span class="badge">可评论互动</span>
        </div>
      </div>

      <router-link class="publishBtn motion-enter-soft motion-delay-2" to="/shares/publish">
        <el-icon><Plus /></el-icon>
        发布分享
      </router-link>
    </div>

    <el-card class="filterCard glow-card motion-enter-soft motion-delay-1" shadow="never">
      <div class="filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标题或内容..."
          :prefix-icon="Search"
          clearable
          class="searchInput"
          @keyup.enter="load"
        />
        <el-select v-model="filterType" placeholder="选择类型" clearable class="typeSelect">
          <el-option v-for="type in mediaTypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-button type="primary" :icon="Search" class="searchBtn" @click="load">立即筛选</el-button>
      </div>
    </el-card>

    <el-skeleton v-if="loading && shares.length === 0" :rows="8" animated style="margin-top: 14px;" />

    <div v-else class="grid motion-enter-soft motion-delay-2">
      <router-link v-for="(it, index) in shares" :key="it.id" class="card" :style="{ '--share-delay': `${Math.min(index, 8) * 55}ms` }" :to="`/shares/${it.id}`">
        <el-card :body-style="{ padding: '0' }" class="cardInner glow-card" shadow="hover">
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
              <span>文字分享</span>
            </div>

            <div class="floatingTag">
              <el-tag size="small" :type="getTypeTagType(it.mediaType)">
                <el-icon><component :is="getTypeIcon(it.mediaType)" /></el-icon>
                {{ it.mediaType || 'NONE' }}
              </el-tag>
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
              <span class="metaId">#{{ it.id }}</span>
              <span class="metaMore">点击查看详情</span>
            </div>
          </div>
        </el-card>
      </router-link>
    </div>

    <div v-if="!loading && shares.length === 0" class="empty motion-enter motion-delay-3">
      <el-empty description="暂无分享，来当第一个发布者吧">
        <router-link to="/shares/publish">
          <el-button type="primary">去发布第一个分享</el-button>
        </router-link>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.shareListPage {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(16, 185, 129, 0.16));
  border: 1px solid rgba(148, 163, 184, 0.24);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.heroSpark {
  position: absolute;
  width: 86px;
  height: 86px;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(8px);
  opacity: 0.4;
}

.sparkLeft {
  top: -18px;
  left: -18px;
  background: rgba(59, 130, 246, 0.28);
}

.sparkRight {
  right: 16px;
  bottom: 12px;
  background: rgba(16, 185, 129, 0.22);
}

.heroMain {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 24px;
  font-weight: 900;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sub {
  color: var(--muted);
  font-size: 13px;
}

.heroBadges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.26);
  color: var(--text);
}

.publishBtn {
  background: linear-gradient(135deg, var(--primary), #6366f1);
  color: #fff;
  border: 0;
  padding: 10px 18px;
  border-radius: 12px;
  cursor: pointer;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  transition: transform 0.15s ease, filter 0.15s ease;
}

.publishBtn:hover {
  transform: translateY(-2px);
  filter: brightness(1.03);
}

.filterCard {
  border-radius: 14px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.searchInput {
  width: 340px;
}

.typeSelect {
  width: 170px;
}

.searchBtn {
  border-radius: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.card {
  display: block;
  text-decoration: none;
  color: inherit;
}

.cardInner {
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.cardInner:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.12);
}

.media {
  height: 190px;
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

.mediaContent :deep(img),
.mediaContent video {
  transition: transform var(--transition-slow);
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
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.playIcon {
  font-size: 44px;
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
  font-size: 38px;
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
  font-size: 34px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.emptyMedia span {
  font-size: 12px;
}

.floatingTag {
  position: absolute;
  top: 10px;
  left: 10px;
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
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
  min-height: 40px;
}

.fileBtn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 10px;
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

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--muted);
}

.metaId {
  font-weight: 700;
}

.empty {
  margin-top: 8px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .filters {
    flex-direction: column;
    align-items: stretch;
  }

  .searchInput,
  .typeSelect,
  .searchBtn {
    width: 100%;
  }
}
</style>
