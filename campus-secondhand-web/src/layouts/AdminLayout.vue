<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminSidebar from '../components/AdminSidebar.vue'

const route = useRoute()

const pageTitle = computed(() => {
  const map = {
    adminPending: '待审核商品',
    adminItems: '商品管理',
    adminShares: '好物分享管理',
    adminUsers: '用户管理',
  }
  return map[route.name] || '管理后台'
})
</script>

<template>
  <div class="admin-shell wrap">
    <aside class="side">
      <div class="brand">
        <div class="brandBadge">Campus Market</div>
        <div class="brandTitle">二手交易后台</div>
        <div class="brandSub">清爽白色系 · 高效治理 · 体验统一</div>
      </div>
      <AdminSidebar />
    </aside>

    <div class="content">
      <header class="top">
        <div>
          <div class="title">{{ pageTitle }}</div>
          <div class="sub">让管理像逛主站一样轻盈、清晰、好用</div>
        </div>
        <div class="spark"></div>
      </header>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  --admin-bg: #f6f8ff;
  --admin-surface: #ffffff;
  --admin-surface-2: #f8faff;
  --admin-surface-strong: #f1f5ff;
  --admin-border: #dbe4ff;
  --admin-border-soft: #e8eeff;
  --admin-text: #0f172a;
  --admin-muted: #64748b;
  --admin-chip: #eef2ff;
  --admin-primary: #3b82f6;
  --admin-primary-2: #14b8a6;
  --admin-danger: #f43f5e;
  --admin-shadow: 0 12px 30px rgba(37, 99, 235, 0.09);

  min-height: 100vh;
  display: grid;
  grid-template-columns: 270px 1fr;
  background:
    radial-gradient(circle at 0% 0%, rgba(59, 130, 246, 0.12), transparent 32%),
    radial-gradient(circle at 100% 0%, rgba(45, 212, 191, 0.12), transparent 34%),
    linear-gradient(180deg, #f9fbff, var(--admin-bg));
  color: var(--admin-text);
}

.side {
  padding: 16px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid var(--admin-border-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(246, 250, 255, 0.95));
}

.brand {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 1), rgba(239, 246, 255, 1));
  border: 1px solid var(--admin-border);
  box-shadow: var(--admin-shadow);
}

.brandBadge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
  background: rgba(191, 219, 254, 0.8);
}

.brandTitle {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.2px;
}

.brandSub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--admin-muted);
}

.content {
  min-width: 0;
}

.top {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--admin-border-soft);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
}

.title {
  font-size: 20px;
  font-weight: 900;
  color: var(--admin-text);
}

.sub {
  margin-top: 2px;
  font-size: 13px;
  color: var(--admin-muted);
}

.spark {
  width: 96px;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #34d399, #f59e0b, #f472b6);
  opacity: 0.9;
}

.main {
  padding: 18px 16px 38px;
  max-width: 1240px;
  margin: 0 auto;
}

@media (max-width: 980px) {
  .wrap {
    grid-template-columns: 1fr;
  }

  .side {
    position: relative;
    height: auto;
  }
}
</style>
