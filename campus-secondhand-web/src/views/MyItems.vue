<script setup>
import { onMounted, ref } from 'vue'
import { deleteItem, listMyItems, offShelfItem, onShelfItem } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')

const keyword = ref('')
const category = ref('')
const status = ref('')

const actionLoadingId = ref(null)
const actionError = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const kw = keyword.value.trim()
    const cat = category.value.trim().toUpperCase()
    const resp = await listMyItems({ keyword: kw, category: cat, status: status.value, page: 0, size: 50 })
    items.value = resp.data || []
  } catch (e) {
    items.value = []
    const data = e?.response?.data
    errorMsg.value =
      data?.message ||
      (typeof data === 'string' ? data.slice(0, 200) : (data ? JSON.stringify(data).slice(0, 200) : '')) ||
      e?.message ||
      '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function doOffShelf(id) {
  actionError.value = ''
  actionLoadingId.value = id
  try {
    await offShelfItem(id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '下架失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doOnShelf(id) {
  actionError.value = ''
  actionLoadingId.value = id
  try {
    await onShelfItem(id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '上架失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doDelete(it) {
  if (!it?.id) return
  const ok = window.confirm(`确认删除商品：${it.title || it.id}？删除后不可恢复。`)
  if (!ok) return
  actionError.value = ''
  actionLoadingId.value = it.id
  try {
    await deleteItem(it.id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '删除失败'
  } finally {
    actionLoadingId.value = null
  }
}
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <div class="title">我的发布</div>
        <div class="sub">发布后会进入审核，通过后才会出现在首页。</div>
      </div>
    </div>

    <div class="search card">
      <div class="row">
        <input v-model="keyword" class="input" placeholder="关键词" />
        <input v-model="category" class="input" placeholder="分类：BOOK / DIGITAL / LIFE" />
        <select v-model="status" class="select">
          <option value="">全部状态</option>
          <option value="PENDING_REVIEW">待审核</option>
          <option value="ON_SALE">已上架</option>
          <option value="OFF_SHELF">已下架</option>
          <option value="REJECTED">已驳回</option>
          <option value="LOCKED">交易中</option>
          <option value="SOLD">已售出</option>
        </select>
        <button class="primary" type="button" @click="load">查询</button>
      </div>
      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
      <div v-if="actionError" class="error">{{ actionError }}</div>
    </div>

    <div v-if="loading" class="muted">加载中...</div>

    <div v-else class="grid">
      <div v-for="it in items" :key="it.id" class="item card card-hover">
        <div class="img">
          <img v-if="it.coverImageUrl" :src="it.coverImageUrl" alt="" loading="lazy" />
        </div>
        <div class="body">
          <div class="name">{{ it.title }}</div>
          <div class="chips">
            <span class="chip">{{ it.category }}</span>
            <span class="chip">{{ it.status }}</span>
            <span class="chip">成色 {{ it.conditionLevel }}</span>
          </div>
          <div class="price">￥{{ it.price }}</div>
          <div v-if="it.status === 'REJECTED' && it.auditReason" class="reason">驳回原因：{{ it.auditReason }}</div>

          <div class="actions">
            <button
              v-if="it.status === 'ON_SALE'"
              class="ghost"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doOffShelf(it.id)"
            >
              {{ actionLoadingId === it.id ? '下架中...' : '下架' }}
            </button>
            <button
              v-else-if="it.status === 'OFF_SHELF'"
              class="ghost"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doOnShelf(it.id)"
            >
              {{ actionLoadingId === it.id ? '上架中...' : '上架' }}
            </button>

            <button
              v-if="it.status === 'OFF_SHELF' || it.status === 'PENDING_REVIEW' || it.status === 'REJECTED'"
              class="danger"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doDelete(it)"
            >
              {{ actionLoadingId === it.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && items.length === 0" class="empty">
      <div class="emptyTitle">暂无记录</div>
      <div class="emptySub">去“发布”提交一个商品吧。</div>
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

.search {
  padding: 12px;
  margin-bottom: 14px;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr 200px auto;
  gap: 10px;
  align-items: center;
}

@media (max-width: 900px) {
  .row {
    grid-template-columns: 1fr;
  }
}

.primary {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.primary:hover {
  background: var(--primary-hover);
}

.error {
  margin-top: 10px;
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

.item {
  overflow: hidden;
}

.img {
  height: 160px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.body {
  padding: 12px;
}

.name {
  font-weight: 900;
  line-height: 1.35;
  margin-bottom: 10px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.price {
  font-size: 18px;
  font-weight: 900;
  color: #ef4444;
}

.reason {
  margin-top: 8px;
  font-size: 13px;
  color: var(--muted);
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ghost {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.ghost:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.danger {
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.28);
  color: #b91c1c;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.danger:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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
