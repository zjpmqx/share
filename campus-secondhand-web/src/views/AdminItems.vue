<script setup>
import { onMounted, ref } from 'vue'
import { adminListItems, adminOffShelfItem, adminDeleteItem, adminOffShelfAll, adminDeleteAllItems } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')

const status = ref('')

const actionLoadingId = ref(null)
const actionError = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await adminListItems({ page: 0, size: 50, status: status.value })
    items.value = resp.data || []
  } catch (e) {
    items.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function doOffShelf(it) {
  if (!it?.id) return
  const ok = window.confirm(`确认下架商品：${it.title || it.id}？`)
  if (!ok) return
  actionError.value = ''
  actionLoadingId.value = it.id
  try {
    await adminOffShelfItem(it.id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '下架失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doDelete(it) {
  if (!it?.id) return
  const ok = window.confirm(`确认删除商品：${it.title || it.id}？（将同时删除图片/留言/订单记录及本地文件）`)
  if (!ok) return
  actionError.value = ''
  actionLoadingId.value = it.id
  try {
    await adminDeleteItem(it.id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '删除失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doOffShelfAll() {
  const confirm = prompt('输入 OFF_SHELF_ALL 确认下架所有在售商品：') || ''
  if (confirm !== 'OFF_SHELF_ALL') return
  actionError.value = ''
  try {
    await adminOffShelfAll(confirm)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '下架全部失败'
  }
}

async function doDeleteAll() {
  const confirm = prompt('危险操作：输入 DELETE_ALL 确认删除所有商品：') || ''
  if (confirm !== 'DELETE_ALL') return
  const ok = window.confirm('再次确认：此操作不可恢复，确定删除所有商品吗？')
  if (!ok) return
  actionError.value = ''
  try {
    await adminDeleteAllItems(confirm)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '删除全部失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <div class="title">商品管理</div>
        <div class="sub">管理员可对商品执行下架/删除操作。删除将清理图片/留言/订单及本地文件。</div>
      </div>
      <div class="ops">
        <select v-model="status" class="select" :disabled="loading" @change="load">
          <option value="">全部状态</option>
          <option value="PENDING_REVIEW">PENDING_REVIEW</option>
          <option value="ON_SALE">ON_SALE</option>
          <option value="OFF_SHELF">OFF_SHELF</option>
          <option value="LOCKED">LOCKED</option>
          <option value="SOLD">SOLD</option>
          <option value="REJECTED">REJECTED</option>
        </select>
        <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
        <button class="warn" type="button" @click="doOffShelfAll">下架全部在售</button>
        <button class="danger" type="button" @click="doDeleteAll">删除全部商品</button>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="actionError" class="error">{{ actionError }}</div>

    <div v-if="loading" class="muted">加载中...</div>

    <div v-else class="list">
      <div v-for="it in items" :key="it.id" class="card">
        <div class="row">
          <div class="left">
            <div class="name">{{ it.title }}</div>
            <div class="meta">
              <span class="chip">ID {{ it.id }}</span>
              <span class="chip">分类 {{ it.category }}</span>
              <span class="chip">状态 {{ it.status }}</span>
              <span class="chip">卖家 {{ it.sellerId }}</span>
            </div>
          </div>
          <div class="actions">
            <button class="warn" type="button" :disabled="actionLoadingId === it.id" @click="doOffShelf(it)">
              {{ actionLoadingId === it.id ? '处理中...' : '下架' }}
            </button>
            <button class="danger" type="button" :disabled="actionLoadingId === it.id" @click="doDelete(it)">
              {{ actionLoadingId === it.id ? '处理中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="items.length === 0" class="empty">暂无商品</div>
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

.ops {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.select {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 10px;
  border-radius: 12px;
  outline: none;
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
}

.chip {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 4px 8px;
  border-radius: 999px;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ghost {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
}

.warn {
  background: rgba(234, 179, 8, 0.14);
  border: 1px solid rgba(234, 179, 8, 0.22);
  color: #fde68a;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  white-space: nowrap;
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

.warn:disabled,
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

.muted {
  color: rgba(255, 255, 255, 0.66);
}

.empty {
  margin-top: 14px;
  text-align: center;
  color: rgba(255, 255, 255, 0.66);
}
</style>
