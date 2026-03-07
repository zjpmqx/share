<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminListUsers } from '../services/api'

const loading = ref(false)
const errorMsg = ref('')
const users = ref([])

const userCount = computed(() => users.value.length)
const adminCount = computed(() => users.value.filter((user) => user.role === 'ADMIN').length)

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
  <div class="admin-page">
    <section class="hero surface-card">
      <div>
        <div class="eyebrow">User Overview</div>
        <div class="title">用户管理</div>
        <div class="sub">统一标题区、统计区和表格容器表现，便于快速浏览角色信息与最近登录记录。</div>
      </div>
      <div class="heroStats">
        <div class="statCard primary">
          <span class="statLabel">用户总数</span>
          <strong class="statValue">{{ userCount }}</strong>
        </div>
        <div class="statCard accent">
          <span class="statLabel">管理员</span>
          <strong class="statValue">{{ adminCount }}</strong>
        </div>
        <button class="ghostBtn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
    </section>

    <div v-if="errorMsg" class="feedback error">{{ errorMsg }}</div>
    <div v-if="loading" class="panelState">加载中...</div>

    <section v-else class="contentBlock">
      <div v-if="users.length" class="tableWrap surface-card">
        <div class="tableHeader">
          <div>
            <div class="tableTitle">账号与登录概览</div>
            <div class="tableSub">保持业务数据不变，仅优化表格外观、信息层级与移动端可读性。</div>
          </div>
        </div>

        <div class="tableScroll">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>角色</th>
                <th>头像</th>
                <th>最近登录 IP</th>
                <th>最近登录时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>
                  <span class="cellBadge">{{ u.id }}</span>
                </td>
                <td>
                  <div class="userCell">
                    <span class="userName">{{ u.username }}</span>
                  </div>
                </td>
                <td>
                  <span class="roleChip" :class="u.role === 'ADMIN' ? 'isAdmin' : 'isUser'">{{ u.role }}</span>
                </td>
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
        </div>
      </div>

      <div v-else class="emptyState surface-card">
        <div class="emptyTitle">暂无用户</div>
        <div class="emptySub">当前没有可展示的用户数据，稍后刷新再查看。</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  color: var(--admin-text);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.surface-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
  border: 1px solid var(--admin-border-soft);
  border-radius: 22px;
  box-shadow: var(--admin-shadow);
}

.hero {
  padding: 22px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: stretch;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.title {
  margin-top: 12px;
  font-size: 28px;
  font-weight: 900;
  line-height: 1.1;
}

.sub {
  margin-top: 8px;
  max-width: 720px;
  color: var(--admin-muted);
  font-size: 14px;
  line-height: 1.7;
}

.heroStats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.statCard {
  min-width: 132px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.statCard.primary {
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
  border-color: #bfdbfe;
}

.statCard.accent {
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border-color: #c7d2fe;
}

.statLabel {
  font-size: 12px;
  color: #475569;
}

.statValue {
  font-size: 26px;
  font-weight: 900;
  color: #0f172a;
}

.ghostBtn {
  align-self: center;
  border-radius: 14px;
  padding: 11px 16px;
  border: 1px solid var(--admin-border);
  background: #ffffff;
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease;
}

.ghostBtn:hover {
  transform: translateY(-1px);
}

.ghostBtn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.feedback {
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 13px;
}

.error {
  color: #be123c;
  border: 1px solid #fecdd3;
  background: #fff1f2;
}

.panelState,
.emptyState {
  padding: 28px 22px;
  color: var(--admin-muted);
}

.contentBlock {
  display: flex;
  flex-direction: column;
}

.tableWrap {
  overflow: hidden;
}

.tableHeader {
  padding: 20px 22px 0;
}

.tableTitle {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.tableSub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--admin-muted);
}

.tableScroll {
  overflow-x: auto;
  padding: 18px 22px 22px;
}

.table {
  width: 100%;
  min-width: 760px;
  border-collapse: separate;
  border-spacing: 0;
}

.table th,
.table td {
  padding: 14px 12px;
  border-bottom: 1px solid #e8eeff;
  text-align: left;
  color: #334155;
  font-size: 13px;
  vertical-align: middle;
}

.table th {
  font-weight: 800;
  color: #475569;
  background: linear-gradient(90deg, #eff6ff, #ecfeff);
}

.table th:first-child {
  border-top-left-radius: 14px;
}

.table th:last-child {
  border-top-right-radius: 14px;
}

.userCell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.userName {
  font-weight: 700;
  color: #0f172a;
}

.cellBadge,
.roleChip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
}

.cellBadge {
  color: #334155;
  background: #f8fafc;
  border-color: #dbeafe;
}

.roleChip.isAdmin {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: #bfdbfe;
}

.roleChip.isUser {
  color: #065f46;
  background: #d1fae5;
  border-color: #a7f3d0;
}

.avatar {
  width: 34px;
  height: 34px;
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

.emptyState {
  text-align: center;
}

.emptyTitle {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.emptySub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--admin-muted);
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
  }

  .heroStats {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .hero,
  .panelState,
  .emptyState,
  .tableHeader,
  .tableScroll {
    padding-left: 18px;
    padding-right: 18px;
  }

  .hero {
    padding-top: 18px;
    padding-bottom: 18px;
  }

  .title {
    font-size: 24px;
  }

  .ghostBtn {
    width: 100%;
  }
}
</style>