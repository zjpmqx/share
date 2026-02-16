<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listItems, onlinePing } from '../services/api'
import { onItemApproved } from '../services/events'

const loading = ref(false)
const items = ref([])
const keyword = ref('')
const category = ref('')
const errorMsg = ref('')

const onlineUsers = ref(0)
const tickerText = computed(() => `头条：当前在线人数 ${onlineUsers.value} 人在线`)

function isSameList(a, b) {
  if (!Array.isArray(a) || !Array.isArray(b)) return false
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i]?.id !== b[i]?.id) return false
  }
  return true
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const kw = keyword.value.trim()
    const cat = category.value.trim().toUpperCase()
    const resp = await listItems({ keyword: kw, category: cat, page: 0, size: 20 })
    const next = resp.data || []
    if (!isSameList(items.value, next)) {
      items.value = next
    }
  } catch (e) {
    items.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

let offItemApproved = null
let onlineTimer = null

async function refreshOnline() {
  try {
    const resp = await onlinePing()
    const n = Number(resp?.data)
    onlineUsers.value = Number.isFinite(n) ? n : 0
  } catch {
  }
}

onMounted(() => {
  load()
  refreshOnline()
  onlineTimer = setInterval(() => {
    refreshOnline()
  }, 15000)
  offItemApproved = onItemApproved(() => {
    load()
  })
})

onUnmounted(() => {
  if (offItemApproved) {
    offItemApproved()
    offItemApproved = null
  }
  if (onlineTimer) {
    clearInterval(onlineTimer)
    onlineTimer = null
  }
})
</script>

<template>
  <div class="page">
    <div class="ticker" aria-label="online-ticker">
      <div class="tickerTrack">
        <div class="tickerText">{{ tickerText }}</div>
      </div>
    </div>

    <div class="hero">
      <div class="heroTitle">淘好物</div>
      <div class="heroSub">校园二手 · 省钱也有好品质</div>

      <div class="searchCard">
        <div class="filters">
          <input v-model="keyword" class="input" placeholder="搜索关键词：教材/手机/桌子..." />
          <input v-model="category" class="input" placeholder="分类：BOOK / DIGITAL / LIFE..." />
          <button class="primary" type="button" @click="load">搜索</button>
        </div>
        <div class="tips muted">提示：分类可直接输入枚举值，例如 BOOK / DIGITAL / LIFE。</div>
      </div>
    </div>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

    <div v-if="loading && items.length === 0">加载中...</div>

    <div v-else class="grid">
      <RouterLink v-for="it in items" :key="it.id" class="itemCard" :to="`/items/${it.id}`">
        <div class="cover">
          <div class="img">
            <img v-if="it.coverImageUrl" :src="it.coverImageUrl" alt="" loading="lazy" />
          </div>
        </div>

        <div class="body">
          <div class="cardTitle">{{ it.title }}</div>

          <div class="chips">
            <span class="chip">{{ it.category }}</span>
            <span class="chip">成色 {{ it.conditionLevel }}</span>
            <span class="chip">{{ it.status }}</span>
          </div>

          <div class="bottom">
            <div class="price">￥{{ it.price }}</div>
          </div>
        </div>
      </RouterLink>
    </div>

    <div v-if="!loading && items.length === 0" class="empty">
      <div class="emptyTitle">暂无商品</div>
      <div class="emptySub">换个关键词试试，或者去发布一个新商品。</div>
    </div>
  </div>
</template>

<style scoped>
.ticker {
  margin: 6px 0 10px;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 12px;
  padding: 6px 10px;
}

.tickerTrack {
  overflow: hidden;
  white-space: nowrap;
}

.tickerText {
  display: inline-block;
  padding-left: 100%;
  animation: marquee 12s linear infinite;
  font-size: 12px;
  color: var(--muted);
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.hero {
  margin: 6px 0 14px;
}

.heroTitle {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.2px;
}

.heroSub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 14px;
}

.searchCard {
  margin-top: 12px;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  padding: 12px;
  box-shadow: var(--shadow);
}

.filters {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 10px;
  align-items: center;
}

@media (max-width: 900px) {
  .filters {
    grid-template-columns: 1fr;
  }
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
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.primary:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

.tips {
  margin-top: 10px;
  font-size: 12px;
}

.error {
  margin: 10px 0 12px;
  padding: 10px 12px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.18);
  border-radius: var(--radius);
  color: var(--danger);
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.itemCard {
  display: block;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius);
  text-decoration: none;
  color: inherit;
  box-shadow: var(--shadow-sm);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  overflow: hidden;
}

.itemCard:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  border-color: var(--border);
}

.cover {
  width: 100%;
}

.img {
  height: 168px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 520px) {
  .img {
    height: 210px;
  }
}

.img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.body {
  padding: 12px;
}

.cardTitle {
  font-weight: 800;
  margin-bottom: 10px;
  line-height: 1.35;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 12px;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 20px;
  font-weight: 900;
  color: #ef4444;
}

.empty {
  margin-top: 18px;
  text-align: center;
  padding: 18px 12px;
  color: var(--muted);
}

.emptyTitle {
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
}

.emptySub {
  font-size: 13px;
}
</style>
