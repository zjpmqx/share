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
  <div class="wrap">
    <el-card class="loginCard" shadow="hover">
      <template #header>
        <div class="cardHeader">
          <el-icon class="headerIcon"><User /></el-icon>
          <span class="title">欢迎回来</span>
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
</template>

<style scoped>
.wrap {
  max-width: 420px;
  margin: 60px auto 0;
  padding: 0 16px;
}

.loginCard {
  border-radius: 16px;
}

.cardHeader {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.headerIcon {
  font-size: 28px;
  color: var(--primary);
}

.title {
  font-size: 22px;
  font-weight: 700;
}

.submitBtn {
  width: 100%;
}

.tips {
  text-align: center;
  font-size: 14px;
  color: var(--muted);
}

.tips a {
  color: var(--primary);
  font-weight: 500;
}

.tips a:hover {
  text-decoration: underline;
}
</style>
