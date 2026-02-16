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
  <div class="page">
    <div class="head">
      <div>
        <div class="title">用户管理</div>
        <div class="sub">展示用户最近登录 IP（Cloudflare / 代理环境会读取 X-Forwarded-For）。</div>
      </div>
      <button class="ghost" type="button" :disabled="loading" @click="load">刷新</button>
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

.ghost {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  color: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
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

.tableWrap {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  overflow: hidden;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  text-align: left;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
}

.table th {
  color: rgba(255, 255, 255, 0.66);
  font-weight: 800;
  background: rgba(255, 255, 255, 0.03);
}

.chip {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
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
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(34, 197, 94, 0.18));
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.empty {
  padding: 14px;
  color: rgba(255, 255, 255, 0.66);
}
</style>
