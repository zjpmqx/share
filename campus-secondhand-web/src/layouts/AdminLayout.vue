<script setup>
import AdminSidebar from '../components/AdminSidebar.vue'
</script>

<template>
  <div class="wrap">
    <aside class="side">
      <div class="brand">管理后台</div>
      <AdminSidebar />
    </aside>

    <div class="content">
      <header class="top">
        <div class="title">商品审核</div>
      </header>
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  /* admin theme overrides */
  --bg: #070b15;
  --surface: #0b1220;
  --surface-2: #0f1a33;
  --text: rgba(255, 255, 255, 0.92);
  --muted: rgba(255, 255, 255, 0.62);
  --border: rgba(255, 255, 255, 0.10);
  --border-2: rgba(255, 255, 255, 0.14);
  --shadow: 0 18px 40px rgba(0, 0, 0, 0.45);
  --shadow-sm: 0 10px 24px rgba(0, 0, 0, 0.35);
  --primary: #22c55e;
  --primary-hover: #16a34a;
  --danger: #fb7185;
  --radius: 16px;
  --radius-sm: 12px;

  background: var(--bg);
  color: var(--text);
  display: grid;
  grid-template-columns: 260px 1fr;
}

.side {
  background: linear-gradient(180deg, #0b1220, #070b15);
  color: var(--text);
  padding: 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid var(--border);
}

.brand {
  font-weight: 900;
  letter-spacing: 0.3px;
  padding: 10px 10px 14px;
}

.content {
  min-width: 0;
}

.top {
  position: sticky;
  top: 0;
  z-index: 5;
  background: rgba(7, 11, 21, 0.72);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border);
  padding: 12px 16px;
}

.title {
  font-weight: 800;
}

.main {
  padding: 18px 16px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

@media (max-width: 900px) {
  .wrap {
    grid-template-columns: 1fr;
  }

  .side {
    position: relative;
    height: auto;
  }
}
</style>
