<script setup>
import NavBar from '../components/NavBar.vue'
</script>

<template>
  <div class="app-shell user-shell">
    <div class="shellGrid"></div>
    <div class="shellSpark sparkA"></div>
    <div class="shellSpark sparkB"></div>

    <NavBar />
    <main class="app-main">
      <div class="page-shell">
        <div class="layoutTopbar">
          <div class="layoutBadge">同校闲置交换站</div>
          <div class="layoutNote">理性买卖，轻松逛逛，偶尔收获一点捡漏快乐</div>
        </div>

        <div class="page-frame">
          <RouterView v-slot="{ Component, route }">
            <Transition name="content-fade" mode="out-in">
              <component :is="Component" :key="route.fullPath" />
            </Transition>
          </RouterView>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page-shell {
  position: relative;
}

.user-shell {
  position: relative;
  overflow: visible;
}

.user-shell::before,
.user-shell::after {
  content: '';
  position: fixed;
  inset: auto;
  pointer-events: none;
  border-radius: 999px;
  filter: blur(84px);
  opacity: 0.6;
  z-index: 0;
}

.user-shell::before {
  width: 260px;
  height: 260px;
  top: 96px;
  left: -86px;
  background: rgba(96, 165, 250, 0.18);
}

.user-shell::after {
  width: 320px;
  height: 320px;
  right: -112px;
  bottom: 42px;
  background: rgba(99, 102, 241, 0.12);
}

.shellGrid {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.5;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: linear-gradient(180deg, rgba(15, 23, 42, 0.4), transparent 78%);
}

.shellSpark {
  position: fixed;
  z-index: 0;
  pointer-events: none;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    0 0 0 8px rgba(255, 255, 255, 0.08),
    0 0 24px rgba(59, 130, 246, 0.16);
  transform-origin: center;
}

.sparkA {
  top: 154px;
  right: 18%;
}

.sparkB {
  bottom: 24%;
  left: 12%;
  width: 10px;
  height: 10px;
}

.app-main {
  position: relative;
  z-index: 1;
  padding-top: 144px;
}

.layoutTopbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin: 6px 0 14px;
  padding: 0 2px;
}

.layoutBadge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.layoutNote {
  color: var(--muted);
  font-size: 13px;
  text-align: right;
}

.page-frame {
  min-height: calc(100vh - 288px);
}

@media (max-width: 860px) {
  .app-main {
    padding-top: 124px;
  }

  .layoutTopbar {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .layoutNote {
    text-align: left;
  }

  .page-frame {
    min-height: calc(100vh - 250px);
  }
}

@media (max-width: 640px) {
  .shellGrid,
  .shellSpark {
    display: none;
  }

  .app-main {
    padding-top: 112px;
  }

  .page-frame {
    min-height: calc(100vh - 228px);
  }
}
</style>
