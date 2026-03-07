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
  <div class="page ordersPage">
    <section class="hero surfacePanel">
      <div>
        <div class="heroTag">MY ORDERS</div>
        <h1 class="title">我的订单</h1>
        <div class="sub">统一查看买到的与卖出的订单状态，快速完成付款、发货、确认收货等操作。</div>
      </div>
      <div class="heroStats">
        <div class="statChip">买到 {{ buyOrders.length }} 单</div>
        <div class="statChip">卖出 {{ sellOrders.length }} 单</div>
      </div>
    </section>

    <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
    <div v-if="loading" class="stateCard">加载中...</div>

    <div v-else class="cols">
      <section class="col cardPanel">
        <div class="panelHead">
          <div>
            <h2 class="panelTitle">我买到的</h2>
            <div class="panelSub">跟进付款、收货与取消操作</div>
          </div>
          <div class="countBadge">{{ buyOrders.length }}</div>
        </div>

        <div v-for="o in buyOrders" :key="o.id" class="card">
          <div class="metaGrid">
            <div class="metaItem"><span>订单ID</span><strong>#{{ o.id }}</strong></div>
            <div class="metaItem"><span>商品ID</span><strong>#{{ o.itemId }}</strong></div>
            <div class="metaItem"><span>金额</span><strong>￥{{ o.amount }}</strong></div>
            <div class="metaItem"><span>状态</span><strong>{{ o.status }}</strong></div>
          </div>

          <div class="actions">
            <button class="btn" type="button" @click="act(() => payOrder(o.id))">付款</button>
            <button class="btn" type="button" @click="act(() => confirmOrder(o.id))">确认收货</button>
            <button class="btn danger" type="button" @click="act(() => cancelOrder(o.id))">取消</button>
          </div>
        </div>

        <div v-if="buyOrders.length === 0" class="empty">暂无买入订单</div>
      </section>

      <section class="col cardPanel">
        <div class="panelHead">
          <div>
            <h2 class="panelTitle">我卖出的</h2>
            <div class="panelSub">集中处理发货状态</div>
          </div>
          <div class="countBadge">{{ sellOrders.length }}</div>
        </div>

        <div v-for="o in sellOrders" :key="o.id" class="card">
          <div class="metaGrid">
            <div class="metaItem"><span>订单ID</span><strong>#{{ o.id }}</strong></div>
            <div class="metaItem"><span>商品ID</span><strong>#{{ o.itemId }}</strong></div>
            <div class="metaItem"><span>金额</span><strong>￥{{ o.amount }}</strong></div>
            <div class="metaItem"><span>状态</span><strong>{{ o.status }}</strong></div>
          </div>

          <div class="actions">
            <button class="btn" type="button" @click="act(() => shipOrder(o.id))">发货</button>
          </div>
        </div>

        <div v-if="sellOrders.length === 0" class="empty">暂无卖出订单</div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.ordersPage {
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
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 28%),
    linear-gradient(135deg, rgba(124, 58, 237, 0.16), rgba(59, 130, 246, 0.16));
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
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.18);
  font-size: 12px;
  font-weight: 700;
}

.title {
  margin: 14px 0 0;
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

.heroStats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.statChip {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.cardPanel {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.panelHead {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panelTitle {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.panelSub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}

.countBadge {
  min-width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 18px;
  padding: 16px;
  margin-bottom: 12px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.96));
}

.metaGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metaItem {
  padding: 12px;
  border-radius: 14px;
  background: rgba(241, 245, 249, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.metaItem span {
  display: block;
  font-size: 12px;
  color: var(--muted);
}

.metaItem strong {
  display: block;
  margin-top: 6px;
  font-size: 15px;
  color: #0f172a;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.btn {
  min-height: 40px;
  background: linear-gradient(135deg, var(--primary), #6366f1);
  color: #fff;
  border: 0;
  padding: 0 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.15s ease, filter 0.15s ease;
  font-weight: 700;
}

.btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
}

.btn.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.empty,
.stateCard {
  padding: 18px;
  border-radius: 18px;
  color: var(--muted);
  font-size: 13px;
  text-align: center;
  border: 1px dashed rgba(148, 163, 184, 0.24);
  background: rgba(248, 250, 252, 0.88);
}

.error {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.18);
  color: var(--danger);
  font-size: 13px;
}

@media (max-width: 960px) {
  .cols {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero,
  .cardPanel,
  .card {
    border-radius: 18px;
  }

  .title {
    font-size: 24px;
  }

  .metaGrid {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
