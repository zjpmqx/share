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
  <div class="page">
    <div class="head">
      <div>
        <div class="title">好物分享管理</div>
        <div class="sub">管理员可删除违规/无效分享，删除后会同时清理本地上传文件。</div>
      </div>
      <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
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
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 6px 0 14px;
}

.title {
  font-size: 20px;
  font-weight: 900;
  color: #fff;
}

.sub {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.66);
  font-size: 13px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px;
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
  color: #fff;
  margin-bottom: 8px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}

.chip {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 4px 8px;
  border-radius: 999px;
}

.url {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
  word-break: break-all;
}

.ghost {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
}

.danger {
  background: rgba(185, 28, 28, 0.16);
  border: 1px solid rgba(185, 28, 28, 0.28);
  color: #fecaca;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.18);
  border-radius: 14px;
  color: #fecaca;
  font-size: 13px;
}

.empty {
  margin-top: 14px;
  text-align: center;
  color: rgba(255, 255, 255, 0.66);
}

.emptyTitle {
  font-weight: 800;
}
</style>
