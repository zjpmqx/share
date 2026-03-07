<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const phone = ref('')

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
  if (password.value.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register({ username: username.value, password: password.value, phone: phone.value })
    ElMessage.success('注册成功！请登录')
    router.push({ name: 'login' })
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '注册失败'
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
      <section class="intro surfacePanel">
        <div class="introBadge">JOIN US</div>
        <h1>注册开启你的校园交易生活</h1>
        <p>发布闲置、快速成交、结识同校买家卖家，让每一件物品都更有价值。</p>

        <div class="introTips motion-enter-soft motion-delay-1">
          <span>发布流程简单</span>
          <span>资料可随时维护</span>
          <span>支持分享种草</span>
        </div>

        <div class="introStats">
          <div class="statCard">
            <strong>一键发布</strong>
            <span>快速上架你的闲置物品</span>
          </div>
          <div class="statCard">
            <strong>同校互动</strong>
            <span>让买卖与分享都更高效</span>
          </div>
        </div>
      </section>

      <el-card class="authCard glow-card motion-enter motion-delay-2" shadow="hover">
        <template #header>
          <div class="cardHeader">
            <div>
              <div class="cardEyebrow">创建新账号</div>
              <div class="titleRow">
                <el-icon class="headerIcon"><EditPen /></el-icon>
                <span class="title">创建账号</span>
              </div>
            </div>
            <div class="headerHint">新用户注册</div>
          </div>
        </template>

        <div class="cardIntro">
          填写基础信息后即可开始发布闲置、管理订单并参与好物分享。
        </div>

        <el-form :model="{ username, password, confirmPassword, phone }" @submit.prevent="onSubmit">
          <el-form-item>
            <el-input
              v-model="username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入密码（至少6位）"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="phone"
              placeholder="手机号（可选）"
              :prefix-icon="Phone"
              size="large"
              clearable
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
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="tips">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
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
    radial-gradient(circle at top left, rgba(16, 185, 129, 0.2), transparent 34%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.14), transparent 30%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.96));
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.08);
}

.bgAura {
  position: absolute;
  border-radius: 999px;
  filter: blur(52px);
  opacity: 0.38;
  pointer-events: none;
}

.auraA {
  width: 260px;
  height: 260px;
  left: -100px;
  top: -100px;
  background: rgba(16, 185, 129, 0.46);
}

.auraB {
  width: 300px;
  height: 300px;
  right: -120px;
  bottom: -140px;
  background: rgba(59, 130, 246, 0.28);
}

.authWrap {
  position: relative;
  z-index: 1;
  max-width: 1040px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(380px, 0.94fr);
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
  background: linear-gradient(145deg, #0f766e, #10b981 58%, #14b8a6);
  color: #ecfdf5;
  box-shadow: 0 22px 50px rgba(16, 185, 129, 0.2);
}

.intro::after {
  content: '';
  position: absolute;
  inset: auto -46px -60px auto;
  width: 230px;
  height: 230px;
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
  line-height: 1.14;
  color: #f8fafc;
}

.intro p {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 470px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(236, 253, 245, 0.94);
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
  transition: transform var(--transition-fast), background-color var(--transition-fast);
}

.introTips span:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.2);
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
  color: rgba(236, 253, 245, 0.92);
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
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
  font-size: 12px;
  font-weight: 700;
}

.cardIntro {
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(59, 130, 246, 0.05));
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
  box-shadow: 0 14px 26px rgba(16, 185, 129, 0.22);
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
