<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="mobile-layout">
    <RouterView />
  </div>
</template>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  background: #f5f5f5;
}
</style>
