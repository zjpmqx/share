<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <nav class="nav">
    <RouterLink class="link" :class="{ active: route.name === 'adminPending' }" :to="{ name: 'adminPending' }">待审核商品</RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminItems' }" :to="{ name: 'adminItems' }">商品管理</RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminShares' }" :to="{ name: 'adminShares' }">好物分享管理</RouterLink>
    <RouterLink class="link" :class="{ active: route.name === 'adminUsers' }" :to="{ name: 'adminUsers' }">用户管理</RouterLink>
    <RouterLink class="link" to="/">返回用户端</RouterLink>
  </nav>
</template>

<style scoped>
.nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link {
  display: block;
  padding: 10px 12px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.86);
  text-decoration: none;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.link:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.14);
}

.link.active {
  background: rgba(37, 99, 235, 0.28);
  border-color: rgba(37, 99, 235, 0.35);
  color: #fff;
}
</style>
