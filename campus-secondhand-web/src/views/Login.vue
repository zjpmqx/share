<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../services/api'
import { setToken } from '../services/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  if (!username.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const resp = await login({ username: username.value, password: password.value })
    const token = resp?.data?.token
    if (!token) {
      throw new Error('登录失败：未返回 token')
    }
    setToken(token)
    ElMessage.success('登录成功！')
    router.push(route.query.redirect ? String(route.query.redirect) : '/')
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '登录失败'
    ElMessage.error(errMsg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="authPage">
    <div class="bgAura auraA"></div>
    <div class="bgAura auraB"></div>

    <div class="authWrap">
      <section class="intro">
        <div class="introBadge">WELCOME BACK</div>
        <h1>登录后继续淘好物</h1>
        <p>一键查看订单、管理发布、继续和同学聊你心动的宝贝。</p>
        <div class="introTips">
          <span>校园可信交易</span>
          <span>实时沟通更高效</span>
        </div>
      </section>

      <el-card class="authCard" shadow="hover">
        <template #header>
          <div class="cardHeader">
            <el-icon class="headerIcon"><User /></el-icon>
            <span class="title">账号登录</span>
          </div>
        </template>

        <el-form :model="{ username, password }" @submit.prevent="onSubmit">
          <el-form-item>
            <el-input
              v-model="username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              clearable
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="submitBtn"
              @click="onSubmit"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="tips">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.authPage {
  position: relative;
  overflow: hidden;
  margin-top: 10px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.78), rgba(248, 250, 252, 0.95));
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 28px 16px;
}

.bgAura {
  position: absolute;
  border-radius: 999px;
  filter: blur(42px);
  opacity: 0.35;
  pointer-events: none;
}

.auraA {
  width: 240px;
  height: 240px;
  left: -90px;
  top: -90px;
  background: rgba(59, 130, 246, 0.65);
}

.auraB {
  width: 260px;
  height: 260px;
  right: -110px;
  bottom: -120px;
  background: rgba(99, 102, 241, 0.5);
}

.authWrap {
  position: relative;
  z-index: 1;
  max-width: 980px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  align-items: stretch;
}

.intro {
  border-radius: 18px;
  padding: 24px;
  background: linear-gradient(140deg, #1e3a8a, #2563eb 62%, #0ea5e9);
  color: #e0f2fe;
  box-shadow: 0 18px 40px rgba(30, 64, 175, 0.28);
}

.introBadge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.4px;
  background: rgba(255, 255, 255, 0.16);
}

.intro h1 {
  margin: 14px 0 10px;
  font-size: 30px;
  line-height: 1.2;
  color: #f8fafc;
}

.intro p {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(224, 242, 254, 0.95);
}

.introTips {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.introTips span {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.16);
}

.authCard {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.cardHeader {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.headerIcon {
  font-size: 24px;
  color: var(--primary);
}

.title {
  font-size: 22px;
  font-weight: 800;
}

.submitBtn {
  width: 100%;
  border-radius: 12px;
}

.tips {
  text-align: center;
  font-size: 14px;
  color: var(--muted);
}

.tips a {
  color: var(--primary);
  font-weight: 600;
}

.tips a:hover {
  text-decoration: underline;
}

@media (max-width: 880px) {
  .authWrap {
    grid-template-columns: 1fr;
  }

  .intro {
    padding: 20px;
  }

  .intro h1 {
    font-size: 24px;
  }
}
</style>
