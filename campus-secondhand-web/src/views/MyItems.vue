<script setup>
import { onMounted, ref } from 'vue'
import { deleteItem, formatCategoryLabel, formatConditionLabel, listMyItems, offShelfItem, onShelfItem } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')

const keyword = ref('')
const category = ref('')
const status = ref('')

const actionLoadingId = ref(null)
const actionError = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const kw = keyword.value.trim()
    const cat = category.value.trim().toUpperCase()
    const resp = await listMyItems({ keyword: kw, category: cat, status: status.value, page: 0, size: 50 })
    items.value = resp.data || []
  } catch (e) {
    items.value = []
    const data = e?.response?.data
    errorMsg.value =
      data?.message ||
      (typeof data === 'string' ? data.slice(0, 200) : (data ? JSON.stringify(data).slice(0, 200) : '')) ||
      e?.message ||
      '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function doOffShelf(id) {
  actionError.value = ''
  actionLoadingId.value = id
  try {
    await offShelfItem(id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '下架失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doOnShelf(id) {
  actionError.value = ''
  actionLoadingId.value = id
  try {
    await onShelfItem(id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '上架失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function doDelete(it) {
  if (!it?.id) return
  const ok = window.confirm(`确认删除商品：${it.title || it.id}？删除后不可恢复。`)
  if (!ok) return
  actionError.value = ''
  actionLoadingId.value = it.id
  try {
    await deleteItem(it.id)
    await load()
  } catch (e) {
    actionError.value = e?.response?.data?.message || e?.message || '删除失败'
  } finally {
    actionLoadingId.value = null
  }
}
</script>

<template>
  <div class="page myItemsPage">
    <section class="hero surfacePanel">
      <div>
        <div class="heroTag">MY ITEMS</div>
        <div class="title">我的发布</div>
        <div class="sub">发布后会直接上架，你可以在这里查看、下架、重新上架或删除自己的商品。</div>
      </div>
      <div class="heroBadges">
        <span>支持关键词筛选</span>
        <span>状态一目了然</span>
        <span>管理更顺手</span>
      </div>
    </section>

    <div class="search card">
      <div class="filterHead">
        <div>
          <div class="filterTitle">筛选我的商品</div>
          <div class="filterSub">按关键词、分类和状态快速定位待处理商品。</div>
        </div>
        <div class="resultBadge">共 {{ items.length }} 条</div>
      </div>

      <div class="row">
        <input v-model="keyword" class="input" placeholder="搜索标题关键词" />
        <input v-model="category" class="input" placeholder="分类：BOOK / DIGITAL / LIFE / SPORT / CLOTHING" />
        <select v-model="status" class="select">
          <option value="">全部状态</option>
          <option value="PENDING_REVIEW">待审核</option>
          <option value="ON_SALE">已上架</option>
          <option value="OFF_SHELF">已下架</option>
          <option value="REJECTED">已驳回</option>
          <option value="LOCKED">交易中</option>
          <option value="SOLD">已售出</option>
        </select>
        <button class="primary" type="button" @click="load">查询</button>
      </div>

      <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
      <div v-if="actionError" class="error">{{ actionError }}</div>
    </div>

    <div v-if="loading" class="stateCard">加载中...</div>

    <div v-else class="grid">
      <div v-for="it in items" :key="it.id" class="item card card-hover">
        <div class="img">
          <img v-if="it.coverImageUrl" :src="it.coverImageUrl" alt="" loading="lazy" />
          <div v-else class="imgFallback">暂无封面</div>
          <div class="floatingPrice">￥{{ it.price }}</div>
        </div>
        <div class="body">
          <div class="name">{{ it.title }}</div>
          <div class="chips">
            <span class="chip">{{ formatCategoryLabel(it.category) }}</span>
            <span class="chip chipStatus">{{ it.status }}</span>
            <span class="chip">成色 {{ formatConditionLabel(it.conditionLevel) }}</span>
          </div>
          <div v-if="it.status === 'REJECTED' && it.auditReason" class="reason">驳回原因：{{ it.auditReason }}</div>

          <div class="actions">
            <button
              v-if="it.status === 'ON_SALE'"
              class="ghost"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doOffShelf(it.id)"
            >
              {{ actionLoadingId === it.id ? '下架中...' : '下架' }}
            </button>
            <button
              v-else-if="it.status === 'OFF_SHELF'"
              class="ghost"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doOnShelf(it.id)"
            >
              {{ actionLoadingId === it.id ? '上架中...' : '上架' }}
            </button>

            <button
              v-if="it.status === 'OFF_SHELF' || it.status === 'PENDING_REVIEW' || it.status === 'REJECTED' || it.status === 'ON_SALE'"
              class="danger"
              type="button"
              :disabled="actionLoadingId === it.id"
              @click="doDelete(it)"
            >
              {{ actionLoadingId === it.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && items.length === 0" class="empty">
      <div class="emptyTitle">暂无记录</div>
      <div class="emptySub">去“发布”提交一个商品吧。</div>
    </div>
  </div>
</template>

<style scoped>
.myItemsPage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.surfacePanel {
  position: relative;
  overflow: hidden;
}

.hero {
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 30%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.16), rgba(16, 185, 129, 0.16));
  border: 1px solid rgba(148, 163, 184, 0.22);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.heroTag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.title {
  margin-top: 14px;
  font-size: 28px;
  font-weight: 900;
  color: #0f172a;
}

.sub {
  margin-top: 8px;
  max-width: 620px;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.heroBadges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.heroBadges span {
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #334155;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.search {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.filterHead {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.filterTitle {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.filterSub {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.resultBadge {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr 200px auto;
  gap: 10px;
  align-items: center;
}

.input,
.select {
  min-height: 42px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 14px;
  padding: 0 14px;
  background: #fff;
}

.input:focus,
.select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.primary {
  min-height: 42px;
  padding: 0 18px;
  background: linear-gradient(135deg, var(--primary), #6366f1);
  color: #fff;
  border: 0;
  border-radius: 14px;
  cursor: pointer;
  font-weight: 700;
}

.error {
  margin-top: 10px;
  padding: 12px 14px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: 14px;
  color: var(--danger);
  font-size: 13px;
}

.stateCard {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.92);
  color: var(--muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.item {
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
}

.img {
  position: relative;
  height: 220px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(16, 185, 129, 0.08));
}

.img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.imgFallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 14px;
}

.floatingPrice {
  position: absolute;
  right: 14px;
  bottom: 14px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.body {
  padding: 16px;
}

.name {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.chip {
  background: rgba(241, 245, 249, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #475569;
}

.chipStatus {
  color: var(--primary);
  background: rgba(59, 130, 246, 0.08);
}

.reason {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
  font-size: 13px;
  line-height: 1.6;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.ghost,
.danger {
  min-height: 40px;
  border-radius: 12px;
  padding: 0 14px;
  cursor: pointer;
  font-weight: 700;
}

.ghost {
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: #fff;
  color: #334155;
}

.danger {
  border: 1px solid rgba(239, 68, 68, 0.22);
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
}

.empty {
  padding: 44px 0;
  text-align: center;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px dashed rgba(148, 163, 184, 0.28);
}

.emptyTitle {
  font-weight: 800;
  font-size: 20px;
}

.emptySub {
  color: var(--muted);
  margin-top: 8px;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .row {
    grid-template-columns: 1fr;
  }

  .filterHead {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .hero,
  .search,
  .item,
  .empty {
    border-radius: 18px;
  }

  .title {
    font-size: 24px;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
