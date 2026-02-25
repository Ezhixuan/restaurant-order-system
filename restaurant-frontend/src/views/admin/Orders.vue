<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrders, getOrderDetail, payOrder, completeOrder, cancelOrder, updateItemStatus } from '@/api/order'
import { getTodayStats, getTopDishes, getTableStats } from '@/api/report'

// 统计数据
const todayStats = ref({
  totalRevenue: 0,
  orderCount: 0,
  avgAmount: 0
})

const topDishes = ref<any[]>([])
const tableStats = ref<any[]>([])

// 订单列表
const orders = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref(null as number | null)

// 订单详情弹窗
const detailVisible = ref(false)
const currentOrder = ref<any>(null)
const currentItems = ref<any[]>([])

const statusOptions = [
  { label: '全部', value: null },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '制作中', value: 2 },
  { label: '待上菜', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 }
]

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'primary',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '制作中',
    3: '待上菜',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

const getItemStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

const getItemStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

const loadStats = async () => {
  try {
    todayStats.value = await getTodayStats()
    topDishes.value = await getTopDishes(5)
    tableStats.value = await getTableStats()
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadOrders = async () => {
  loading.value = true
  try {
    orders.value = await getOrders(statusFilter.value)
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const showDetail = async (order: any) => {
  try {
    const detail = await getOrderDetail(order.id)
    currentOrder.value = detail.order
    currentItems.value = detail.items
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('加载订单详情失败')
  }
}

const handlePay = async (order: any) => {
  try {
    await ElMessageBox.confirm(`确认订单 ${order.orderNo} 已收款？`, '确认收款', {
      type: 'warning'
    })
    await payOrder(order.id, { payType: 3, amount: order.payAmount }) // 3=现金
    ElMessage.success('收款成功')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleComplete = async (order: any) => {
  try {
    await ElMessageBox.confirm(`确认完成订单 ${order.orderNo}？`, '确认完成', {
      type: 'warning'
    })
    await completeOrder(order.id)
    ElMessage.success('订单已完成')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleCancel = async (order: any) => {
  try {
    await ElMessageBox.confirm(`确认取消订单 ${order.orderNo}？`, '确认取消', {
      type: 'danger'
    })
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleUpdateItemStatus = async (item: any, status: number) => {
  try {
    await updateItemStatus(item.id, status)
    ElMessage.success('状态更新成功')
    // 刷新详情
    if (currentOrder.value) {
      const detail = await getOrderDetail(currentOrder.value.id)
      currentOrder.value = detail.order
      currentItems.value = detail.items
    }
    loadOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const filteredOrders = computed(() => {
  if (statusFilter.value === null) return orders.value
  return orders.value.filter(o => o.status === statusFilter.value)
})

onMounted(() => {
  loadStats()
  loadOrders()
})
</script>

<template>
  <div class="orders-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card>
          <div class="stat-title">今日营业额</div>
          <div class="stat-value text-success">¥{{ todayStats.totalRevenue?.toFixed(2) || '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">今日订单</div>
          <div class="stat-value text-primary">{{ todayStats.orderCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">平均客单价</div>
          <div class="stat-value text-warning">¥{{ todayStats.avgAmount?.toFixed(2) || '0.00' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订单列表 -->
    <el-card class="orders-card">
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="loadOrders">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value ?? 'all'"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
      </template>

      <el-table :data="filteredOrders" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="tableNo" label="桌号" width="100" />
        <el-table-column prop="customerCount" label="人数" width="80" />
        <el-table-column prop="totalAmount" label="金额" width="120">
          <template #default="{ row }">
            <div>总价: ¥{{ row.totalAmount?.toFixed(2) }}</div>
            <div v-if="row.discountAmount > 0" class="discount">优惠: ¥{{ row.discountAmount?.toFixed(2) }}</div>
            <div class="pay-amount">实付: ¥{{ row.payAmount?.toFixed(2) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="handlePay(row)"
            >
              收款
            </el-button>
            <el-button
              v-if="row.status === 3"
              size="small"
              type="primary"
              @click="handleComplete(row)"
            >
              完成
            </el-button>
            <el-button
              v-if="row.status < 2"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <div v-if="currentOrder" class="order-detail">
        <div class="detail-header">
          <div>
            <span class="label">订单号:</span> {{ currentOrder.orderNo }}
          </div>
          <div>
            <span class="label">桌号:</span> {{ currentOrder.tableNo }}
          </div>
          <div>
            <span class="label">状态:</span>
            <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusLabel(currentOrder.status) }}</el-tag>
          </div>
        </div>

        <el-divider />

        <!-- 订单明细 -->
        <el-table :data="currentItems" border size="small">
          <el-table-column prop="dishName" label="菜品" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="subtotal" label="小计" width="100">
            <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getItemStatusType(row.status)" size="small">
                {{ getItemStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button-group size="small">
                <el-button v-if="row.status === 0" @click="handleUpdateItemStatus(row, 1)">制作</el-button>
                <el-button v-if="row.status === 1" @click="handleUpdateItemStatus(row, 2)">完成</el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <el-divider />

        <div class="detail-footer">
          <div>
            <span class="label">下单时间:</span> {{ currentOrder.createdAt }}
          </div>
          <div class="total-amount">
            <span>合计: </span>
            <span class="amount">¥{{ currentOrder.payAmount?.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.orders-page {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.text-success {
  color: #67c23a;
}

.text-primary {
  color: #409eff;
}

.text-warning {
  color: #e6a23c;
}

.orders-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.discount {
  font-size: 12px;
  color: #909399;
  text-decoration: line-through;
}

.pay-amount {
  font-weight: bold;
  color: #f56c6c;
}

.order-detail {
  padding: 10px;
}

.detail-header {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.label {
  color: #909399;
  margin-right: 5px;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-amount {
  font-size: 18px;
}

.total-amount .amount {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
