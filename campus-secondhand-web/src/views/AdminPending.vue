<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { adminAuditItem, adminPendingItems, formatCategoryLabel, formatConditionLabel } from '../services/api'

const loading = ref(false)
const items = ref([])
const errorMsg = ref('')
const actionLoadingId = ref(null)

const pendingCount = computed(() => items.value.length)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await adminPendingItems({ page: 0, size: 20 })
    items.value = resp.data || []
  } catch (e) {
    items.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败（需要 ADMIN 权限）'
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  if (!id) return
  actionLoadingId.value = id
  errorMsg.value = ''
  try {
    await adminAuditItem(id, { action: 'APPROVE' })
    await load()
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '审核通过失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function reject(id) {
  if (!id) return
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（可选）', '驳回商品', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：图片不清晰 / 描述不完整'
    })
    actionLoadingId.value = id
    errorMsg.value = ''
    try {
      await adminAuditItem(id, { action: 'REJECT', reason: value || '' })
      await load()
    } catch (e) {
      errorMsg.value = e?.response?.data?.message || e?.message || '驳回失败'
    } finally {
      actionLoadingId.value = null
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
        <div class="eyebrow">Pending Review</div>
        <div class="title">待审核商品</div>
        <div class="sub">保持业务逻辑不变，统一标题区、信息卡片与审核操作体验，方便快速判断与处理。</div>
      </div>
      <div class="heroStats">
        <div class="statCard primary">
          <span class="statLabel">当前待审</span>
          <strong class="statValue">{{ pendingCount }}</strong>
        </div>
        <button class="ghostBtn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
    </section>

    <div v-if="errorMsg" class="feedback error">{{ errorMsg }}</div>
    <div v-if="loading" class="panelState">加载中...</div>

    <section v-else class="contentBlock">
      <div v-if="items.length" class="cardGrid">
        <article v-for="it in items" :key="it.id" class="itemCard surface-card">
          <div class="itemTop">
            <div class="itemMain">
              <div class="itemTitle">{{ it.title }}</div>
              <div class="itemMeta">
                <span class="chip neutral">分类 {{ formatCategoryLabel(it.category) }}</span>
                <span class="chip neutral">成色 {{ formatConditionLabel(it.conditionLevel) }}</span>
                <span class="chip warning">状态 {{ it.status }}</span>
              </div>
            </div>
            <div class="priceWrap">
              <span class="priceLabel">建议售价</span>
              <strong class="price">￥{{ it.price }}</strong>
            </div>
          </div>

          <div class="descBlock">
            <div class="descLabel">商品描述</div>
            <div class="desc">{{ it.description || '（无描述）' }}</div>
          </div>

          <div class="actions">
            <button class="actionBtn success" type="button" :disabled="actionLoadingId === it.id" @click="approve(it.id)">
              {{ actionLoadingId === it.id ? '处理中...' : '通过' }}
            </button>
            <button class="actionBtn danger" type="button" :disabled="actionLoadingId === it.id" @click="reject(it.id)">
              {{ actionLoadingId === it.id ? '处理中...' : '驳回' }}
            </button>
          </div>
        </article>
      </div>

      <div v-else class="emptyState surface-card">
        <div class="emptyTitle">暂无待审核商品</div>
        <div class="emptySub">当前审核队列为空，可以稍后刷新再看。</div>
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
}

.hero {
  padding: 22px;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: stretch;
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

.heroStats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: stretch;
  gap: 12px;
}

.statCard {
  min-width: 132px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
}

.statCard.primary {
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
  border-color: #bfdbfe;
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
  align-self: center;
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

.cardGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
}

.itemCard {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.itemCard:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.12);
}

.itemTop {
  display: flex;
  justify-content: space-between;
  gap: 14px;
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

.chip.warning {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.priceWrap {
  flex-shrink: 0;
  min-width: 96px;
  padding: 12px 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.08), rgba(250, 204, 21, 0.16));
  text-align: right;
}

.priceLabel {
  display: block;
  font-size: 12px;
  color: #9a3412;
}

.price {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  font-weight: 900;
  color: #c2410c;
}

.descBlock {
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.descLabel {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.desc {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  word-break: break-word;
}

.actions {
  display: flex;
  gap: 10px;
}

.actionBtn {
  min-width: 108px;
}

.actionBtn.success {
  background: linear-gradient(135deg, #0ea5e9, #22c55e);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.2);
}

.actionBtn.danger {
  background: linear-gradient(135deg, #fb7185, #f43f5e);
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(244, 63, 94, 0.18);
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

  .priceWrap {
    text-align: left;
  }
}

@media (max-width: 640px) {
  .admin-page {
    gap: 14px;
  }

  .hero,
  .itemCard,
  .panelState,
  .emptyState {
    padding: 18px;
  }

  .title {
    font-size: 24px;
  }

  .actions {
    flex-direction: column;
  }

  .actionBtn,
  .ghostBtn {
    width: 100%;
  }
}
</style>