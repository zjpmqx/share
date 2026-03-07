<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { createOrder, formatCategoryLabel, formatConditionLabel, getItem, listMessages, me, postMessage } from '../services/api'
import { getToken } from '../services/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()

const item = ref(null)
const loading = ref(false)
const msgLoading = ref(false)
const messages = ref([])
const content = ref('')

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
  item.value = null
  try {
    const resp = await getItem(route.params.id)
    item.value = resp.data
  } catch {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  msgLoading.value = true
  messages.value = []
  try {
    const resp = await listMessages(route.params.id)
    messages.value = resp.data || []
  } catch {
  } finally {
    msgLoading.value = false
  }
}

async function reloadByRouteParam() {
  content.value = ''
  await Promise.all([loadItem(), loadMessages()])
}

async function sendMessage() {
  if (!content.value.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  posting.value = true
  try {
    await postMessage(route.params.id, { content: content.value })
    content.value = ''
    ElMessage.success('留言发送成功')
    await loadMessages()
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '发送失败'
    ElMessage.error(errMsg)
  } finally {
    posting.value = false
  }
}

async function orderCreate() {
  ordering.value = true
  try {
    await createOrder({ itemId: Number(route.params.id) })
    ElMessage.success('下单成功！')
    await loadItem()
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '下单失败'
    ElMessage.error(errMsg)
  } finally {
    ordering.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadMe(), reloadByRouteParam()])
})

watch(() => route.params.id, async (nextId, prevId) => {
  if (!nextId || nextId === prevId) return
  await reloadByRouteParam()
})
</script>

<template>
  <div class="page">
    <el-skeleton v-if="loading" :rows="10" animated />

    <div v-else-if="item" class="layout">
      <div class="left">
        <el-card class="mediaCard" shadow="hover">
          <div class="mediaBox">
            <el-image
              v-if="item.coverImageUrl"
              :src="item.coverImageUrl"
              fit="contain"
              :preview-src-list="[item.coverImageUrl]"
              lazy
            />
            <el-empty v-else description="暂无图片" :image-size="100" />
          </div>
        </el-card>
      </div>

      <div class="right">
        <el-card class="infoCard" shadow="hover">
          <div class="head">
            <h1 class="title">{{ item.title }}</h1>
            <div class="price">
              <span class="priceSymbol">¥</span>{{ item.price }}
            </div>
          </div>

          <div class="meta">
            <el-tag size="small" type="info">{{ formatCategoryLabel(item.category) }}</el-tag>
            <el-tag size="small" type="success">成色 {{ formatConditionLabel(item.conditionLevel) }}</el-tag>
            <el-tag size="small" type="warning">{{ item.status }}</el-tag>
          </div>

          <el-divider />

          <div class="desc">
            <h3 class="descTitle">
              <el-icon><Document /></el-icon>
              商品描述
            </h3>
            <p>{{ item.description || '（暂无描述）' }}</p>
          </div>

          <el-divider />

          <div class="actions">
            <el-button
              type="primary"
              size="large"
              :disabled="!getToken() || ordering || item.status !== 'ON_SALE'"
              :loading="ordering"
              @click="orderCreate"
            >
              <el-icon><ShoppingCart /></el-icon>
              立即下单
            </el-button>

            <div class="hints">
              <el-alert
                v-if="!getToken()"
                title="提示：游客可浏览详情，登录后才能下单"
                type="warning"
                :closable="false"
                show-icon
                size="small"
              />
              <el-alert
                v-else-if="item.status !== 'ON_SALE'"
                title="提示：商品正在审核或暂不可购买"
                type="info"
                :closable="false"
                show-icon
                size="small"
              />
              <el-alert
                v-else
                title="提示：下单后商品会被锁定，卖家发货后买家确认收货完成交易"
                type="info"
                :closable="false"
                show-icon
                size="small"
              />
            </div>
          </div>
        </el-card>

        <el-card class="messageCard" shadow="hover" style="margin-top: 14px;">
          <template #header>
            <div class="cardHeader">
              <el-icon><ChatDotRound /></el-icon>
              <span>商品留言</span>
            </div>
          </template>

          <div v-if="msgLoading" class="msgLoading">
            <el-skeleton :rows="3" animated />
          </div>

          <div v-else class="msgs">
            <div v-for="m in messages" :key="m.id" class="msg">
              <div class="msgHead">
                <el-avatar :size="32">
                  <img v-if="m.avatarUrl" :src="m.avatarUrl" alt="" />
                  <template v-else>
                    <el-icon><User /></el-icon>
                  </template>
                </el-avatar>
                <div class="who">{{ m.userName || ('用户' + (m.fromUserId || '')) }}</div>
                <div class="msgTime">{{ m.createdAt || '' }}</div>
              </div>
              <div class="msgContent">{{ m.content }}</div>
            </div>

            <el-empty v-if="messages.length === 0" description="暂无留言" :image-size="60" />
          </div>

          <div class="composer">
            <el-input
              v-model="content"
              type="textarea"
              :rows="2"
              placeholder="说点什么..."
              resize="none"
            />
            <el-button
              type="primary"
              :disabled="!getToken() || posting"
              :loading="posting"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>

          <el-alert
            v-if="!getToken()"
            title="游客可查看留言，登录后才能参与留言"
            type="warning"
            :closable="false"
            show-icon
            size="small"
            style="margin-top: 10px;"
          />
        </el-card>
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

.mediaCard,
.infoCard,
.messageCard {
  border-radius: 16px;
}

.mediaBox {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.title {
  margin: 0;
  font-size: 26px;
  line-height: 1.3;
}

.price {
  font-size: 30px;
  font-weight: 900;
  color: #dc2626;
  white-space: nowrap;
}

.priceSymbol {
  font-size: 18px;
  margin-right: 2px;
}

.meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.descTitle {
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
}

.desc p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msgs {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #f8fafc;
}

.msgHead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.who {
  font-weight: 700;
}

.msgTime {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
}

.msgContent {
  color: #334155;
  line-height: 1.6;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin-top: 14px;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .mediaBox {
    min-height: 300px;
  }
}

@media (max-width: 640px) {
  .head,
  .composer {
    grid-template-columns: 1fr;
    display: grid;
  }

  .price {
    font-size: 24px;
  }
}
</style>
