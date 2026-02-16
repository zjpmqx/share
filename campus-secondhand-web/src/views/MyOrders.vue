<script setup>
import { onMounted, ref } from 'vue'
import { cancelOrder, confirmOrder, myBuyOrders, mySellOrders, payOrder, shipOrder } from '../services/api'

const loading = ref(false)
const buyOrders = ref([])
const sellOrders = ref([])
const errorMsg = ref('')

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [b, s] = await Promise.all([myBuyOrders(), mySellOrders()])
    buyOrders.value = b.data || []
    sellOrders.value = s.data || []
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function act(fn) {
  try {
    await fn()
    await load()
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e?.message || '操作失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <h1 class="title">我的订单</h1>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="loading">加载中...</div>

    <div v-else class="cols">
      <div class="col">
        <h2 class="sub">我买到的</h2>
        <div v-for="o in buyOrders" :key="o.id" class="card">
          <div class="row"><span>订单ID：</span>{{ o.id }}</div>
          <div class="row"><span>商品ID：</span>{{ o.itemId }}</div>
          <div class="row"><span>金额：</span>￥{{ o.amount }}</div>
          <div class="row"><span>状态：</span>{{ o.status }}</div>

          <div class="actions">
            <button class="btn" type="button" @click="act(() => payOrder(o.id))">付款</button>
            <button class="btn" type="button" @click="act(() => confirmOrder(o.id))">确认收货</button>
            <button class="btn danger" type="button" @click="act(() => cancelOrder(o.id))">取消</button>
          </div>
        </div>
        <div v-if="buyOrders.length === 0" class="empty">暂无</div>
      </div>

      <div class="col">
        <h2 class="sub">我卖出的</h2>
        <div v-for="o in sellOrders" :key="o.id" class="card">
          <div class="row"><span>订单ID：</span>{{ o.id }}</div>
          <div class="row"><span>商品ID：</span>{{ o.itemId }}</div>
          <div class="row"><span>金额：</span>￥{{ o.amount }}</div>
          <div class="row"><span>状态：</span>{{ o.status }}</div>

          <div class="actions">
            <button class="btn" type="button" @click="act(() => shipOrder(o.id))">发货</button>
          </div>
        </div>
        <div v-if="sellOrders.length === 0" class="empty">暂无</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.title {
  margin: 10px 0 12px;
  font-size: 22px;
  font-weight: 800;
}

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 900px) {
  .cols {
    grid-template-columns: 1fr;
  }
}

.sub {
  margin: 0 0 10px;
  font-size: 16px;
}

.card {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-sm);
}

.row {
  font-size: 13px;
  color: #374151;
  margin-bottom: 6px;
}

.row span {
  color: var(--muted);
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.btn {
  background: var(--primary);
  color: #fff;
  border: 0;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.btn:hover {
  background: var(--primary-hover);
  transform: translateY(-1px);
}

.btn.danger {
  background: var(--danger);
}

.btn.danger:hover {
  background: #991b1b;
}

.empty {
  color: var(--muted);
  font-size: 13px;
}

.error {
  margin: 8px 0;
  color: var(--danger);
  font-size: 13px;
}
</style>
