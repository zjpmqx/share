<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { adminDeleteShare, listShares } from '../services/api'

const loading = ref(false)
const shares = ref([])
const errorMsg = ref('')
const actionLoadingId = ref(null)
const actionError = ref('')

const shareCount = computed(() => shares.value.length)
const mediaCount = computed(() => shares.value.filter((item) => item.mediaUrl).length)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const resp = await listShares({ page: 0, size: 50 })
    shares.value = resp.data || []
  } catch (e) {
    shares.value = []
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function doDelete(it) {
  if (!it?.id) return
  try {
    await ElMessageBox.confirm(`确认删除分享：${it.title || it.id}？将同时删除评论与本地媒体文件。`, '删除分享', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    actionError.value = ''
    actionLoadingId.value = it.id
    try {
      await adminDeleteShare(it.id)
      await load()
    } catch (e) {
      actionError.value = e?.response?.data?.message || e?.message || '删除失败'
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
        <div class="eyebrow">Share Moderation</div>
        <div class="title">好物分享管理</div>
        <div class="sub">统一信息层级、媒体信息展示与删除确认方式，帮助后台更顺畅地处理内容治理任务。</div>
      </div>
      <div class="heroStats">
        <div class="statCard primary">
          <span class="statLabel">分享总数</span>
          <strong class="statValue">{{ shareCount }}</strong>
        </div>
        <div class="statCard accent">
          <span class="statLabel">含媒体内容</span>
          <strong class="statValue">{{ mediaCount }}</strong>
        </div>
        <button class="ghostBtn" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </div>
    </section>

    <div v-if="errorMsg" class="feedback error">{{ errorMsg }}</div>
    <div v-if="actionError" class="feedback error">{{ actionError }}</div>
    <div v-if="loading" class="panelState">加载中...</div>

    <section v-else class="contentBlock">
      <div v-if="shares.length" class="listGrid">
        <article v-for="it in shares" :key="it.id" class="shareCard surface-card">
          <div class="cardHead">
            <div>
              <div class="cardTitle">{{ it.title }}</div>
              <div class="cardMeta">
                <span class="chip media">{{ it.mediaType || 'NONE' }}</span>
                <span class="chip neutral">ID {{ it.id }}</span>
                <span class="chip neutral">用户 {{ it.userId }}</span>
              </div>
            </div>
            <button class="actionBtn danger" type="button" :disabled="actionLoadingId === it.id" @click="doDelete(it)">
              {{ actionLoadingId === it.id ? '删除中...' : '删除' }}
            </button>
          </div>

          <div class="resourceList">
            <div class="resourceRow">
              <span class="resourceLabel">媒体地址</span>
              <span class="resourceValue">{{ it.mediaUrl || '无' }}</span>
            </div>
            <div class="resourceRow">
              <span class="resourceLabel">外部链接</span>
              <span class="resourceValue">{{ it.linkUrl || '无' }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="emptyState surface-card">
        <div class="emptyTitle">暂无分享</div>
        <div class="emptySub">当前没有可处理的分享内容，稍后刷新即可查看最新动态。</div>
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
  background: rgba(79, 70, 229, 0.1);
  color: #4338ca;
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
  gap: 12px;
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
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border-color: #c7d2fe;
}

.statCard.accent {
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

.shareCard {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.shareCard:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(67, 56, 202, 0.12);
}

.cardHead {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.cardTitle {
  font-size: 18px;
  font-weight: 900;
  line-height: 1.4;
  color: #0f172a;
}

.cardMeta {
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

.chip.media {
  color: #3730a3;
  background: #eef2ff;
  border-color: #c7d2fe;
}

.chip.neutral {
  color: #334155;
  background: #f8fafc;
  border-color: #dbeafe;
}

.resourceList {
  display: grid;
  gap: 10px;
}

.resourceRow {
  padding: 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.resourceLabel {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.resourceValue {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  word-break: break-all;
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
  .cardHead {
    flex-direction: column;
  }

  .heroStats {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .hero,
  .shareCard,
  .panelState,
  .emptyState {
    padding: 18px;
  }

  .title {
    font-size: 24px;
  }

  .actionBtn,
  .ghostBtn {
    width: 100%;
  }
}
</style>