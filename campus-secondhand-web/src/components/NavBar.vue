<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { me } from '../services/api'
import { clearToken, getRole, getToken, setRole } from '../services/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const token = ref(getToken())
const loggedIn = computed(() => !!token.value)
const menuOpen = ref(false)
const role = ref('')
const userInfo = ref(null)
const isAdmin = computed(() => role.value === 'ADMIN')

async function refreshMeIfNeeded() {
  if (!token.value) {
    role.value = ''
    userInfo.value = null
    return
  }
  if (role.value && userInfo.value) return

  try {
    const resp = await me()
    userInfo.value = resp?.data
    role.value = userInfo.value?.role || ''
    if (role.value) setRole(role.value)
  } catch {
  }
}

function syncFromStorage() {
  token.value = getToken()
  role.value = getRole() || ''
  if (!token.value) {
    menuOpen.value = false
    userInfo.value = null
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

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

async function logout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    clearToken()
    token.value = ''
    role.value = ''
    userInfo.value = null
    ElMessage.success('已退出登录')
    router.push({ name: 'home' })
  } catch {
  }
}

function toggle() {
  menuOpen.value = !menuOpen.value
}
</script>

<template>
  <header class="nav">
    <div class="glow glowLeft"></div>
    <div class="glow glowRight"></div>

    <div class="inner">
      <div class="brand" @click="router.push({ name: 'home' })">
        <el-icon class="brandIcon"><ShoppingBag /></el-icon>
        <div class="brandText">
          <span class="brandMain">校园二手交易平台</span>
          <span class="brandSub">轻松买卖 闪电成交</span>
        </div>
      </div>

      <el-button class="hamburger" text type="info" @click="toggle">
        <el-icon><Menu /></el-icon>
      </el-button>

      <nav class="links" :class="{ open: menuOpen }">
        <router-link class="link" :class="{ active: route.name === 'home' }" to="/">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </router-link>
        <router-link class="link" :class="{ active: route.name === 'shares' }" to="/shares">
          <el-icon><ChatLineSquare /></el-icon>
          <span>好物分享</span>
        </router-link>
        <router-link v-if="loggedIn" class="link" :class="{ active: route.name === 'publish' }" to="/publish">
          <el-icon><Plus /></el-icon>
          <span>发布</span>
        </router-link>
        <router-link v-if="loggedIn" class="link" :class="{ active: route.name === 'myItems' }" to="/my-items">
          <el-icon><Box /></el-icon>
          <span>我的发布</span>
        </router-link>
        <router-link v-if="loggedIn" class="link" :class="{ active: route.name === 'orders' }" to="/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单</span>
        </router-link>

        <template v-if="!loggedIn">
          <router-link class="link loginBtn" :class="{ active: route.name === 'login' }" to="/login">
            <el-icon><User /></el-icon>
            <span>登录</span>
          </router-link>
          <router-link class="link registerBtn" :class="{ active: route.name === 'register' }" to="/register">
            <span>注册</span>
          </router-link>
        </template>

        <template v-else>
          <el-dropdown v-if="isAdmin" trigger="click">
            <router-link class="link" :class="{ active: route.name?.startsWith('admin') }" to="#">
              <el-icon><Setting /></el-icon>
              <span>管理后台</span>
              <el-icon><ArrowDown /></el-icon>
            </router-link>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/admin/pending')">待审核商品</el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/items')">商品管理</el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/shares')">分享管理</el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/users')">用户管理</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click">
            <div class="link userDropdown">
              <el-avatar :size="28">
                <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="" />
                <template v-else>
                  <el-icon><User /></el-icon>
                </template>
              </el-avatar>
              <span class="username">{{ userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 100;
  overflow: hidden;
  background: linear-gradient(120deg, #172554 0%, #1d4ed8 45%, #0ea5e9 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(23, 37, 84, 0.28);
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(30px);
  opacity: 0.4;
  pointer-events: none;
}

.glowLeft {
  width: 180px;
  height: 180px;
  left: -70px;
  top: -100px;
  background: rgba(56, 189, 248, 0.7);
}

.glowRight {
  width: 220px;
  height: 220px;
  right: -80px;
  top: -120px;
  background: rgba(99, 102, 241, 0.72);
}

.inner {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.brandIcon {
  font-size: 24px;
}

.brandText {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brandMain {
  font-size: 16px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: 0.25px;
}

.brandSub {
  margin-top: 2px;
  font-size: 11px;
  letter-spacing: 0.3px;
  color: rgba(226, 232, 240, 0.9);
}

.hamburger {
  display: none;
  color: #fff;
  padding: 8px;
  font-size: 24px;
}

.links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.link {
  color: rgba(255, 255, 255, 0.96);
  text-decoration: none;
  padding: 8px 14px;
  border-radius: 999px;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  cursor: pointer;
}

.link:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-1px);
}

.link.active {
  background: rgba(255, 255, 255, 0.24);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.15);
}

.loginBtn {
  margin-left: 8px;
}

.registerBtn {
  color: #1d4ed8;
  background: #f8fafc;
}

.registerBtn:hover {
  background: #ffffff;
  color: #1e3a8a;
}

.userDropdown {
  padding: 4px 10px;
}

.username {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .brandSub {
    display: none;
  }
}

@media (max-width: 860px) {
  .hamburger {
    display: inline-flex;
  }

  .links {
    display: none;
    position: absolute;
    right: 16px;
    top: 58px;
    flex-direction: column;
    align-items: stretch;
    background: linear-gradient(140deg, rgba(30, 64, 175, 0.96), rgba(14, 116, 144, 0.96));
    padding: 12px;
    border-radius: 16px;
    width: 236px;
    box-shadow: 0 12px 40px rgba(15, 23, 42, 0.35);
    border: 1px solid rgba(255, 255, 255, 0.18);
  }

  .links.open {
    display: flex;
  }

  .link {
    border-radius: 12px;
    padding: 12px 14px;
    justify-content: flex-start;
  }

  .loginBtn {
    margin-left: 0;
  }

  .userDropdown {
    padding: 12px 14px;
  }

  .username {
    max-width: none;
  }
}

@media (max-width: 560px) {
  .brandMain {
    font-size: 14px;
  }
}
</style>
