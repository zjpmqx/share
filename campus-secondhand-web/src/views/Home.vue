<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { listItems, onlinePing } from '../services/api'
import { onItemApproved } from '../services/events'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const items = ref([])
const keyword = ref('')
const category = ref('')
const conditionLevel = ref('')
const priceMin = ref('')
const priceMax = ref('')
const errorMsg = ref('')

const onlineUsers = ref(0)
const tickerText = computed(() => `头条：当前在线人数 ${onlineUsers.value} 人在线`)

const categories = [
  { value: '', label: '全部分类' },
  { value: 'BOOK', label: '书籍教材' },
  { value: 'DIGITAL', label: '数码产品' },
  { value: 'LIFE', label: '生活用品' },
  { value: 'SPORT', label: '运动器材' },
  { value: 'CLOTHING', label: '服装配饰' },
  { value: 'OTHER', label: '其他' }
]

const conditions = [
  { value: '', label: '全部成色' },
  { value: 'NEW', label: '全新' },
  { value: 'LIKE_NEW', label: '几乎全新' },
  { value: 'GOOD', label: '良好' },
  { value: 'FAIR', label: '一般' }
]

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
    const cond = conditionLevel.value.trim().toUpperCase()
    
    const params = { keyword: kw, category: cat, page: 0, size: 20 }
    
    if (cond) {
      params.condition = cond
    }
    
    const resp = await listItems(params)
    let next = resp.data || []
    
    if (priceMin.value || priceMax.value) {
      const min = parseFloat(priceMin.value) || 0
      const max = parseFloat(priceMax.value) || Infinity
      next = next.filter(item => {
        const price = parseFloat(item.price)
        return price >= min && price <= max
      })
    }
    
    if (!isSameList(items.value, next)) {
      items.value = next
    }
    
    if (next.length === 0 && kw) {
      ElMessage.info('未找到相关商品，试试其他关键词吧')
    }
  } catch (e) {
    items.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  conditionLevel.value = ''
  priceMin.value = ''
  priceMax.value = ''
  load()
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
    ElMessage.success('有新商品通过审核啦！')
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
  <div class="page homePage">
    <div class="ticker" aria-label="online-ticker">
      <div class="tickerTrack">
        <div class="tickerText">
          <el-icon class="tickerIcon"><User /></el-icon>
          {{ tickerText }} · 今日推荐：先看成色再看价格，快乐捡漏～
        </div>
      </div>
    </div>

    <div class="hero">
      <div class="heroLeft">
        <div class="heroTitle">
          <el-icon class="heroCartIcon"><ShoppingCart /></el-icon>
          淘好物
          <span class="heroEmoji">(๑•̀ㅂ•́)و✧</span>
        </div>
        <div class="heroSub">校园二手 · 省钱也有好品质</div>
        <div class="heroBadges">
          <span class="badge">实时在线 {{ onlineUsers }}</span>
          <span class="badge">当前商品 {{ items.length }}</span>
          <span class="badge">支持多条件筛选</span>
        </div>
      </div>

      <el-card class="searchCard" shadow="never">
        <div class="cardTitleRow">快速淘货</div>

        <div class="searchRow">
          <el-input
            v-model="keyword"
            class="searchInput"
            placeholder="搜索关键词：教材/手机/桌子..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="load"
          />
          <el-button type="primary" :icon="Search" class="searchBtn" @click="load">搜索</el-button>
          <el-button :icon="RefreshRight" class="resetBtn" @click="resetFilters">重置</el-button>
        </div>

        <div class="filtersRow">
          <el-select v-model="category" placeholder="选择分类" clearable class="filterSelect">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>

          <el-select v-model="conditionLevel" placeholder="选择成色" clearable class="filterSelect">
            <el-option v-for="cond in conditions" :key="cond.value" :label="cond.label" :value="cond.value" />
          </el-select>

          <div class="priceFilter">
            <span class="priceLabel">价格</span>
            <el-input-number v-model="priceMin" :min="0" placeholder="最低价" :precision="2" size="default" class="priceInput" />
            <span class="priceSeparator">~</span>
            <el-input-number v-model="priceMax" :min="0" placeholder="最高价" :precision="2" size="default" class="priceInput" />
          </div>
        </div>
      </el-card>
    </div>

    <div v-if="errorMsg" class="error">
      <el-alert :title="errorMsg" type="error" show-icon :closable="false" />
    </div>

    <div v-if="loading && items.length === 0" class="skeletonContainer">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else class="grid">
      <RouterLink v-for="it in items" :key="it.id" class="itemCard" :to="`/items/${it.id}`">
        <el-card :body-style="{ padding: '0px' }" class="cardInner" shadow="hover">
          <div class="cover">
            <div class="img">
              <img v-if="it.coverImageUrl" :src="it.coverImageUrl" alt="" loading="lazy" />
              <el-empty v-else description="暂无图片" :image-size="56" />
              <div class="floatingPrice">¥{{ it.price }}</div>
            </div>
          </div>

          <div class="body">
            <div class="productTitle" :title="it.title">{{ it.title }}</div>

            <div class="chips">
              <el-tag size="small" type="info">{{ it.category }}</el-tag>
              <el-tag size="small" type="success">成色 {{ it.conditionLevel }}</el-tag>
              <el-tag size="small" type="warning">{{ it.status }}</el-tag>
            </div>

            <div class="bottom">
              <div class="priceHint">喜欢就冲～</div>
              <el-button type="primary" size="small" link>查看详情</el-button>
            </div>
          </div>
        </el-card>
      </RouterLink>
    </div>

    <div v-if="!loading && items.length === 0" class="empty">
      <el-empty description="暂无商品，来发一个让同学们眼前一亮吧">
        <el-button type="primary" @click="$router.push('/publish')">去发布商品</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.homePage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ticker {
  margin: 6px 0 4px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(16, 185, 129, 0.12));
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  padding: 7px 12px;
}

