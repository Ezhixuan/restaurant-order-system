<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="pad-layout">
    <RouterView />
  </div>
</template>

<style scoped>
.pad-layout {
  min-height: 100vh;
  background: #f0f2f5;
}
</style>
