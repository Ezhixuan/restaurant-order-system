<script setup lang="ts">
interface Props {
  description?: string
}

withDefaults(defineProps<Props>(), {
  description: '暂无数据',
})
</script>

<template>
  <div class="empty-container">
    <el-empty :description="description" />
  </div>
</template>

<style scoped>
.empty-container {
  padding: 40px 0;
}
</style>
