<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../services/api'
import { setToken } from '../services/auth'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)

async function onSubmit() {
  errorMsg.value = ''
  loading.value = true
  try {
    const resp = await login({ username: username.value, password: password.value })
    const token = resp?.data?.token
    if (!token) {
      throw new Error('登录失败：未返回 token')
    }
    setToken(token)
    router.push(route.query.redirect ? String(route.query.redirect) : '/')
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <h1 class="title">登录</h1>

    <div class="card">
      <div class="field">
        <div class="label">用户名</div>
        <input v-model="username" class="input" placeholder="请输入用户名" />
      </div>

      <div class="field">
        <div class="label">密码</div>
        <input v-model="password" class="input" type="password" placeholder="请输入密码" />
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

      <button class="primary" type="button" :disabled="loading" @click="onSubmit">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <div class="tips">
        没有账号？<RouterLink to="/register">去注册</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 420px;
  margin: 0 auto;
}

.title {
  margin: 10px 0 12px;
  font-size: 22px;
  font-weight: 800;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 14px;
  box-shadow: var(--shadow);
}

.field {
  margin-bottom: 12px;
}

.label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--surface);
}

.primary {
  width: 100%;
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.primary:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

.error {
  margin: 8px 0;
  color: var(--danger);
  font-size: 13px;
}

.tips {
  margin-top: 10px;
  font-size: 13px;
  color: var(--muted);
}
</style>
