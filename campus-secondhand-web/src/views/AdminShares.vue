<script setup>
import { onMounted, ref } from 'vue'
import { adminDeleteShare, listShares } from '../services/api'

const loading = ref(false)
const shares = ref([])
const errorMsg = ref('')
const actionLoadingId = ref(null)
const actionError = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await listShares({ page: 0, size: 50 })
    shares.value = resp.data || []
  } catch (e) {
    shares.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function doDelete(it) {
  if (!it?.id) return
  const ok = window.confirm(`确认删除分享：${it.title || it.id}？（将同时删除评论与本地媒体文件）`)
  if (!ok) return
  actionError.value = ''
  actionLoadingId.value = it.id
  try {
    await adminDeleteShare(it.id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '删除失败'
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <div>
        <div class="title">好物分享管理</div>
        <div class="sub">集中处理违规内容，自动联动清理关联媒体文件。</div>
      </div>
      <div class="headRight">
        <div class="tag">共 {{ shares.length }} 条</div>
        <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="actionError" class="error">{{ actionError }}</div>

    <div v-if="loading" class="muted">加载中...</div>

    <div v-else class="list">
      <div v-for="it in shares" :key="it.id" class="card">
        <div class="row">
          <div class="left">
            <div class="name">{{ it.title }}</div>
            <div class="meta">
              <span class="chip">{{ it.mediaType || 'NONE' }}</span>
              <span class="chip">ID {{ it.id }}</span>
              <span class="chip">用户 {{ it.userId }}</span>
            </div>
            <div v-if="it.mediaUrl" class="url">媒体：{{ it.mediaUrl }}</div>
            <div v-if="it.linkUrl" class="url">链接：{{ it.linkUrl }}</div>
          </div>
          <button class="danger" type="button" :disabled="actionLoadingId === it.id" @click="doDelete(it)">
            {{ actionLoadingId === it.id ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="!loading && shares.length === 0" class="empty">
      <div class="emptyTitle">暂无分享</div>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  color: var(--admin-text);
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 6px 0 14px;
}

.headRight {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-size: 20px;
  font-weight: 900;
}

.sub {
  margin-top: 4px;
  color: var(--admin-muted);
  font-size: 13px;
}

.tag {
  height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, #ddd6fe, #dbeafe);
  color: #312e81;
  border: 1px solid #c4b5fd;
  font-size: 12px;
  font-weight: 700;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border-soft);
  border-radius: 18px;
  padding: 12px;
  box-shadow: var(--admin-shadow);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.left {
  min-width: 0;
}

.name {
  font-weight: 900;
  margin-bottom: 8px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 8px;
}

.chip {
  background: #eef2ff;
  border: 1px solid #ddd6fe;
  padding: 4px 8px;
  border-radius: 999px;
  color: #3730a3;
}

.url {
  font-size: 12px;
  color: var(--admin-muted);
  word-break: break-all;
}

.ghost {
  background: #ffffff;
  border: 1px solid var(--admin-border);
  color: var(--admin-text);
  padding: 8px 12px;
  border-radius: 12px;
}

.danger {
  background: linear-gradient(90deg, #ffe4e6, #fecdd3);
  border: 1px solid #fda4af;
  color: #9f1239;
  padding: 8px 12px;
  border-radius: 12px;
  white-space: nowrap;
}

.danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  border-radius: 12px;
  color: #be123c;
  font-size: 13px;
}

.empty,
.muted {
  color: var(--admin-muted);
}

.empty {
  margin-top: 14px;
  text-align: center;
}

.emptyTitle {
  font-weight: 800;
}
</style>
