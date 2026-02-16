<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../services/api'

const router = useRouter()

const username = ref('')
const password = ref('')
const phone = ref('')

const errorMsg = ref('')
const loading = ref(false)

async function onSubmit() {
  errorMsg.value = ''
  loading.value = true
  try {
    await register({ username: username.value, password: password.value, phone: phone.value })
    router.push({ name: 'login' })
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <h1 class="title">注册</h1>

    <div class="card">
      <div class="field">
        <div class="label">用户名</div>
        <input v-model="username" class="input" placeholder="请输入用户名" />
      </div>

      <div class="field">
        <div class="label">密码</div>
        <input v-model="password" class="input" type="password" placeholder="请输入密码" />
      </div>

      <div class="field">
        <div class="label">手机号（可选）</div>
        <input v-model="phone" class="input" placeholder="请输入手机号" />
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

      <button class="primary" type="button" :disabled="loading" @click="onSubmit">
        {{ loading ? '提交中...' : '注册' }}
      </button>

      <div class="tips">
        已有账号？<RouterLink to="/login">去登录</RouterLink>
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
