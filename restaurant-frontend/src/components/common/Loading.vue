<script setup lang="ts">
interface Props {
  visible: boolean
  text?: string
}

withDefaults(defineProps<Props>(), {
  text: '加载中...',
})
</script>

<template>
  <div v-if="visible" class="loading-mask">
    <div class="loading-content">
      <el-icon class="loading-icon" size="32"><Loading /></el-icon>
      <p class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<style scoped>
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-content {
  text-align: center;
}

.loading-icon {
  animation: rotate 1s linear infinite;
  color: var(--el-color-primary);
}

.loading-text {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
