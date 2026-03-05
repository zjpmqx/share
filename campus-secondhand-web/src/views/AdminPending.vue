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
  <div class="page admin-page">
    <div class="pageHead">
      <div>
        <div class="title">待审核商品</div>
        <div class="sub">审核通过后将自动展示在用户端首页，驳回可附理由提高沟通效率。</div>
      </div>
      <div class="headRight">
        <div class="tag">当前 {{ items.length }} 条</div>
        <button class="ghost" type="button" :disabled="loading" @click="load">刷新列表</button>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="loading" class="muted">加载中...</div>

    <div v-else>
      <div v-for="it in items" :key="it.id" class="card">
        <div class="head">
          <div class="name">{{ it.title }}</div>
          <div class="price">￥{{ it.price }}</div>
        </div>
        <div class="meta">
          <span class="chip">分类 {{ it.category }}</span>
          <span class="chip">成色 {{ it.conditionLevel }}</span>
          <span class="chip">状态 {{ it.status }}</span>
        </div>
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
.admin-page {
  color: var(--admin-text);
}

.pageHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  margin: 4px 0 14px;
}

.headRight {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-size: 22px;
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
  background: linear-gradient(90deg, #dbeafe, #ccfbf1);
  color: #0f172a;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  font-weight: 700;
}

.card {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border-soft);
  border-radius: 18px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: var(--admin-shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.12);
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.name {
  font-weight: 900;
}

.price {
  font-weight: 900;
  color: #d97706;
}

.meta {
  margin: 10px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  font-size: 12px;
  color: #334155;
  border: 1px solid #dbeafe;
  background: #f0f9ff;
  padding: 4px 8px;
  border-radius: 999px;
}

.desc {
  font-size: 14px;
  line-height: 1.6;
  color: #334155;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn {
  border: 1px solid transparent;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  color: #fff;
  transition: transform 0.15s ease, filter 0.15s ease;
}

.btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.ok {
  background: linear-gradient(90deg, #0ea5e9, #22c55e);
}

.danger {
  background: linear-gradient(90deg, #f43f5e, #fb7185);
}

.ghost {
  background: #ffffff;
  border: 1px solid var(--admin-border);
  color: var(--admin-text);
  padding: 8px 12px;
  border-radius: 12px;
}

.empty,
.muted {
  color: var(--admin-muted);
  font-size: 13px;
}

.error {
  margin: 8px 0 12px;
  color: #be123c;
  border: 1px solid #fecdd3;
  background: #fff1f2;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 13px;
}
</style>