.tickerTrack {
  overflow: hidden;
  white-space: nowrap;
}

.tickerText {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding-left: 100%;
  animation: marquee 14s linear infinite;
  font-size: 12px;
  color: var(--muted);
}

.tickerIcon {
  vertical-align: -2px;
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
  margin: 2px 0 6px;
  border-radius: 18px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(99, 102, 241, 0.12), rgba(16, 185, 129, 0.13));
  border: 1px solid rgba(148, 163, 184, 0.24);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.95fr);
  gap: 14px;
  align-items: start;
}

.heroLeft {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.heroTitle {
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0.2px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.heroCartIcon {
  color: var(--primary);
}

.heroEmoji {
  font-size: 16px;
  font-weight: 700;
  color: #0f766e;
}

.heroSub {
  color: var(--muted);
  font-size: 14px;
}

.heroBadges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.searchCard {
  border-radius: 14px;
}

.cardTitleRow {
  font-size: 14px;
  font-weight: 700;
  color: var(--muted);
  margin-bottom: 10px;
}

.searchRow {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.searchInput {
  flex: 1;
}

.searchBtn,
.resetBtn {
  border-radius: 10px;
}

.filtersRow {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filterSelect {
  min-width: 140px;
}

.priceFilter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.priceLabel {
  font-size: 13px;
  color: var(--muted);
}

.priceInput {
  width: 118px;
}

.priceSeparator {
  color: var(--muted);
}

.error {
  margin: 4px 0 2px;
}

.skeletonContainer {
  padding: 20px;
  background: var(--surface);
  border-radius: 14px;
  border: 1px solid var(--border-2);
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.itemCard {
  display: block;
  text-decoration: none;
  color: inherit;
}

.cardInner {
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.cardInner:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 26px rgba(15, 23, 42, 0.12);
}

.cover {
  width: 100%;
}

.img {
  height: 176px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border-2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.floatingPrice {
  position: absolute;
  right: 10px;
  top: 10px;
  background: rgba(239, 68, 68, 0.9);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 999px;
}

.body {
  padding: 12px;
}

.productTitle {
  font-weight: 800;
  margin-bottom: 10px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.priceHint {
  font-size: 12px;
  color: var(--muted);
}

.empty {
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 14px;
  }

  .heroTitle {
    font-size: 24px;
  }

  .searchRow,
  .filtersRow {
    flex-direction: column;
    align-items: stretch;
  }

  .searchBtn,
  .resetBtn,
  .filterSelect,
  .priceInput {
    width: 100%;
  }

  .priceFilter {
    width: 100%;
  }

  .img {
    height: 210px;
  }
}
</style>
