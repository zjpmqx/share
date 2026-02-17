<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { cancelOrder, confirmOrder, createOrder, getItem, listMessages, me, payOrder, postMessage, shipOrder } from '../services/api'
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
  try {
    const resp = await getItem(route.params.id)
    item.value = resp.data
  } catch (e) {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  msgLoading.value = true
  try {
    const resp = await listMessages(route.params.id)
    messages.value = resp.data || []
  } catch {
  } finally {
    msgLoading.value = false
  }
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
  await Promise.all([loadMe(), loadItem(), loadMessages()])
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
            <el-tag size="small" type="info">{{ item.category }}</el-tag>
            <el-tag size="small" type="success">成色 {{ item.conditionLevel }}</el-tag>
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
                title="提示：请先登录后下单"
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
            title="登录后才能留言"
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

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.mediaCard {
  overflow: hidden;
}

.mediaBox {
  width: 100%;
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(99, 102, 241, 0.06));
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

.mediaBox :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  font-size: 22px;
  margin: 0;
  font-weight: 700;
  flex: 1;
  min-width: 200px;
}

.price {
  font-size: 28px;
  font-weight: 800;
  color: #ef4444;
  white-space: nowrap;
}

.priceSymbol {
  font-size: 18px;
}

.meta {
  margin: 12px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.descTitle {
  font-size: 14px;
  color: var(--muted);
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.desc p {
  margin: 0;
  line-height: 1.7;
  color: var(--text);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hints {
  width: 100%;
}

.cardHeader {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.msgLoading {
  padding: 10px 0;
}

.msgs {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.msg {
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  border-radius: 12px;
  padding: 12px;
}

.msgHead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.who {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.msgTime {
  font-size: 12px;
  color: var(--muted-2);
  margin-left: auto;
}

.msgContent {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
}

.composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

@media (max-width: 520px) {
  .composer {
    flex-direction: column;
  }
  
  .composer .el-button {
    width: 100%;
  }
}

.composer :deep(.el-textarea) {
  flex: 1;
}
</style>
