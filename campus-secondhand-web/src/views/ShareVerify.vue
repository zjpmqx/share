<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { verifyShareGate } from '../services/api'
import { setShareGateToken } from '../services/auth'

const router = useRouter()
const route = useRoute()

const pass = ref('')
const errorMsg = ref('')
const loading = ref(false)

const redirectTo = computed(() => {
  const r = typeof route.query?.redirect === 'string' ? route.query.redirect : ''
  if (r && r.startsWith('/')) return r
  return '/shares'
})

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    const val = (pass.value || '').trim()
    if (!val) {
      errorMsg.value = '请输入口令'
      return
    }

    const resp = await verifyShareGate({ passphrase: val })
    const token = resp?.data?.token
    if (!token) {
      errorMsg.value = '验证失败，请稍后重试'
      return
    }

    setShareGateToken(token)
    await router.replace(redirectTo.value)
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '口令验证失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page verifyPage">
    <div class="hero">
      <div class="title">好物分享口令验证</div>
      <div class="sub muted">首次进入需要输入口令，验证通过后将自动记住登录状态。</div>
    </div>

    <div class="card">
      <div class="field">
        <div class="label">访问口令</div>
        <input v-model="pass" class="input" placeholder="请输入口令" @keydown.enter="submit" />
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

      <button class="primary" type="button" :disabled="loading" @click="submit">
        {{ loading ? '验证中...' : '验证进入' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.verifyPage {
  max-width: 560px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  margin-top: 8px;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.14), rgba(59, 130, 246, 0.14));
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.title {
  font-size: 20px;
  font-weight: 900;
  margin-bottom: 6px;
}

.sub {
  font-size: 12px;
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
  border-radius: 10px;
  padding: 10px 12px;
  background: var(--surface);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.primary {
  width: 100%;
  background: linear-gradient(135deg, var(--primary), #6366f1);
  color: #fff;
  border: 0;
  padding: 11px 14px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 700;
  transition: transform 0.15s ease, filter 0.15s ease;
}

.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.primary:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.error {
  margin: 8px 0 12px;
  color: var(--danger);
  font-size: 13px;
}
</style>
