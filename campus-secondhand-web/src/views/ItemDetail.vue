<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { cancelOrder, confirmOrder, createOrder, getItem, listMessages, me, payOrder, postMessage, shipOrder } from '../services/api'
import { getToken } from '../services/auth'

const route = useRoute()

const item = ref(null)
const loading = ref(false)
const msgLoading = ref(false)
const messages = ref([])
const content = ref('')

const msgError = ref('')
const orderError = ref('')
const posting = ref(false)
const ordering = ref(false)

const my = ref(null)
const myLoading = ref(false)

async function loadMe() {
  if (!getToken()) return
  myLoading.value = true
  try {
    const resp = await me()
    my.value = resp.data
  } catch {
  } finally {
    myLoading.value = false
  }
}

async function loadItem() {
  loading.value = true
  try {
    const resp = await getItem(route.params.id)
    item.value = resp.data
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  msgLoading.value = true
  try {
    const resp = await listMessages(route.params.id)
    messages.value = resp.data || []
  } finally {
    msgLoading.value = false
  }
}

async function sendMessage() {
  if (!content.value.trim()) return
  msgError.value = ''
  posting.value = true
  try {
    await postMessage(route.params.id, { content: content.value })
    content.value = ''
    await loadMessages()
  } catch (e) {
    msgError.value = e?.response?.data?.message || e?.message || '发送失败'
  } finally {
    posting.value = false
  }
}

async function orderCreate() {
  orderError.value = ''
  ordering.value = true
  try {
    await createOrder({ itemId: Number(route.params.id) })
    await loadItem()
  } catch (e) {
    orderError.value = e?.response?.data?.message || e?.message || '下单失败'
  } finally {
    ordering.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadMe(), loadItem(), loadMessages()])
})
</script>

<template>
  <div class="page">
    <div v-if="loading">加载中...</div>

    <div v-else-if="item" class="layout">
      <div class="left">
        <div class="card media">
          <div class="mediaBox">
            <img v-if="item.coverImageUrl" :src="item.coverImageUrl" alt="" loading="lazy" />
          </div>
        </div>
      </div>

      <div class="right">
        <div class="card">
          <div class="head">
            <h1 class="title">{{ item.title }}</h1>
            <div class="price">￥{{ item.price }}</div>
          </div>

          <div class="meta">
            <span class="chip">{{ item.category }}</span>
            <span class="chip">成色 {{ item.conditionLevel }}</span>
            <span class="chip">{{ item.status }}</span>
          </div>

          <div class="desc">{{ item.description || '（无描述）' }}</div>

          <div class="actions">
            <button class="primary" type="button" :disabled="!getToken() || ordering || item.status !== 'ON_SALE'" @click="orderCreate">
              下单
            </button>
            <div v-if="!getToken()" class="hint">提示：请先登录后下单。</div>
            <div v-else-if="item.status !== 'ON_SALE'" class="hint">提示：商品正在审核或暂不可购买。</div>
            <div v-else class="hint">提示：下单后商品会被锁定，卖家发货后买家确认收货完成交易。</div>
            <div v-if="orderError" class="error">{{ orderError }}</div>
          </div>
        </div>

        <div class="card" style="margin-top: 12px;">
          <h2 class="sub">留言</h2>

          <div v-if="msgLoading">加载留言中...</div>
          <div v-else class="msgs">
            <div v-for="m in messages" :key="m.id" class="msg">
              <div class="msgHead">
                <div class="avatar">
                  <img v-if="m.avatarUrl" :src="m.avatarUrl" alt="" />
                  <div v-else class="avatarFallback"></div>
                </div>
                <div class="who">{{ m.userName || ('用户' + (m.fromUserId || '')) }}</div>
              </div>
              <div class="msgContent">{{ m.content }}</div>
              <div class="msgTime">{{ m.createdAt || '' }}</div>
            </div>
            <div v-if="messages.length === 0" class="empty">暂无留言</div>
          </div>

          <div class="composer">
            <input v-model="content" class="input" placeholder="说点什么..." />
            <button class="primary" type="button" :disabled="!getToken() || posting" @click="sendMessage">
              {{ posting ? '发送中...' : '发送' }}
            </button>
          </div>
          <div v-if="!getToken()" class="hint">登录后才能留言</div>
          <div v-if="msgError" class="error" style="margin-top: 8px;">{{ msgError }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 14px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.media {
  overflow: hidden;
}

.mediaBox {
  width: 100%;
  height: 420px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(99, 102, 241, 0.10));
}

@media (max-width: 900px) {
  .mediaBox {
    height: 320px;
  }
}

@media (max-width: 520px) {
  .mediaBox {
    height: 260px;
  }
}

.mediaBox img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  padding: 14px;
  box-shadow: var(--shadow-sm);
}

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-size: 20px;
  margin: 0;
}

.price {
  font-size: 22px;
  font-weight: 800;
  color: #ef4444;
}

.meta {
  margin: 10px 0;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 13px;
}

.desc {
  padding: 10px 0;
  line-height: 1.6;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 520px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }

  .actions .primary {
    width: 100%;
  }
}

.primary {
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

.hint {
  font-size: 12px;
  color: var(--muted);
}

.error {
  font-size: 13px;
  color: var(--danger);
}

.sub {
  margin: 0 0 10px;
  font-size: 16px;
}

.msgs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.msg {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  border-radius: 12px;
  padding: 10px;
}

.msgHead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.who {
  font-size: 13px;
  color: var(--muted);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid var(--border-2);
  background: var(--surface);
  flex: 0 0 auto;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatarFallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(34, 197, 94, 0.18));
}

.msgContent {
  font-size: 14px;
}

.msgTime {
  font-size: 12px;
  color: var(--muted-2);
  margin-top: 6px;
}

.empty {
  color: var(--muted);
  font-size: 13px;
}

.composer {
  display: flex;
  gap: 10px;
}

@media (max-width: 520px) {
  .composer {
    flex-direction: column;
  }

  .composer .primary {
    width: 100%;
  }
}

.input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: var(--surface);
}
</style>
