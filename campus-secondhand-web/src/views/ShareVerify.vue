<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { verifyShareGate } from '../services/api'

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
    if (!resp?.data?.verified) {
      errorMsg.value = '验证失败，请稍后重试'
      return
    }

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
      <div class="heroBadge">SHARE GATE</div>
      <div class="title">好物分享口令验证</div>
      <div class="sub">首次进入需要输入口令，验证通过后将通过安全会话自动记住当前浏览器的访问状态。</div>
      <div class="heroTips">
        <span>仅首次验证</span>
        <span>通过后自动跳转</span>
        <span>不暴露门禁凭证</span>
      </div>
    </div>

    <div class="card">
      <div class="cardHead">
        <div>
          <div class="cardTitle">输入访问口令</div>
          <div class="cardDesc">验证成功后将自动返回分享页面。</div>
        </div>
        <div class="cardHint">安全访问</div>
      </div>

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
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 28%),
    linear-gradient(135deg, rgba(16, 185, 129, 0.18), rgba(59, 130, 246, 0.2));
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
}

.heroBadge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #0f172a;
}

.title {
  margin-top: 16px;
  font-size: 30px;
  line-height: 1.16;
  font-weight: 900;
  color: #0f172a;
}

.sub {
  margin-top: 10px;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
}

.heroTips {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.heroTips span {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  color: #334155;
}

.card {
  padding: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 22px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.cardHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.cardTitle {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.cardDesc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.cardHint {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.field {
  margin-bottom: 14px;
}

.label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.input {
  width: 100%;
  min-height: 48px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  padding: 0 14px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.primary {
  width: 100%;
  min-height: 48px;
  background: linear-gradient(135deg, var(--primary), #6366f1);
  color: #fff;
  border: 0;
  padding: 12px 16px;
  border-radius: 14px;
  cursor: pointer;
}

.error {
  margin: 8px 0 12px;
  color: var(--danger);
  font-size: 13px;
}
</style>
