<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { adminListItems, adminOffShelfItem, adminDeleteItem, adminOffShelfAll, adminDeleteAllItems } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')

const status = ref('')

const actionLoadingId = ref(null)
const actionError = ref('')
const bulkLoading = ref('')

const filteredCount = computed(() => items.value.length)
const onSaleCount = computed(() => items.value.filter((item) => item.status === 'ON_SALE').length)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await adminListItems({ page: 0, size: 50, status: status.value })
    items.value = resp.data || []
  } catch (e) {
    items.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function doOffShelf(it) {
  if (!it?.id) return
  try {
    await ElMessageBox.confirm(`确认下架商品：${it.title || it.id}？`, '下架商品', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    actionError.value = ''
    actionLoadingId.value = it.id
    try {
      await adminOffShelfItem(it.id)
      await load()
    } catch (e) {
      actionError.value = e?.response?.data?.message || e?.message || '下架失败'
    } finally {
      actionLoadingId.value = null
    }
  } catch {
  }
}

async function doDelete(it) {
  if (!it?.id) return
  try {
    await ElMessageBox.confirm(`确认删除商品：${it.title || it.id}？将同时删除图片、留言、订单记录及本地文件。`, '删除商品', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    actionError.value = ''
    actionLoadingId.value = it.id
    try {
      await adminDeleteItem(it.id)
      await load()
    } catch (e) {
      actionError.value = e?.response?.data?.message || e?.message || '删除失败'
    } finally {
      actionLoadingId.value = null
    }
  } catch {
  }
}

async function doOffShelfAll() {
  try {
    const { value } = await ElMessageBox.prompt('输入 OFF_SHELF_ALL 确认下架所有在售商品', '批量下架', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      inputPlaceholder: 'OFF_SHELF_ALL'
    })
    const confirmValue = value || ''
    if (confirmValue !== 'OFF_SHELF_ALL') return
    actionError.value = ''
    bulkLoading.value = 'off-shelf'
    try {
      await adminOffShelfAll(confirmValue)
      await load()
    } catch (e) {
      actionError.value = e?.response?.data?.message || e?.message || '下架全部失败'
    } finally {
      bulkLoading.value = ''
    }
  } catch {
  }
}

async function doDeleteAll() {
  try {
    const { value } = await ElMessageBox.prompt('危险操作：输入 DELETE_ALL 确认删除所有商品', '批量删除', {
      confirmButtonText: '继续',
      cancelButtonText: '取消',
      inputPlaceholder: 'DELETE_ALL'
    })
    const confirmValue = value || ''
    if (confirmValue !== 'DELETE_ALL') return
    await ElMessageBox.confirm('再次确认：此操作不可恢复，确定删除所有商品吗？', '最终确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    actionError.value = ''
    bulkLoading.value = 'delete'
    try {
      await adminDeleteAllItems(confirmValue)
      await load()
    } catch (e) {
      actionError.value = e?.response?.data?.message || e?.message || '删除全部失败'
    } finally {
      bulkLoading.value = ''
    }
  } catch {
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <section class="hero surface-card">
      <div>
        <div class="eyebrow">Inventory Control</div>
        <div class="title">商品管理</div>
        <div class="sub">统一筛选区、批量操作区与列表卡片层级，让高频治理操作更直观，同时保持原有接口与流程不变。</div>
      </div>
      <div class="heroStats">
        <div class="statCard primary">
          <span class="statLabel">筛选结果</span>
          <strong class="statValue">{{ filteredCount }}</strong>
        </div>
        <div class="statCard accent">
          <span class="statLabel">在售商品</span>
          <strong class="statValue">{{ onSaleCount }}</strong>
        </div>
      </div>
    </section>

    <section class="toolbar surface-card">
      <div class="filterGroup">
        <label class="fieldLabel">状态筛选</label>
        <select v-model="status" class="select" :disabled="loading" @change="load">
          <option value="">全部状态</option>
          <option value="PENDING_REVIEW">PENDING_REVIEW</option>
          <option value="ON_SALE">ON_SALE</option>
          <option value="OFF_SHELF">OFF_SHELF</option>
          <option value="LOCKED">LOCKED</option>
          <option value="SOLD">SOLD</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </div>

      <div class="toolbarActions">
        <button class="ghostBtn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
        <button class="actionBtn warn" type="button" :disabled="bulkLoading === 'off-shelf' || bulkLoading === 'delete'" @click="doOffShelfAll">
          {{ bulkLoading === 'off-shelf' ? '处理中...' : '下架全部在售' }}
        </button>
        <button class="actionBtn danger" type="button" :disabled="bulkLoading === 'delete' || bulkLoading === 'off-shelf'" @click="doDeleteAll">
          {{ bulkLoading === 'delete' ? '处理中...' : '删除全部商品' }}
        </button>
      </div>
    </section>

    <div v-if="errorMsg" class="feedback error">{{ errorMsg }}</div>
    <div v-if="actionError" class="feedback error">{{ actionError }}</div>
    <div v-if="loading" class="panelState">加载中...</div>

    <section v-else class="contentBlock">
      <div v-if="items.length" class="listGrid">
        <article v-for="it in items" :key="it.id" class="itemCard surface-card">
          <div class="itemTop">
            <div class="itemMain">
              <div class="itemTitle">{{ it.title }}</div>
              <div class="itemMeta">
                <span class="chip neutral">ID {{ it.id }}</span>
                <span class="chip neutral">分类 {{ it.category }}</span>
                <span class="chip status">状态 {{ it.status }}</span>
                <span class="chip neutral">卖家 {{ it.sellerId }}</span>
              </div>
            </div>
            <div class="singleActions">
              <button class="actionBtn warn" type="button" :disabled="actionLoadingId === it.id" @click="doOffShelf(it)">
                {{ actionLoadingId === it.id ? '处理中...' : '下架' }}
              </button>
              <button class="actionBtn danger" type="button" :disabled="actionLoadingId === it.id" @click="doDelete(it)">
                {{ actionLoadingId === it.id ? '处理中...' : '删除' }}
              </button>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="emptyState surface-card">
        <div class="emptyTitle">暂无商品</div>
        <div class="emptySub">当前筛选条件下没有匹配结果，试试切换状态后再查看。</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  color: var(--admin-text);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.surface-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
  border: 1px solid var(--admin-border-soft);
  border-radius: 22px;
  box-shadow: var(--admin-shadow);
  transition: transform var(--transition-slow), box-shadow var(--transition-slow), border-color var(--transition-slow);
}

.hero,
.toolbar {
  padding: 22px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.hero {
  align-items: stretch;
}

.toolbar {
  align-items: flex-end;
  flex-wrap: wrap;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.title {
  margin-top: 12px;
  font-size: 28px;
  font-weight: 900;
  line-height: 1.1;
}

.sub {
  margin-top: 8px;
  max-width: 720px;
  color: var(--admin-muted);
  font-size: 14px;
  line-height: 1.7;
}

.heroStats,
.toolbarActions,
.singleActions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.heroStats {
  justify-content: flex-end;
}

.statCard {
  min-width: 132px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.statCard.primary {
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
  border-color: #bfdbfe;
}

.statCard.accent {
  background: linear-gradient(135deg, #ecfdf5, #f0fdf4);
  border-color: #bbf7d0;
}

.statLabel {
  font-size: 12px;
  color: #475569;
}

.statValue {
  font-size: 26px;
  font-weight: 900;
  color: #0f172a;
}

.filterGroup {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}

.fieldLabel {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.select {
  min-height: 44px;
  background: #ffffff;
  border: 1px solid var(--admin-border);
  color: var(--admin-text);
  padding: 0 14px;
  border-radius: 14px;
  outline: none;
}

.ghostBtn,
.actionBtn {
  border-radius: 14px;
  padding: 11px 16px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.ghostBtn {
  background: #ffffff;
  border-color: var(--admin-border);
  color: var(--admin-text);
}

.ghostBtn:hover,
.actionBtn:hover {
  transform: translateY(-1px);
}

.ghostBtn:disabled,
.actionBtn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.actionBtn.warn {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-color: #fcd34d;
  color: #92400e;
}

.actionBtn.danger {
  background: linear-gradient(135deg, #ffe4e6, #fecdd3);
  border-color: #fda4af;
  color: #9f1239;
}

.feedback {
  border-radius: 14px;
  padding: 12px 14px;
  font-size: 13px;
}

.error {
  color: #be123c;
  border: 1px solid #fecdd3;
  background: #fff1f2;
}

.panelState,
.emptyState {
  padding: 28px 22px;
  color: var(--admin-muted);
}

.contentBlock {
  display: flex;
  flex-direction: column;
}

.listGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
}

.itemCard {
  padding: 18px;
  transition: transform var(--transition-slow), box-shadow var(--transition-slow), border-color var(--transition-slow);
}

.itemCard:hover {
  transform: translateY(-4px);
  border-color: rgba(59, 130, 246, 0.22);
  box-shadow: 0 18px 34px rgba(148, 163, 184, 0.18);
}

.itemTop {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.itemMain {
  min-width: 0;
}

.itemTitle {
  font-size: 18px;
  font-weight: 900;
  line-height: 1.4;
  color: #0f172a;
}

.itemMeta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
}

.chip.neutral {
  color: #334155;
  background: #f8fafc;
  border-color: #dbeafe;
}

.chip.status {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.singleActions {
  justify-content: flex-end;
  flex-shrink: 0;
}

.emptyState {
  text-align: center;
}

.emptyTitle {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.emptySub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--admin-muted);
}

@media (max-width: 900px) {
  .hero,
  .itemTop {
    flex-direction: column;
  }

  .heroStats {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .hero,
  .toolbar,
  .itemCard,
  .panelState,
  .emptyState {
    padding: 18px;
  }

  .title {
    font-size: 24px;
  }

  .toolbarActions,
  .singleActions {
    width: 100%;
    flex-direction: column;
  }

  .ghostBtn,
  .actionBtn,
  .select {
    width: 100%;
  }
}
</style>