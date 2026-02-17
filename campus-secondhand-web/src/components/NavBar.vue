<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
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
    <div class="inner">
      <div class="brand" @click="router.push({ name: 'home' })">
        <el-icon class="brandIcon"><ShoppingBag /></el-icon>
        <span>校园二手交易平台</span>
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
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 50%, #3b82f6 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(30, 58, 138, 0.3);
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
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  transition: opacity 0.2s ease;
}

.brand:hover {
  opacity: 0.9;
}

.brandIcon {
  font-size: 24px;
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
  color: rgba(255, 255, 255, 0.95);
  text-decoration: none;
  padding: 8px 14px;
  border-radius: 10px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  cursor: pointer;
}

.link:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-1px);
}

.link.active {
  background: rgba(255, 255, 255, 0.22);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loginBtn {
  margin-left: 8px;
}

.registerBtn {
  background: rgba(255, 255, 255, 0.2);
}

.registerBtn:hover {
  background: rgba(255, 255, 255, 0.28);
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

@media (max-width: 860px) {
  .hamburger {
    display: inline-flex;
  }

  .links {
    display: none;
    position: absolute;
    right: 16px;
    top: 60px;
    flex-direction: column;
    align-items: stretch;
    background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%);
    padding: 12px;
    border-radius: 14px;
    width: 220px;
    box-shadow: 0 10px 40px rgba(30, 58, 138, 0.4);
  }

  .links.open {
    display: flex;
  }

  .link {
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
</style>
