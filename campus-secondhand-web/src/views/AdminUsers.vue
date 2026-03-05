<script setup>
import { onMounted, ref } from 'vue'
import { adminListUsers } from '../services/api'

const loading = ref(false)
const errorMsg = ref('')
const users = ref([])

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await adminListUsers({ page: 0, size: 50 })
    users.value = resp.data || []
  } catch (e) {
    users.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page admin-page">
    <div class="head">
      <div>
        <div class="title">用户管理</div>
        <div class="sub">展示用户最近登录信息，用于风控与行为排查。</div>
      </div>
      <div class="headRight">
        <div class="tag">用户 {{ users.length }}</div>
        <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="loading" class="muted">加载中...</div>

    <div v-else class="tableWrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>角色</th>
            <th>头像</th>
            <th>最近登录IP</th>
            <th>最近登录时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td><span class="chip">{{ u.role }}</span></td>
            <td>
              <div class="avatar">
                <img v-if="u.avatarUrl" :src="u.avatarUrl" alt="" />
                <div v-else class="avatarFallback"></div>
              </div>
            </td>
            <td class="mono">{{ u.lastLoginIp || '-' }}</td>
            <td class="mono">{{ u.lastLoginAt || '-' }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="users.length === 0" class="empty">暂无用户</div>
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
  background: linear-gradient(90deg, #e0e7ff, #dbeafe);
  color: #1e3a8a;
  border: 1px solid #c7d2fe;
  font-size: 12px;
  font-weight: 700;
}

.ghost {
  background: #ffffff;
  border: 1px solid var(--admin-border);
  color: var(--admin-text);
  padding: 8px 12px;
  border-radius: 12px;
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

.muted {
  color: var(--admin-muted);
}

.tableWrap {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border-soft);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: var(--admin-shadow);
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e8eeff;
  text-align: left;
  color: #334155;
  font-size: 13px;
}

.table th {
  color: #475569;
  font-weight: 800;
  background: linear-gradient(90deg, #eff6ff, #ecfeff);
}

.chip {
  background: #eef2ff;
  border: 1px solid #dbeafe;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #334155;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid var(--admin-border);
  background: #f8fafc;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatarFallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #93c5fd, #86efac);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.empty {
  padding: 14px;
  color: var(--admin-muted);
}
</style>
