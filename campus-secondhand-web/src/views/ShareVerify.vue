<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const pass = ref('')
const errorMsg = ref('')
const loading = ref(false)

const SHARE_GATE_KEY = 'share_gate_ok'
const REQUIRED = '嗯对'

const redirectTo = computed(() => {
  const r = typeof route.query?.redirect === 'string' ? route.query.redirect : ''
  if (r && r.startsWith('/')) return r
  return '/shares'
})

async function submit() {
  errorMsg.value = ''
  loading.value = true
  try {
    if ((pass.value || '').trim() !== REQUIRED) {
      errorMsg.value = '口令错误'
      return
    }
    localStorage.setItem(SHARE_GATE_KEY, '1')
    await router.replace(redirectTo.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="title">好物分享口令验证</div>
      <div class="sub muted">首次进入需要输入口令，验证通过后将记住。</div>

      <div class="field">
        <div class="label">口令</div>
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
.page {
  max-width: 520px;
  margin: 0 auto;
}

.card {
  margin-top: 14px;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  padding: 14px;
  box-shadow: var(--shadow-sm);
}

.title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 6px;
}

.sub {
  font-size: 12px;
  margin-bottom: 12px;
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
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.error {
  margin: 8px 0 12px;
  color: var(--danger);
  font-size: 13px;
}
</style>
