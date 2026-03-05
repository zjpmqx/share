<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <nav class="nav">
    <RouterLink class="link" :class="{ active: route.name === 'adminPending' }" :to="{ name: 'adminPending' }">
      <span class="dot"></span>
      <span>待审核商品</span>
    </RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminItems' }" :to="{ name: 'adminItems' }">
      <span class="dot"></span>
      <span>商品管理</span>
    </RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminShares' }" :to="{ name: 'adminShares' }">
      <span class="dot"></span>
      <span>好物分享管理</span>
    </RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminUsers' }" :to="{ name: 'adminUsers' }">
      <span class="dot"></span>
      <span>用户管理</span>
    </RouterLink>
    <RouterLink class="link back" to="/">
      <span class="dot"></span>
      <span>返回用户端</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 12px;
  color: #334155;
  text-decoration: none;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  transition: all 0.2s ease;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
}

.link:hover {
  background: #f0f7ff;
  border-color: #bfdbfe;
  transform: translateX(2px);
}

.link.active {
  background: linear-gradient(90deg, #e0f2fe, #dcfce7);
  border-color: #93c5fd;
  color: #0f172a;
  font-weight: 700;
}

.link.active .dot {
  background: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.22);
}

.back {
  margin-top: 6px;
  background: #f8fafc;
}
</style>
