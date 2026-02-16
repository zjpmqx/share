<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { me } from '../services/api'
import { clearToken, getRole, getToken, setRole } from '../services/auth'

const router = useRouter()
const route = useRoute()

const token = ref(getToken())
const loggedIn = computed(() => !!token.value)
const menuOpen = ref(false)
const role = ref('')
const isAdmin = computed(() => role.value === 'ADMIN')

async function refreshMeIfNeeded() {
  if (!token.value) {
    role.value = ''
    return
  }
  if (role.value) return

  try {
    const resp = await me()
    role.value = resp?.data?.role || ''
    if (role.value) setRole(role.value)
  } catch {
  }
}

function syncFromStorage() {
  token.value = getToken()
  role.value = getRole() || ''
  if (!token.value) {
    menuOpen.value = false
  }
  refreshMeIfNeeded()
}

function onAuthChanged() {
  syncFromStorage()
}

onMounted(() => {
  syncFromStorage()
  window.addEventListener('auth-changed', onAuthChanged)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', onAuthChanged)
})

function logout() {
  clearToken()
  token.value = ''
  role.value = ''
  router.push({ name: 'home' })
}

function toggle() {
  menuOpen.value = !menuOpen.value
}
</script>

<template>
  <header class="nav">
    <div class="inner">
      <div class="brand" @click="router.push({ name: 'home' })">
        校园二手交易平台
      </div>

      <button class="hamburger" type="button" @click="toggle">菜单</button>

      <nav class="links" :class="{ open: menuOpen }">
        <RouterLink class="link" :class="{ active: route.name === 'home' }" to="/">首页</RouterLink>
        <RouterLink class="link" :class="{ active: route.name === 'shares' }" to="/shares">好物分享</RouterLink>
        <RouterLink class="link" :class="{ active: route.name === 'publish' }" to="/publish">发布</RouterLink>
        <RouterLink class="link" :class="{ active: route.name === 'myItems' }" to="/my-items">我的发布</RouterLink>
        <RouterLink class="link" :class="{ active: route.name === 'orders' }" to="/orders">订单</RouterLink>
        <RouterLink v-if="loggedIn" class="link" :class="{ active: route.name === 'profile' }" to="/profile">个人资料</RouterLink>
        <RouterLink v-if="isAdmin" class="link" :class="{ active: route.name === 'adminPending' }" to="/admin/pending">管理后台</RouterLink>

        <template v-if="!loggedIn">
          <RouterLink class="link" :class="{ active: route.name === 'login' }" to="/login">登录</RouterLink>
          <RouterLink class="link" :class="{ active: route.name === 'register' }" to="/register">注册</RouterLink>
        </template>
        <button v-else class="link btn" type="button" @click="logout">退出</button>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #111827;
  color: #fff;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.18);
}

.inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.3px;
}

.hamburger {
  display: none;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 6px 10px;
  border-radius: 8px;
}

.links {
  display: flex;
  align-items: center;
  gap: 10px;
}

.link {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.link:hover {
  background: rgba(255, 255, 255, 0.08);
}

.link.active {
  background: rgba(255, 255, 255, 0.14);
}

.btn {
  background: rgba(255, 255, 255, 0.14);
  border: 0;
  cursor: pointer;
}

@media (max-width: 760px) {
  .hamburger {
    display: inline-block;
  }

  .links {
    display: none;
    position: absolute;
    right: 16px;
    top: 56px;
    flex-direction: column;
    align-items: stretch;
    background: #111827;
    padding: 10px;
    border-radius: 10px;
    width: 180px;
  }

  .links.open {
    display: flex;
  }
}
</style>
