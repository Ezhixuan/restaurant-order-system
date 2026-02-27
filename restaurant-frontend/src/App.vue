<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MobileLayout from './layouts/MobileLayout.vue'
import PadLayout from './layouts/PadLayout.vue'
import AdminLayout from './layouts/AdminLayout.vue'

const route = useRoute()

const layout = computed(() => {
  if (route.path.startsWith('/m')) return MobileLayout
  if (route.path.startsWith('/pad')) return PadLayout
  return AdminLayout
})
</script>

<template>
  <component :is="layout" />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}
</style>
