<script setup>
import { onMounted, ref } from 'vue'
import { adminAuditItem, adminPendingItems } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await adminPendingItems({ page: 0, size: 20 })
    items.value = resp.data || []
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败（需要 ADMIN 权限）'
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  await adminAuditItem(id, { action: 'APPROVE' })
  await load()
}

async function reject(id) {
  const reason = prompt('请输入驳回原因（可选）') || ''
  await adminAuditItem(id, { action: 'REJECT', reason })
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="pageHead">
      <div>
        <div class="title">待审核商品</div>
        <div class="sub">仅管理员可操作。通过后商品将出现在用户端首页。</div>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="loading">加载中...</div>

    <div v-else>
      <div v-for="it in items" :key="it.id" class="card">
        <div class="head">
          <div class="name">{{ it.title }}</div>
          <div class="price">￥{{ it.price }}</div>
        </div>
        <div class="meta">分类：{{ it.category }} | 成色：{{ it.conditionLevel }} | 状态：{{ it.status }}</div>
        <div class="desc">{{ it.description || '（无描述）' }}</div>
        <div class="actions">
          <button class="btn ok" type="button" @click="approve(it.id)">通过</button>
          <button class="btn danger" type="button" @click="reject(it.id)">驳回</button>
        </div>
      </div>

      <div v-if="items.length === 0" class="empty">暂无待审核商品</div>
    </div>
  </div>
</template>

<style scoped>
.pageHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 6px 0 14px;
}

.title {
  font-size: 22px;
  font-weight: 800;
}

.sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-sm);
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.name {
  font-weight: 800;
}

.price {
  font-weight: 800;
}

.meta {
  margin: 8px 0;
  font-size: 12px;
  color: var(--muted);
}

.desc {
  font-size: 14px;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.btn {
  border: 0;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: #fff;
  transition: transform 0.15s ease;
}

.btn:hover {
  transform: translateY(-1px);
}

.ok {
  background: #059669;
}

.danger {
  background: var(--danger);
}

.empty {
  color: var(--muted);
  font-size: 13px;
}

.error {
  margin: 8px 0;
  color: var(--danger);
  font-size: 13px;
}
</style>
