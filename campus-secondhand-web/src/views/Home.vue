<script setup>
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  CATEGORY_OPTIONS,
  CONDITION_OPTIONS,
  formatCategoryLabel,
  formatConditionLabel,
  listItems,
  normalizeConditionValue,
  onlinePing,
} from '../services/api'
import { onItemApproved } from '../services/events'
import { getToken } from '../services/auth'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const loadingMore = ref(false)
const items = ref([])
const keyword = ref('')
const category = ref('')
const conditionLevel = ref('')
const priceMin = ref(null)
const priceMax = ref(null)
const errorMsg = ref('')
const page = ref(0)
const pageSize = 20
const hasMore = ref(false)

const onlineUsers = ref(0)
const tickerText = computed(() => `头条：当前在线人数 ${onlineUsers.value} 人在线`)
const categories = CATEGORY_OPTIONS
const conditions = CONDITION_OPTIONS
const isLoggedIn = computed(() => !!getToken())
const activeFilterCount = computed(() => {
  let count = 0
  if (keyword.value.trim()) count += 1
  if (category.value) count += 1
  if (conditionLevel.value) count += 1
  if (priceMin.value !== null && priceMin.value !== '') count += 1
  if (priceMax.value !== null && priceMax.value !== '') count += 1
  return count
})

function isSameList(a, b) {
  if (!Array.isArray(a) || !Array.isArray(b)) return false
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i]?.id !== b[i]?.id) return false
  }
  return true
}

