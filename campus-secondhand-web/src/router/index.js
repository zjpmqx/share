import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ItemDetail from '../views/ItemDetail.vue'
import PublishItem from '../views/PublishItem.vue'
import MyOrders from '../views/MyOrders.vue'
import AdminPending from '../views/AdminPending.vue'
import MyItems from '../views/MyItems.vue'
import ShareList from '../views/ShareList.vue'
import SharePublish from '../views/SharePublish.vue'
import ShareDetail from '../views/ShareDetail.vue'
import ShareVerify from '../views/ShareVerify.vue'
import AdminItems from '../views/AdminItems.vue'
import AdminShares from '../views/AdminShares.vue'
import AdminUsers from '../views/AdminUsers.vue'
import Profile from '../views/Profile.vue'
import { clearToken, getRole, getToken, getShareGateToken, setRole } from '../services/auth'
import { me } from '../services/api'

const routes = [
  {
    path: '/',
    component: UserLayout,
    children: [
      { path: '', name: 'home', component: Home },
      { path: 'login', name: 'login', component: Login },
      { path: 'register', name: 'register', component: Register },
      { path: 'profile', name: 'profile', component: Profile, meta: { requiresAuth: true } },
      { path: 'items/:id', name: 'itemDetail', component: ItemDetail },
      { path: 'publish', name: 'publish', component: PublishItem, meta: { requiresAuth: true } },
      { path: 'my-items', name: 'myItems', component: MyItems, meta: { requiresAuth: true } },
      { path: 'orders', name: 'orders', component: MyOrders, meta: { requiresAuth: true } },
      { path: 'shares', name: 'shares', component: ShareList },
      { path: 'shares/verify', name: 'shareVerify', component: ShareVerify },
      { path: 'shares/publish', name: 'sharePublish', component: SharePublish, meta: { requiresAuth: true } },
      { path: 'shares/:id', name: 'shareDetail', component: ShareDetail },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: 'pending', name: 'adminPending', component: AdminPending },
      { path: 'items', name: 'adminItems', component: AdminItems, alias: ['items/list'] },
      { path: 'shares', name: 'adminShares', component: AdminShares },
      { path: 'users', name: 'adminUsers', component: AdminUsers },
    ],
  },
]

const router = createRouter({
  history: createWebHistory('/'),
  routes,
})

router.beforeEach(async (to) => {
  const token = getToken()

  if (to.path.startsWith('/shares') && to.name !== 'shareVerify') {
    try {
      const shareGateToken = getShareGateToken()
      if (!shareGateToken) {
        return { name: 'shareVerify', query: { redirect: to.fullPath } }
      }
    } catch {
      return { name: 'shareVerify', query: { redirect: to.fullPath } }
    }
  }

  // 方案 B：除登录/注册外，其他页面都必须登录
  if (!token && to.name !== 'login' && to.name !== 'register') {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (token && (to.name === 'login' || to.name === 'register')) {
    return { name: 'home' }
  }

  if (to.meta?.requiresAdmin) {
    let role = getRole()
    try {
      // 不要完全信任本地缓存的 role；普通窗口可能残留旧值(USER)导致无法进入后台
      if (!role || role !== 'ADMIN') {
        const resp = await me()
        role = resp?.data?.role || ''
        if (role) setRole(role)
      }
    } catch {
      clearToken()
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (role !== 'ADMIN') return { name: 'home' }
  }

  return true
})

export default router
