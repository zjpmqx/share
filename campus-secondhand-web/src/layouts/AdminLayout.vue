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
  <div class="admin-shell">
    <aside class="side surface">
      <div class="brand surface">
        <div class="brandBadge">Campus Market</div>
        <div class="brandTitle">二手交易后台</div>
        <div class="brandSub">清爽白色系 · 高效治理 · 体验统一</div>
      </div>
      <AdminSidebar />
    </aside>

    <div class="content">
      <header class="top surface">
        <div>
          <div class="title">{{ pageTitle }}</div>
          <div class="sub">让管理像逛主站一样轻盈、清晰、好用</div>
        </div>
        <div class="spark floaty"></div>
      </header>
      <main class="main">
        <RouterView v-slot="{ Component, route }">
          <Transition name="admin-content-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 24%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.1), transparent 20%),
    linear-gradient(180deg, #f8fbff 0%, var(--bg) 100%);
  color: var(--text);
}

.side {
  position: sticky;
  top: 18px;
  height: calc(100vh - 36px);
  padding: 14px;
}

.brand {
  margin-bottom: 12px;
  padding: 16px;
  border-color: rgba(59, 130, 246, 0.14);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(239, 246, 255, 0.94));
}

.brandBadge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  color: var(--primary);
  background: var(--primary-soft);
}

.brandTitle {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.brandSub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.content {
  min-width: 0;
}

.top {
  position: sticky;
  top: 18px;
  z-index: 5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  transition: box-shadow var(--transition-slow), transform var(--transition-slow);
}

.top:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(148, 163, 184, 0.16);
}

.title {
  font-size: 24px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.spark {
  width: 108px;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #34d399, #f59e0b, #f472b6);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.28);
}

.main {
  max-width: var(--content-width);
  margin: 18px auto 0;
}

.admin-content-fade-enter-active,
.admin-content-fade-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

.admin-content-fade-enter-from,
.admin-content-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.admin-content-fade-enter-to,
.admin-content-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 1080px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .side {
    position: relative;
    top: 0;
    height: auto;
  }

  .top {
    top: 0;
  }
}

@media (max-width: 768px) {
  .admin-shell {
    gap: 14px;
    padding: 12px;
  }

  .top {
    padding: 16px;
  }

  .spark {
    width: 72px;
  }
}
</style>
