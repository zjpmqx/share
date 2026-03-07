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
const navHidden = ref(false)
const lastScrollY = ref(0)
const isAdmin = computed(() => role.value === 'ADMIN')
const activeHint = computed(() => {
  const map = {
    home: '今日捡漏进行中',
    shares: '逛逛同学们的好物灵感',
    publish: '给闲置一个新机会',
    myItems: '看看你的宝贝近况',
    orders: '成交进度别错过',
    login: '登录后解锁完整体验',
    register: '注册后开始轻松淘卖',
    profile: '整理你的校园名片'
  }

  if (typeof route.name === 'string' && route.name.startsWith('admin')) {
    return '切换到管理视角'
  }

  return map[route.name] || '欢迎回来，慢慢挑'
})

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

function onScroll() {
  const currentScrollY = Math.max(window.scrollY || window.pageYOffset || 0, 0)
  const delta = currentScrollY - lastScrollY.value

  if (currentScrollY <= 24) {
    navHidden.value = false
    lastScrollY.value = currentScrollY
    return
  }

  if (menuOpen.value) {
    navHidden.value = false
    lastScrollY.value = currentScrollY
    return
  }

  if (delta > 8) {
    navHidden.value = true
  } else if (delta < -8) {
    navHidden.value = false
  }

  lastScrollY.value = currentScrollY
}

onMounted(() => {
  syncFromStorage()
  lastScrollY.value = Math.max(window.scrollY || window.pageYOffset || 0, 0)
  window.addEventListener('auth-changed', onAuthChanged)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', onAuthChanged)
  window.removeEventListener('scroll', onScroll)
})

watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
    navHidden.value = false
    lastScrollY.value = Math.max(window.scrollY || window.pageYOffset || 0, 0)
  }
)

watch(menuOpen, (open) => {
  if (open) {
    navHidden.value = false
  }
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
  <header class="nav" :class="{ navHidden }">
    <div class="glow glowLeft"></div>
    <div class="glow glowRight"></div>
    <div class="grain"></div>

    <div class="inner">
      <div class="brandWrap">
        <div class="brand" @click="router.push({ name: 'home' })">
          <div class="brandBadge pulse-dot">C</div>
          <div class="brandText">
            <div class="brandMainRow">
              <span class="brandMain">校园二手交易平台</span>
              <span class="brandChip">闲置流转中</span>
              <span class="brandMood floaty">今日好运签</span>
            </div>
            <span class="brandSub">轻松买卖 · 同校更放心 · 逛一圈总有惊喜</span>
          </div>
        </div>

        <div class="routeHint motion-enter-soft motion-delay-1">
          <span class="routeHintDot pulse-dot"></span>
          <span>{{ activeHint }}</span>
        </div>
      </div>

      <div class="actions">
        <div class="quickEntry">
          <router-link class="quickLink" to="/publish">
            <span class="quickLinkSpark pulse-dot"></span>
            <el-icon><Plus /></el-icon>
            <span>{{ loggedIn ? '去发布' : '先逛逛' }}</span>
          </router-link>
        </div>

        <el-button class="hamburger" text type="info" @click="toggle">
          <el-icon><Menu /></el-icon>
        </el-button>
      </div>

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
            <router-link class="link manageLink" :class="{ active: route.name?.startsWith('admin') }" to="#">
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
              <el-avatar :size="30">
                <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="" />
                <template v-else>
                  <el-icon><User /></el-icon>
                </template>
              </el-avatar>
              <div class="userMeta">
                <span class="username">{{ userInfo?.username || '用户' }}</span>
                <span class="userTag">今天也适合捡漏</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/my-items')">
                  <el-icon><Box /></el-icon>
                  我的发布
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">
                  <el-icon><Tickets /></el-icon>
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" divided @click="router.push('/admin/pending')">
                  <el-icon><Setting /></el-icon>
                  管理后台
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
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 100;
  overflow: hidden;
  backdrop-filter: blur(22px);
  background:
    linear-gradient(135deg, rgba(248, 251, 255, 0.88), rgba(242, 247, 255, 0.96) 42%, rgba(232, 241, 252, 0.92)),
    rgba(245, 249, 255, 0.88);
  color: #334155;
  box-shadow: 0 20px 46px rgba(90, 126, 177, 0.14);
  border-bottom: 1px solid rgba(203, 217, 236, 0.92);
  transition: transform 0.32s ease, opacity 0.32s ease, box-shadow 0.32s ease, background 0.32s ease;
  will-change: transform;
}

.nav::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.5), transparent 24%, transparent 76%, rgba(232, 244, 255, 0.42));
  opacity: 0.9;
}

.nav::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(125, 157, 201, 0.42), transparent);
}

.navHidden {
  transform: translateY(calc(-100% - 8px));
  opacity: 0;
  box-shadow: none;
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(42px);
  opacity: 0.56;
  pointer-events: none;
}

.glowLeft {
  width: 220px;
  height: 220px;
  left: -78px;
  top: -110px;
  background: rgba(96, 165, 250, 0.16);
}

.glowRight {
  width: 260px;
  height: 260px;
  right: -94px;
  top: -136px;
  background: rgba(14, 165, 233, 0.14);
}

.grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.45) 0.7px, transparent 0.7px);
  background-size: 12px 12px;
  opacity: 0.16;
}

.inner {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
}

.brandWrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.brand {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 10px 12px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.62), rgba(239, 246, 255, 0.42));
  border: 1px solid rgba(191, 219, 254, 0.58);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82), 0 10px 24px rgba(148, 163, 184, 0.08);
  transition: transform var(--transition), box-shadow var(--transition), background-color var(--transition);
}

.brand:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(96, 165, 250, 0.12);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(233, 244, 255, 0.56));
}

.brandBadge {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 900;
  color: #1d4ed8;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.98), rgba(255, 255, 255, 0.92));
  border: 1px solid rgba(191, 219, 254, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.84), 0 12px 26px rgba(96, 165, 250, 0.16);
}

.brandText {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.brandMainRow {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.brandMain {
  font-size: 17px;
  font-weight: 900;
  line-height: 1.1;
  letter-spacing: 0.25px;
  color: #0f172a;
}

.brandChip {
  flex: none;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.78);
  border: 1px solid rgba(147, 197, 253, 0.6);
}

.brandMood {
  flex: none;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  background: rgba(255, 255, 255, 0.52);
  border: 1px dashed rgba(148, 163, 184, 0.42);
}

.brandSub {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 0.2px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.routeHint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.52);
  border: 1px solid rgba(191, 219, 254, 0.56);
  color: #475569;
  font-size: 12px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.routeHintDot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #38bdf8;
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.12);
}

.actions {
  display: none;
  align-items: center;
  gap: 10px;
}

.quickEntry {
  display: none;
}

.quickLink {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #1d4ed8;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.84), rgba(255, 255, 255, 0.88));
  border: 1px solid rgba(191, 219, 254, 0.68);
  box-shadow: 0 12px 24px rgba(96, 165, 250, 0.1);
  transition: transform var(--transition-fast), background-color var(--transition-fast), box-shadow var(--transition-fast), color var(--transition-fast);
}

.quickLink:hover {
  color: #1e40af;
  background: linear-gradient(135deg, rgba(232, 244, 255, 0.94), rgba(255, 255, 255, 0.96));
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(96, 165, 250, 0.14);
}

.quickLinkSpark {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #38bdf8;
  box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.12);
}

.hamburger {
  display: none;
  color: #3b82f6;
  padding: 8px;
  font-size: 24px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(191, 219, 254, 0.58);
}

.links {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.link {
  color: #475569;
  text-decoration: none;
  padding: 9px 14px;
  border-radius: 999px;
  transition: transform var(--transition-fast), background-color var(--transition-fast), box-shadow var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.24);
  border: 1px solid transparent;
}

.link:hover {
  color: #0f172a;
  background: rgba(241, 247, 255, 0.82);
  border-color: rgba(191, 219, 254, 0.72);
  transform: translateY(-1px);
}

.link.active {
  color: #1d4ed8;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.96), rgba(255, 255, 255, 0.94));
  box-shadow: 0 12px 24px rgba(96, 165, 250, 0.12);
  border: 1px solid rgba(147, 197, 253, 0.76);
}

.loginBtn {
  margin-left: auto;
}

.registerBtn {
  color: #0f172a;
  background: linear-gradient(135deg, rgba(224, 242, 254, 0.92), rgba(255, 255, 255, 0.96));
  border-color: rgba(125, 211, 252, 0.52);
}

.registerBtn:hover {
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.98), rgba(255, 255, 255, 0.98));
  color: #0369a1;
}

.manageLink,
.userDropdown {
  background: rgba(255, 255, 255, 0.38);
  border-color: rgba(191, 219, 254, 0.62);
}

.userDropdown {
  padding: 5px 10px 5px 6px;
}

.userMeta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 1px;
}

.username {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #334155;
}

.userTag {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

@media (max-width: 980px) {
  .brandMood,
  .brandChip,
  .brandSub,
  .routeHint {
    display: none;
  }
}

@media (min-width: 861px) {
  .quickEntry {
    display: block;
  }
}

@media (max-width: 860px) {
  .inner {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .actions {
    display: flex;
  }

  .hamburger {
    display: inline-flex;
  }

  .quickEntry {
    display: block;
  }

  .links {
    display: none;
    position: absolute;
    right: 16px;
    top: calc(100% - 2px);
    width: min(280px, calc(100vw - 32px));
    padding: 12px;
    border-radius: 20px;
    flex-direction: column;
    align-items: stretch;
    background: linear-gradient(160deg, rgba(241, 247, 255, 0.98), rgba(255, 255, 255, 0.97));
    border: 1px solid rgba(191, 219, 254, 0.78);
    box-shadow: 0 18px 48px rgba(96, 165, 250, 0.14);
    z-index: 20;
  }

  .links.open {
    display: flex;
    animation: floatUp 0.22s cubic-bezier(0.22, 1, 0.36, 1);
  }

  .link,
  .loginBtn,
  .registerBtn,
  .manageLink,
  .userDropdown {
    margin-left: 0;
    width: 100%;
    justify-content: flex-start;
    border-radius: 14px;
    padding: 12px 14px;
  }

  .userTag,
  .username {
    max-width: none;
  }
}

@media (max-width: 560px) {
  .inner {
    padding: 12px;
  }

  .brand {
    padding: 9px 10px;
  }

  .brandBadge {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    font-size: 18px;
  }

  .brandMain {
    font-size: 15px;
  }

  .quickEntry {
    display: none;
  }
}
</style>
