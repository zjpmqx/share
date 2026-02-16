<script setup>
import NavBar from '../components/NavBar.vue'
</script>

<template>
  <div class="app">
    <NavBar />
    <main class="main">
      <div class="container">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background: var(--bg);
  color: var(--text);
}

.main {
  padding: 20px 0 40px;
}
</style>
