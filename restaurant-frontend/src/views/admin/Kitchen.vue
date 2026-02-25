<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getActiveOrders, updateItemStatus, completeOrder } from '@/api/order'

const orders = ref<any[]>([])
const loading = ref(false)
const wsConnected = ref(false)
let ws: WebSocket | null = null

// WebSocket连接
const connectWebSocket = () => {
  const token = localStorage.getItem('token')
  const wsUrl = `ws://localhost:8080/ws/kitchen?token=${token}`
  
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    wsConnected.value = true
    ElMessage.success('已连接到厨房系统')
  }
  
  ws.onmessage = (event) => {
    const message = JSON.parse(event.data)
    handleWebSocketMessage(message)
  }
  
  ws.onclose = () => {
    wsConnected.value = false
    // 5秒后重连
    setTimeout(() => {
      if (!wsConnected.value) {
        connectWebSocket()
      }
    }, 5000)
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
    wsConnected.value = false
  }
}

const handleWebSocketMessage = (message: any) => {
  switch (message.type) {
    case 'NEW_ORDER':
      ElMessage.success('收到新订单！')
      loadOrders()
      // 播放提示音
      playNotificationSound()
      break
    case 'ORDER_STATUS':
      loadOrders()
      break
    case 'ITEM_STATUS':
      loadOrders()
      break
  }
}

const playNotificationSound = () => {
  // 创建简单的提示音
  const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBTGH0fPTgjMGHm7A7+OZSA0PVanu8blqFgUuh9Dz2YU2Bhxqv+zplkcODVGm5O+4ZSAEMYrO89GFNwYdcfDr4pVFDA1Pp+XysWUeBjiS1/LNfi0GI33R8tOENAcdcO/r4ZdJDQtPp+TwxWUhBjqT1/PQfS4GI3/R8tSFNwYdcfDr4plHDAtQp+TwxmUgBDeOzvPVhjYGHG3A7uSaSQ0MTKjl8sZmIAU2jc7z1YU1Bhxwv+zmmUgNC1Gn5O/EZSAFNo/M89CEMwYccPDs4ppIDQtRp+TvvWUfBTiOz/PShjUGG3Dw7OKbSA0LUqjl8b1oHwU3jM3z0oU1Bxtw8OzhmUgNC1Ko5fG+ZyAFN4vM89CEMwYccO/t4plHDAtRqOXyxWUfBTiKzvPVhjYGHG3A7eSaSQ0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyAFN4rO89CEMwYccPDs4plHDAtRqOXyxWUfBTiKzvPVhjYGHG3A7eSaSQ0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyAFN4rO89CEMwYccPDs4plHDAtRqOXyxWUfBTiKzvPVhjYGHG3A7eSaSQ0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyAFN4rO89CEMwYccPDs4plHDAtRqOXyxWUfBTiKzvPVhjYGHG3A7eSaSQ0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyA==')
  audio.play().catch(() => {
    // 忽略自动播放限制错误
  })
}

const loadOrders = async () => {
  loading.value = true
  try {
    orders.value = await getActiveOrders()
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const handleStartCooking = async (item: any) => {
  try {
    await updateItemStatus(item.id, 1) // 1=制作中
    ElMessage.success('开始制作')
    loadOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleFinishCooking = async (item: any) => {
  try {
    await updateItemStatus(item.id, 2) // 2=已完成
    ElMessage.success('制作完成')
    loadOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

onMounted(() => {
  loadOrders()
  connectWebSocket()
})

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
})
</script>

<template>
  <div class="kitchen-page" v-loading="loading">
    <div class="header">
      <h1>🍳 后厨订单管理</h1>
      <div class="connection-status">
        <el-tag :type="wsConnected ? 'success' : 'danger'">
          {{ wsConnected ? '🟢 已连接' : '🔴 未连接' }}
        </el-tag>
        <el-button @click="loadOrders" icon="Refresh">刷新</el-button>
      </div>
    </div>

    <div v-if="orders.length === 0" class="empty-state">
      <el-empty description="暂无订单" />
    </div>

    <div v-else class="orders-list">
      <el-card
        v-for="order in orders"
        :key="order.id"
        class="order-card"
        :class="{ 'urgent': order.status === 1 }"
      >
        <template #header>
          <div class="order-header">
            <div class="order-info">
              <span class="table-no">{{ order.tableNo }}号桌</span>
              <span class="order-no">订单: {{ order.orderNo?.slice(-6) }}</span>
              <span class="time">{{ new Date(order.createdAt).toLocaleTimeString() }}</span>
            </div>
            <el-tag :type="getStatusType(order.status)" size="large">
              {{ getStatusLabel(order.status) }}
            </el-tag>
          </div>
        </template>

        <div class="items-list">
          <div
            v-for="item in order.items"
            :key="item.id"
            class="item-row"
            :class="{ 'finished': item.status === 2 }"
          >
            <div class="item-info">
              <span class="item-name">{{ item.dishName }}</span>
              <span class="item-quantity">x{{ item.quantity }}</span>
              <span v-if="item.remark" class="item-remark">({{ item.remark }})</span>
            </div>
            
            <div class="item-status">
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusLabel(item.status) }}
              </el-tag>
            </div>
            
            <div class="item-actions">
              <el-button
                v-if="item.status === 0"
                type="primary"
                size="small"
                @click="handleStartCooking(item)"
              >
                开始制作
              </el-button>
              <el-button
                v-if="item.status === 1"
                type="success"
                size="small"
                @click="handleFinishCooking(item)"
              >
                完成
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.kitchen-page {
  padding: 20px;
  min-height: 100vh;
  background: #1a1a2e;
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #16213e;
}

.header h1 {
  font-size: 32px;
  margin: 0;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 15px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.orders-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.order-card {
  background: #16213e;
  border: none;
  color: #fff;
}

.order-card :deep(.el-card__header) {
  background: #0f3460;
  border-bottom: 2px solid #e94560;
}

.order-card.urgent :deep(.el-card__header) {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { background: #0f3460; }
  50% { background: #e94560; }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.table-no {
  font-size: 24px;
  font-weight: bold;
  color: #e94560;
}

.order-no {
  font-size: 14px;
  color: #a0a0a0;
}

.time {
  font-size: 14px;
  color: #a0a0a0;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #0f3460;
  border-radius: 8px;
  transition: all 0.3s;
}

.item-row.finished {
  opacity: 0.5;
  background: #1a1a2e;
}

.item-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-name {
  font-size: 18px;
  font-weight: bold;
}

.item-quantity {
  font-size: 20px;
  color: #e94560;
}

.item-remark {
  font-size: 14px;
  color: #ffd700;
}

.item-status {
  margin: 0 15px;
}

.item-actions {
  display: flex;
  gap: 10px;
}
</style>
