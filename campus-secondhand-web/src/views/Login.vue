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

function resolveSafeRedirect(target) {
  if (typeof target !== 'string' || !target.trim()) {
    return '/'
  }

  try {
    const base = typeof window !== 'undefined' ? window.location.origin : 'http://localhost'
    const url = new URL(target, base)
    if (url.origin !== base || !url.pathname.startsWith('/')) {
      return '/'
    }
    return `${url.pathname}${url.search}${url.hash}`
  } catch {
    return '/'
  }
}

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
    await router.replace(resolveSafeRedirect(route.query.redirect))
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '登录失败'
    ElMessage.error(errMsg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="authPage motion-enter-soft">
    <div class="bgAura auraA floaty"></div>
    <div class="bgAura auraB pulse-dot"></div>

    <div class="authWrap">
      <section class="intro surfacePanel glow-card motion-enter">
        <div class="introBadge">WELCOME BACK</div>
        <h1>登录后继续淘好物</h1>
        <p>一键查看订单、管理发布、继续和同学聊你心动的宝贝。</p>

        <div class="introTips">
          <span>校园可信交易</span>
          <span>实时沟通更高效</span>
          <span>收藏心动宝贝更方便</span>
        </div>

        <div class="introStats">
          <div class="statCard">
            <strong>订单管理</strong>
            <span>快速查看买卖进度</span>
          </div>
          <div class="statCard">
            <strong>发布维护</strong>
            <span>统一管理个人商品</span>
          </div>
        </div>
      </section>

      <el-card class="authCard" shadow="hover">
        <template #header>
          <div class="cardHeader">
            <div>
              <div class="cardEyebrow">账号入口</div>
              <div class="titleRow">
                <el-icon class="headerIcon"><User /></el-icon>
                <span class="title">账号登录</span>
              </div>
            </div>
            <div class="headerHint">欢迎回来</div>
          </div>
        </template>

        <div class="cardIntro">
          输入账号信息后即可继续浏览、下单与管理个人内容。
        </div>

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
          <span>还没有账号？</span>
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
  border-radius: 28px;
  padding: 28px 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 34%),
    radial-gradient(circle at bottom right, rgba(99, 102, 241, 0.16), transparent 32%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.96));
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.08);
}

.bgAura {
  position: absolute;
  border-radius: 999px;
  filter: blur(52px);
  opacity: 0.4;
  pointer-events: none;
}

.auraA {
  width: 260px;
  height: 260px;
  left: -100px;
  top: -90px;
  background: rgba(59, 130, 246, 0.52);
}

.auraB {
  width: 300px;
  height: 300px;
  right: -120px;
  bottom: -130px;
  background: rgba(99, 102, 241, 0.34);
}

.authWrap {
  position: relative;
  z-index: 1;
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
  gap: 22px;
  align-items: stretch;
}

.surfacePanel {
  position: relative;
  overflow: hidden;
}

.intro {
  border-radius: 24px;
  padding: 28px;
  background: linear-gradient(145deg, #1e3a8a, #2563eb 58%, #0ea5e9);
  color: #e0f2fe;
  box-shadow: 0 22px 50px rgba(37, 99, 235, 0.24);
}

.intro::after {
  content: '';
  position: absolute;
  inset: auto -40px -56px auto;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.introBadge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.intro h1 {
  position: relative;
  z-index: 1;
  margin: 18px 0 12px;
  font-size: 34px;
  line-height: 1.12;
  color: #f8fafc;
}

.intro p {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 460px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(224, 242, 254, 0.94);
}

.introTips {
  position: relative;
  z-index: 1;
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.introTips span {
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.introStats {
  position: relative;
  z-index: 1;
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.statCard {
  min-height: 96px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
}

.statCard strong {
  display: block;
  font-size: 16px;
  color: #f8fafc;
}

.statCard span {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(224, 242, 254, 0.92);
}

.authCard {
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.cardHeader {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.cardEyebrow {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.06em;
}

.titleRow {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.headerIcon {
  font-size: 22px;
  color: var(--primary);
}

.title {
  font-size: 24px;
  font-weight: 900;
  color: #0f172a;
}

.headerHint {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.cardIntro {
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(99, 102, 241, 0.06));
  border: 1px solid rgba(148, 163, 184, 0.18);
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.submitBtn {
  width: 100%;
  min-height: 46px;
  border-radius: 14px;
  font-weight: 700;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.submitBtn:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(37, 99, 235, 0.22);
}

.tips {
  margin-top: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  text-align: center;
  font-size: 14px;
  color: var(--muted);
}

.tips a {
  color: var(--primary);
  font-weight: 700;
}

.tips a:hover {
  text-decoration: underline;
}

@media (max-width: 940px) {
  .authWrap {
    grid-template-columns: 1fr;
  }

  .intro,
  .authCard {
    border-radius: 22px;
  }
}

@media (max-width: 680px) {
  .authPage {
    padding: 18px 12px;
    border-radius: 22px;
  }

  .intro {
    padding: 22px 18px;
  }

  .intro h1 {
    font-size: 28px;
  }

  .introStats {
    grid-template-columns: 1fr;
  }

  .cardHeader {
    flex-direction: column;
  }
}
</style>
