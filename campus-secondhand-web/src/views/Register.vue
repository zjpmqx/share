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
      <section class="intro">
        <div class="introBadge">JOIN US</div>
        <h1>注册开启你的校园交易生活</h1>
        <p>发布闲置、快速成交、结识同校买家卖家，让每一件物品都更有价值。</p>
        <div class="introTips">
          <span>发布流程简单</span>
          <span>资料可随时维护</span>
          <span>支持分享种草</span>
        </div>
      </section>

      <el-card class="authCard" shadow="hover">
        <template #header>
          <div class="cardHeader">
            <el-icon class="headerIcon"><EditPen /></el-icon>
            <span class="title">创建账号</span>
          </div>
        </template>

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
          已有账号？
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
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.75), rgba(248, 250, 252, 0.95));
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
  width: 250px;
  height: 250px;
  left: -100px;
  top: -100px;
  background: rgba(34, 197, 94, 0.55);
}

.auraB {
  width: 280px;
  height: 280px;
  right: -120px;
  bottom: -140px;
  background: rgba(59, 130, 246, 0.45);
}

.authWrap {
  position: relative;
  z-index: 1;
  max-width: 980px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 18px;
  align-items: stretch;
}

.intro {
  border-radius: 18px;
  padding: 24px;
  background: linear-gradient(145deg, #0f766e, #10b981 55%, #14b8a6);
  color: #ecfdf5;
  box-shadow: 0 18px 40px rgba(13, 148, 136, 0.24);
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
  color: rgba(236, 253, 245, 0.95);
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