function parseOptionalPrice(value) {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function applyClientSideFilters(list) {
  const expectedCondition = normalizeConditionValue(conditionLevel.value)
  const min = parseOptionalPrice(priceMin.value)
  const max = parseOptionalPrice(priceMax.value)

  return (Array.isArray(list) ? list : []).filter((item) => {
    if (expectedCondition && normalizeConditionValue(item?.conditionLevel) !== expectedCondition) {
      return false
    }
    const price = Number(item?.price)
    if (!Number.isFinite(price)) {
      return false
    }
    if (min !== null && price < min) {
      return false
    }
    if (max !== null && price > max) {
      return false
    }
    return true
  })
}

async function load(targetPage = 0) {
  const isLoadMore = targetPage > 0
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  errorMsg.value = ''
  try {
    const kw = keyword.value.trim()
    const cat = String(category.value || '').trim().toUpperCase()
    const params = { keyword: kw, category: cat, page: targetPage, size: pageSize }

    const resp = await listItems(params)
    const currentPageItems = applyClientSideFilters(resp.data || [])
    const next = isLoadMore ? [...items.value, ...currentPageItems] : currentPageItems

    page.value = targetPage
    hasMore.value = Array.isArray(resp.data) && resp.data.length === pageSize

    if (!isSameList(items.value, next)) {
      items.value = next
    }

    if (!isLoadMore && next.length === 0 && (kw || cat || conditionLevel.value || priceMin.value !== null || priceMax.value !== null)) {
      ElMessage.info('未找到符合筛选条件的商品，试试放宽条件吧')
    }
  } catch (e) {
    if (!isLoadMore) {
      items.value = []
    }
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
    ElMessage.error(errorMsg.value)
  } finally {
    if (isLoadMore) {
      loadingMore.value = false
    } else {
      loading.value = false
    }
  }
}

function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  load(page.value + 1)
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  conditionLevel.value = ''
  priceMin.value = null
  priceMax.value = null
  page.value = 0
  hasMore.value = false
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

    <section class="hero">
      <div class="heroContent">
        <div class="heroBadge">校园二手精选站</div>
        <div class="heroTitleWrap">
          <div class="heroTitle">
            <el-icon class="heroCartIcon"><ShoppingCart /></el-icon>
            淘好物
            <span class="heroEmoji">(๑•̀ㅂ•́)و✧</span>
          </div>
          <div class="heroSub">把闲置变成惊喜，用更轻松的方式找到高性价比好物。</div>
        </div>

        <div class="heroHighlights">
          <div class="highlightCard highlightPrimary">
            <div class="highlightLabel">实时在线</div>
            <div class="highlightValue">{{ onlineUsers }}</div>
            <div class="highlightMeta">随时看看同学们正在淘什么</div>
          </div>
          <div class="highlightCard glow-card floaty">
            <div class="highlightLabel">当前展示</div>
            <div class="highlightValue">{{ items.length }}</div>
            <div class="highlightMeta">支持关键词、分类、成色与价格筛选</div>
          </div>
        </div>

        <div class="heroBadges">
          <span class="badge">未登录也能浏览</span>
          <span class="badge">支持多条件筛选</span>
          <span class="badge">商品持续上新</span>
        </div>
      </div>

      <el-card class="searchCard glow-card motion-enter-soft motion-delay-2" shadow="never">
        <div class="cardTop">
          <div>
            <div class="cardEyebrow">快速淘货</div>
            <div class="cardTitle">按你关心的条件精准筛选</div>
          </div>
          <div class="resultPanel">
            <div class="resultValue">{{ items.length }}</div>
            <div class="resultLabel">当前结果</div>
          </div>
        </div>

        <div class="searchRow">
          <el-input
            v-model="keyword"
            class="searchInput"
            placeholder="搜索关键词：教材 / 手机 / 桌子..."
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
            <span class="priceLabel">价格区间</span>
            <el-input-number v-model="priceMin" :min="0" placeholder="最低价" :precision="2" size="default" class="priceInput" />
            <span class="priceSeparator">~</span>
            <el-input-number v-model="priceMax" :min="0" placeholder="最高价" :precision="2" size="default" class="priceInput" />
          </div>
        </div>

        <div class="filterSummary">
          <span class="summaryTag">已启用筛选 {{ activeFilterCount }} 项</span>
          <span class="summaryText">想更快找到心仪商品？试试组合关键词、成色和预算范围。</span>
        </div>
      </el-card>
    </section>

    <div v-if="errorMsg" class="error">
      <el-alert :title="errorMsg" type="error" show-icon :closable="false" />
    </div>

    <div v-if="loading && items.length === 0" class="skeletonContainer">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else>
      <section class="contentHeader motion-enter motion-delay-2">
        <div>
          <div class="contentEyebrow">好物广场</div>
          <h2 class="contentTitle">发现同学们正在出售的精选闲置</h2>
        </div>
        <div class="contentMeta">
          <span class="metaChip">在线 {{ onlineUsers }}</span>
          <span class="metaChip">展示 {{ items.length }} 件</span>
        </div>
      </section>

      <div v-if="items.length > 0" class="grid motion-enter-soft motion-delay-3">
        <RouterLink v-for="(it, index) in items" :key="it.id" class="itemCard" :style="{ '--card-delay': `${Math.min(index, 7) * 60}ms` }" :to="`/items/${it.id}`">
          <el-card :body-style="{ padding: '0px' }" class="cardInner glow-card" shadow="hover">
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
                <el-tag size="small" type="info">{{ formatCategoryLabel(it.category) }}</el-tag>
                <el-tag size="small" type="success">成色 {{ formatConditionLabel(it.conditionLevel) }}</el-tag>
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

      <div v-else class="emptyState motion-enter motion-delay-3">
        <el-empty description="暂无商品，来发一个让同学们眼前一亮吧">
          <template #image>
            <div class="emptyIllustration">✨</div>
          </template>
          <div class="emptyText">现在发闲置，也许下一位同学正好在找它。</div>
          <el-button v-if="isLoggedIn" type="primary" @click="$router.push('/publish')">去发布商品</el-button>
          <el-button v-else type="primary" @click="$router.push('/login?redirect=/publish')">登录后发布</el-button>
        </el-empty>
      </div>
    </template>

    <div v-if="items.length > 0" class="loadMoreWrap">
      <el-button v-if="hasMore" :loading="loadingMore" plain class="loadMoreBtn" @click="loadMore">
        {{ loadingMore ? '加载中...' : '加载更多商品' }}
      </el-button>
      <div v-else class="loadMoreEnd">已经到底啦，当前条件下的商品都展示完了</div>
    </div>
  </div>
</template>

<style scoped>
.homePage {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ticker {
  margin-top: 4px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(16, 185, 129, 0.12));
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 14px;
  padding: 8px 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
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
  position: relative;
  overflow: hidden;
  border-radius: 26px;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.65), transparent 28%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(99, 102, 241, 0.12), rgba(16, 185, 129, 0.13));
  border: 1px solid rgba(148, 163, 184, 0.24);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  gap: 18px;
  align-items: stretch;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.08);
}

.heroContent {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.heroBadge {
  display: inline-flex;
  align-self: flex-start;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.22);
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
}

.heroTitleWrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.heroTitle {
  font-size: 36px;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0.3px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #0f172a;
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
  max-width: 560px;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.75;
}

.heroHighlights {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.highlightCard {
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.highlightPrimary {
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.16), rgba(255, 255, 255, 0.84));
}

.highlightLabel {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 8px;
}

.highlightValue {
  font-size: 32px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1;
}

.highlightMeta {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--muted);
}

.heroBadges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.badge {
  font-size: 12px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: #334155;
}

.searchCard {
  height: 100%;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
}

.cardTop {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.cardEyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2563eb;
  margin-bottom: 8px;
}

.cardTitle {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.35;
  color: #0f172a;
}

.resultPanel {
  flex-shrink: 0;
  min-width: 96px;
  padding: 12px 14px;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.12), rgba(99, 102, 241, 0.08));
  border: 1px solid rgba(148, 163, 184, 0.2);
  text-align: center;
}

.resultValue {
  font-size: 26px;
  line-height: 1;
  font-weight: 900;
  color: #1d4ed8;
}

.resultLabel {
  margin-top: 6px;
  font-size: 12px;
  color: var(--muted);
}

.searchRow {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.searchInput {
  flex: 1;
}

.searchBtn,
.resetBtn,
.loadMoreBtn {
  border-radius: 12px;
}

.filtersRow {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filterSelect {
  min-width: 148px;
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
  width: 122px;
}

.priceSeparator {
  color: var(--muted);
}

.filterSummary {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed rgba(148, 163, 184, 0.26);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.summaryTag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.summaryText {
  color: var(--muted);
  font-size: 13px;
}

.error {
  margin-top: -4px;
}

.skeletonContainer {
  padding: 24px;
  background: var(--surface);
  border-radius: 20px;
  border: 1px solid var(--border-2);
}

.contentHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.contentEyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #0f766e;
  margin-bottom: 6px;
}

.contentTitle {
  margin: 0;
  font-size: 24px;
  line-height: 1.35;
  color: #0f172a;
}

.contentMeta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metaChip {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(148, 163, 184, 0.22);
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.itemCard {
  display: block;
  text-decoration: none;
  color: inherit;
}

.cardInner {
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.16);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.cardInner:hover {
  transform: translateY(-7px);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
  border-color: rgba(59, 130, 246, 0.22);
}

.itemCard:hover .img img {
  transform: scale(1.03);
}

.cover {
  width: 100%;
}

.img {
  height: 196px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.92));
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
  transition: transform var(--transition-slow);
}

.floatingPrice {
  position: absolute;
  right: 12px;
  top: 12px;
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  padding: 6px 10px;
  border-radius: 999px;
  box-shadow: 0 10px 20px rgba(239, 68, 68, 0.22);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.itemCard:hover .floatingPrice {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 14px 26px rgba(239, 68, 68, 0.26);
}

.body {
  padding: 14px;
}

.productTitle {
  font-weight: 800;
  font-size: 16px;
  margin-bottom: 12px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 46px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.priceHint {
  font-size: 12px;
  color: var(--muted);
}

.emptyState {
  border-radius: 22px;
  padding: 28px 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(248, 250, 252, 0.96));
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.emptyIllustration {
  width: 90px;
  height: 90px;
  margin: 0 auto;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(16, 185, 129, 0.14));
  font-size: 42px;
}

.emptyText {
  margin: 6px 0 14px;
  color: var(--muted);
  font-size: 14px;
}

.loadMoreWrap {
  display: flex;
  justify-content: center;
  margin: -2px 0 16px;
}

.loadMoreEnd {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero {
    padding: 20px;
  }

  .heroTitle {
    font-size: 30px;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .homePage {
    gap: 14px;
  }

  .hero {
    padding: 16px;
    border-radius: 20px;
  }

  .heroTitle {
    font-size: 24px;
  }

  .heroHighlights {
    grid-template-columns: 1fr;
  }

  .cardTop,
  .searchRow,
  .filtersRow,
  .filterSummary,
  .contentHeader,
  .bottom {
    flex-direction: column;
    align-items: stretch;
  }

  .resultPanel,
  .searchBtn,
  .resetBtn,
  .filterSelect,
  .priceInput,
  .loadMoreBtn {
    width: 100%;
  }

  .priceFilter {
    width: 100%;
  }

  .grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .img {
    height: 220px;
  }

  .contentTitle,
  .cardTitle {
    font-size: 18px;
  }
}
</style>
