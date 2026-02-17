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
  <div class="page">
    <div class="ticker" aria-label="online-ticker">
      <div class="tickerTrack">
        <div class="tickerText">
          <el-icon style="margin-right: 6px; vertical-align: -2px;"><User /></el-icon>
          {{ tickerText }}
        </div>
      </div>
    </div>

    <div class="hero">
      <div class="heroTitle">
        <el-icon style="margin-right: 8px; color: var(--primary);"><ShoppingCart /></el-icon>
        淘好物
      </div>
      <div class="heroSub">校园二手 · 省钱也有好品质</div>

      <el-card class="searchCard" shadow="hover">
        <div class="searchRow">
          <el-input
            v-model="keyword"
            class="searchInput"
            placeholder="搜索关键词：教材/手机/桌子..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="load"
          />
          <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
          <el-button :icon="RefreshRight" @click="resetFilters">重置</el-button>
        </div>
        
        <div class="filtersRow">
          <el-select v-model="category" placeholder="选择分类" clearable class="filterSelect">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
          
          <el-select v-model="conditionLevel" placeholder="选择成色" clearable class="filterSelect">
            <el-option v-for="cond in conditions" :key="cond.value" :label="cond.label" :value="cond.value" />
          </el-select>
          
          <div class="priceFilter">
            <span class="priceLabel">价格：</span>
            <el-input-number v-model="priceMin" :min="0" placeholder="最低价" :precision="2" size="default" class="priceInput" />
            <span class="priceSeparator">-</span>
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
              <el-empty v-else description="暂无图片" :image-size="60" />
            </div>
          </div>

          <div class="body">
            <div class="cardTitle" :title="it.title">{{ it.title }}</div>

            <div class="chips">
              <el-tag size="small" type="info">{{ it.category }}</el-tag>
              <el-tag size="small" type="success">成色 {{ it.conditionLevel }}</el-tag>
              <el-tag size="small" type="warning">{{ it.status }}</el-tag>
            </div>

            <div class="bottom">
              <div class="price">
                <span class="priceSymbol">¥</span>{{ it.price }}
              </div>
              <el-button type="primary" size="small" link>查看详情</el-button>
            </div>
          </div>
        </el-card>
      </RouterLink>
    </div>

    <div v-if="!loading && items.length === 0" class="empty">
      <el-empty description="暂无商品">
        <el-button type="primary" @click="$router.push('/publish')">去发布商品</el-button>
      </el-empty>
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
  display: flex;
  align-items: center;
}

.heroSub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 14px;
}

.searchCard {
  margin-top: 12px;
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
}

.priceLabel {
  font-size: 14px;
  color: var(--muted);
}

.priceInput {
  width: 120px;
}

.priceSeparator {
  color: var(--muted);
}

.error {
  margin: 10px 0 12px;
}

.skeletonContainer {
  padding: 20px;
  background: var(--surface);
  border-radius: var(--radius);
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
  
  .filtersRow {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filterSelect {
    width: 100%;
    min-width: auto;
  }
  
  .priceFilter {
    width: 100%;
  }
}

.itemCard {
  display: block;
  text-decoration: none;
  color: inherit;
}

.cardInner {
  height: 100%;
  transition: transform 0.15s ease;
}

.cardInner:hover {
  transform: translateY(-2px);
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
  gap: 6px;
  margin-bottom: 12px;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 22px;
  font-weight: 900;
  color: #ef4444;
}

.priceSymbol {
  font-size: 16px;
}

.empty {
  margin-top: 18px;
}
</style>
