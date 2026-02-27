<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getActiveOrders, updateItemStatus } from '@/api/order'

const orders = ref<any[]>([])
const loading = ref(false)
const wsConnected = ref(false)
let ws: WebSocket | null = null

// 排序选项
const sortBy = ref('time') // 'time' | 'table' | 'status'
const sortOrder = ref('asc') // 'asc' | 'desc'

// WebSocket连接
const connectWebSocket = () => {
  const token = localStorage.getItem('token')
  const wsUrl = `ws://localhost:8080/ws/kitchen?token=${token}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    wsConnected.value = true
    ElMessage.success('已连接到厨房系统')
  }

  ws.onmessage = event => {
    const message = JSON.parse(event.data)
    handleWebSocketMessage(message)
  }

  ws.onclose = () => {
    wsConnected.value = false
    setTimeout(() => {
      if (!wsConnected.value) {
        connectWebSocket()
      }
    }, 5000)
  }

  ws.onerror = () => {
    wsConnected.value = false
  }
}

const handleWebSocketMessage = (message: any) => {
  switch (message.type) {
    case 'NEW_ORDER':
      ElMessage.success('收到新订单！')
      loadOrders()
      playNotificationSound()
      break
    case 'ORDER_STATUS':
    case 'ITEM_STATUS':
      loadOrders()
      break
  }
}

const playNotificationSound = () => {
  const audio = new Audio(
    'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBTGH0fPTgjMGHm7A7+OZSA0PVanu8blqFgUuh9Dz2YU2Bhxqv+zplkcODVGm5O+4ZSAEMYrO89GFNwYdcfDr4pVFDA1Pp+XysWUeBjiS1/LNfi0GI33R8tOENAcdcO/r4ZdJDQtPp+TwxWUhBjqT1/PQfS4GI3/R8tSFNwYdcfDr4plHDAtQp+TwxmUgBDeOzvPVhjYGHG3A7uSaSQ0MTKjl8sZmIAU2jc7z1YU1Bhxwv+zmmUgNC1Gn5O/EZSAFNo/M89CEMwYccPDs4ppIDQtRp+TvvWUfBTiOz/PShjUGG3Dw7OKbSA0LUqjl8b1oHwU3jM3z0oU1Bxtw8OzhmUgNC1Ko5fG+ZyAFN4vM89CEMwYccO/t4plHDAtRqOXyxWUfBTiKzvPVhjYGHG3A7eSaSQ0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyAFN4rO89CEMwYccPDs4ppIDQtRp+TvvWUfBTiOz/PShjUGG3Dw7OKbSA0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyAFN4rO89CEMwYccPDs4ppIDQtRp+TvvWUfBTiOz/PShjUGG3Dw7OKbSA0LUqjl8b1nHwU3is7z1YU1Bxtw8OzhmUgNC1Ko5fG/ZyA=='
  )
  audio.play().catch(() => {})
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getActiveOrders()
    orders.value = res
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

// 排序后的订单
const sortedOrders = computed(() => {
  const sorted = [...orders.value]

  sorted.sort((a, b) => {
    let comparison = 0

    switch (sortBy.value) {
      case 'time':
        comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        break
      case 'table':
        comparison = a.tableNo.localeCompare(b.tableNo)
        break
      case 'status':
        comparison = a.status - b.status
        break
    }

    return sortOrder.value === 'asc' ? comparison : -comparison
  })

  return sorted
})

// 统计
const stats = computed(() => {
  const total = orders.value.length
  const pending = orders.value.filter(o => o.status === 0).length
  const cooking = orders.value.filter(o => o.status === 1).length
  const done = orders.value.filter(o => o.status === 2).length
  return { total, pending, cooking, done }
})

const handleStartCooking = async (item: any) => {
  try {
    await updateItemStatus(item.id, 1)
    ElMessage.success('开始制作')
    loadOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleFinishCooking = async (item: any) => {
  try {
    await updateItemStatus(item.id, 2)
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

const toggleSort = (field: string) => {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortOrder.value = 'asc'
  }
}

// 自动刷新
let refreshTimer: number | null = null

onMounted(() => {
  loadOrders()
  connectWebSocket()
  refreshTimer = window.setInterval(loadOrders, 30000)
})

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div v-loading="loading" class="kitchen-page">
    <!-- 顶部统计栏 -->
    <div class="stats-bar">
      <div class="stat-item">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">总订单</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value text-info">{{ stats.pending }}</div>
        <div class="stat-label">待制作</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value text-warning">{{ stats.cooking }}</div>
        <div class="stat-label">制作中</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value text-success">{{ stats.done }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <!-- 主标题和控制栏 -->
    <div class="header">
      <h1>🍳 后厨订单管理</h1>

      <div class="controls">
        <!-- 排序按钮 -->
        <div class="sort-buttons">
          <span class="sort-label">排序:</span>
          <el-button
            :type="sortBy === 'time' ? 'primary' : 'default'"
            size="small"
            @click="toggleSort('time')"
          >
            时间 {{ sortBy === 'time' ? (sortOrder === 'asc' ? '↑' : '↓') : '' }}
          </el-button>
          <el-button
            :type="sortBy === 'table' ? 'primary' : 'default'"
            size="small"
            @click="toggleSort('table')"
          >
            桌号 {{ sortBy === 'table' ? (sortOrder === 'asc' ? '↑' : '↓') : '' }}
          </el-button>
          <el-button
            :type="sortBy === 'status' ? 'primary' : 'default'"
            size="small"
            @click="toggleSort('status')"
          >
            状态 {{ sortBy === 'status' ? (sortOrder === 'asc' ? '↑' : '↓') : '' }}
          </el-button>
        </div>

        <div class="connection-status">
          <el-tag :type="wsConnected ? 'success' : 'danger'" size="large">
            {{ wsConnected ? '🟢 已连接' : '🔴 未连接' }}
          </el-tag>
          <el-button icon="Refresh" @click="loadOrders">刷新</el-button>
        </div>
      </div>
    </div>

    <div v-if="sortedOrders.length === 0" class="empty-state">
      <el-empty description="暂无订单">
        <template #image>
          <el-icon :size="60" color="#909399"><Coffee /></el-icon>
        </template>
      </el-empty>
    </div>

    <div v-else class="orders-list">
      <el-card
        v-for="order in sortedOrders"
        :key="order.id"
        class="order-card"
        :class="{ urgent: order.status === 1, new: order.status === 0 }"
      >
        <template #header>
          <div class="order-header">
            <div class="order-info">
              <span class="table-no">{{ order.tableNo }}号桌</span>
              <span class="order-no">{{ order.orderNo?.slice(-6) }}</span>
              <span class="time">{{ new Date(order.createdAt).toLocaleTimeString() }}</span>
            </div>
            <el-tag :type="getStatusType(order.status)" size="large" effect="dark">
              {{ getStatusLabel(order.status) }}
            </el-tag>
          </div>
        </template>

        <div class="items-list">
          <div
            v-for="item in order.items"
            :key="item.id"
            class="item-row"
            :class="{ finished: item.status === 2, cooking: item.status === 1 }"
          >
            <div class="item-main">
              <div class="item-info">
                <span class="item-name">{{ item.dishName }}</span>
                <span class="item-quantity">x{{ item.quantity }}</span>
              </div>
              <span v-if="item.remark" class="item-remark">{{ item.remark }}</span>
            </div>

            <div class="item-actions">
              <el-tag :type="getStatusType(item.status)" size="small" class="status-tag">
                {{ getStatusLabel(item.status) }}
              </el-tag>

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
              <el-icon v-if="item.status === 2" class="check-icon" color="#67c23a" :size="20"
                ><Check
              /></el-icon>
            </div>
          </div>
        </div>

        <div v-if="order.remark" class="order-remark">
          <span class="remark-label">备注:</span> {{ order.remark }}
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.kitchen-page {
  padding: 20px;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 30px;
  margin-bottom: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 5px;
}

.text-info {
  color: #409eff;
}

.text-warning {
  color: #e6a23c;
}

.text-success {
  color: #67c23a;
}

.stat-label {
  font-size: 14px;
  color: #a0a0a0;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
}

/* 标题栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
}

.header h1 {
  font-size: 32px;
  margin: 0;
  text-shadow: 0 2px 10px rgba(233, 69, 96, 0.5);
}

.controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sort-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  color: #a0a0a0;
  font-size: 14px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* 订单列表 */
.orders-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 20px;
}

.order-card {
  background: rgba(22, 33, 62, 0.8);
  border: 2px solid transparent;
  color: #fff;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
}

.order-card:hover {
  border-color: #e94560;
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(233, 69, 96, 0.3);
}

.order-card.new {
  border-color: #409eff;
  animation: glow-blue 2s infinite;
}

.order-card.urgent {
  border-color: #e94560;
  animation: glow-red 2s infinite;
}

@keyframes glow-blue {
  0%,
  100% {
    box-shadow: 0 0 5px rgba(64, 158, 255, 0.5);
  }
  50% {
    box-shadow: 0 0 20px rgba(64, 158, 255, 0.8);
  }
}

@keyframes glow-red {
  0%,
  100% {
    box-shadow: 0 0 5px rgba(233, 69, 96, 0.5);
  }
  50% {
    box-shadow: 0 0 20px rgba(233, 69, 96, 0.8);
  }
}

.order-card :deep(.el-card__header) {
  background: rgba(15, 52, 96, 0.8);
  border-bottom: 2px solid rgba(233, 69, 96, 0.5);
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
  text-shadow: 0 2px 5px rgba(233, 69, 96, 0.5);
}

.order-no {
  font-size: 14px;
  color: #a0a0a0;
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.time {
  font-size: 14px;
  color: #a0a0a0;
}

/* 菜品列表 */
.items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  transition: all 0.3s;
  border-left: 4px solid transparent;
}

.item-row:hover {
  background: rgba(255, 255, 255, 0.1);
}

.item-row.cooking {
  border-left-color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
}

.item-row.finished {
  border-left-color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
  opacity: 0.7;
}

.item-main {
  flex: 1;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 5px;
}

.item-name {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.item-quantity {
  font-size: 20px;
  color: #e94560;
  font-weight: bold;
}

.item-remark {
  font-size: 14px;
  color: #ffd700;
  background: rgba(255, 215, 0, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-tag {
  min-width: 70px;
  text-align: center;
}

.check-icon {
  margin-left: 10px;
}

/* 订单备注 */
.order-remark {
  margin-top: 15px;
  padding: 10px;
  background: rgba(255, 215, 0, 0.1);
  border-radius: 8px;
  border-left: 3px solid #ffd700;
}

.remark-label {
  color: #ffd700;
  font-weight: bold;
}
</style>
