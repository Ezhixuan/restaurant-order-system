<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveOrders, getOrderDetail, completeOrder, getOrderByTable, getUnpaidAmount } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)

// 订单详情对话框
const detailVisible = ref(false)
const selectedOrder = ref<any>(null)
const selectedItems = ref<any[]>([])
const selectedUnpaidAmount = ref(0)
const detailLoading = ref(false)

// 结账对话框
const checkoutVisible = ref(false)
const payType = ref(3)
const payTypes = [
  { label: '现金', value: 3, icon: 'Money' },
  { label: '微信', value: 1, icon: 'ChatDotRound' },
  { label: '支付宝', value: 2, icon: 'Wallet' }
]

// 从路由参数获取桌台信息
const queryTableId = computed(() => Number(route.query.tableId) || 0)
const queryTableNo = computed(() => route.query.tableNo as string || '')
const queryCustomerCount = computed(() => Number(route.query.customerCount) || 1)

const loadOrders = async () => {
  loading.value = true
  try {
    if (queryTableId.value) {
      // 有桌台信息，获取该桌台的订单
      const order = await getOrderByTable(queryTableId.value)
      orders.value = order ? [order.order] : []
    } else {
      // 没有桌台信息，获取所有活跃订单
      orders.value = await getActiveOrders()
    }
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

// 点击订单卡片 - 显示详情
const handleCardClick = async (order: any) => {
  detailLoading.value = true
  detailVisible.value = true
  selectedOrder.value = order
  
  try {
    // 获取订单详情（包含菜品）
    const detail = await getOrderDetail(order.id)
    if (detail) {
      selectedItems.value = detail.items || []
      // 获取未结账金额
      try {
        selectedUnpaidAmount.value = await getUnpaidAmount(order.id)
      } catch (e) {
        // 手动计算
        selectedUnpaidAmount.value = selectedItems.value
          .filter((item: any) => !item.isPaid)
          .reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0)
      }
    }
  } catch (error) {
    ElMessage.error('加载订单详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 跳转到加菜页面
const showAddDish = () => {
  if (!selectedOrder.value) return
  
  detailVisible.value = false
  router.push({
    path: '/pad/order',
    query: {
      tableId: selectedOrder.value.tableId,
      tableNo: selectedOrder.value.tableNo,
      orderId: selectedOrder.value.id,
      customerCount: selectedOrder.value.customerCount,
      mode: 'add'
    }
  })
}

// 打开结账对话框
const openCheckout = () => {
  if (!selectedOrder.value) return
  if (selectedUnpaidAmount.value <= 0) {
    ElMessage.warning('没有待结账的金额')
    return
  }
  checkoutVisible.value = true
}

// 确认结账
const confirmCheckout = async () => {
  if (!selectedOrder.value) return
  
  try {
    const { payOrder } = await import('@/api/order')
    await payOrder(selectedOrder.value.id, {
      payType: payType.value,
      amount: selectedUnpaidAmount.value
    })
    
    ElMessage.success('结账成功')
    checkoutVisible.value = false
    detailVisible.value = false
    loadOrders() // 刷新列表
  } catch (error: any) {
    ElMessage.error(error.message || '结账失败')
  }
}

// 获取菜品状态样式
const getItemStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

const getItemStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',      // 待上菜
    1: 'warning',   // 上菜中
    2: 'success',   // 待结账
    3: 'success',   // 已完成
    4: 'danger'     // 追加订单
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待上菜',
    1: '上菜中',
    2: '待结账',
    3: '已完成',
    4: '追加订单'
  }
  return map[status] || '未知'
}

// 返回桌台或回到点餐页面
const goBack = () => {
  if (queryTableId.value) {
    router.push({
      path: '/pad/order',
      query: {
        tableId: queryTableId.value,
        tableNo: queryTableNo.value,
        customerCount: queryCustomerCount.value
      }
    })
  } else {
    router.push('/pad/tables')
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="pad-orders" v-loading="loading">
    <div class="header">
      <h2>订单管理</h2>
      <el-button type="primary" @click="goBack">返回桌台</el-button>
    </div>

    <el-row :gutter="20">
      <el-col
        v-for="order in orders"
        :key="order.id"
        :span="8"
        style="margin-bottom: 20px"
      >
        <el-card class="order-card" shadow="hover" @click="handleCardClick(order)">
          <template #header>
            <div class="order-header">
              <span>{{ order.tableNo }} - {{ order.orderNo?.slice(-6) }}</span>
              <el-tag :type="getStatusType(order.status)" size="small">
                {{ getStatusLabel(order.status) }}
              </el-tag>
            </div>
          </template>

          <div class="order-info">
            <div>人数: {{ order.customerCount }}人</div>
            <div class="amount">¥{{ order.payAmount?.toFixed(2) }}</div>
          </div>

          <template #footer>
            <div class="order-actions">
              <el-button size="small" @click.stop="handleCardClick(order)">查看详情</el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="orders.length === 0" description="暂无进行中的订单" />

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="`${selectedOrder?.tableNo}号桌 - 订单详情`"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-loading="detailLoading">
        <div v-if="selectedOrder" class="detail-header">
          <div class="detail-info">
            <span>订单号: {{ selectedOrder.orderNo }}</span>
            <el-tag :type="getStatusType(selectedOrder.status)" effect="dark">
              {{ getStatusLabel(selectedOrder.status) }}
            </el-tag>
          </div>
          <div class="detail-meta">
            <span>人数: {{ selectedOrder.customerCount }}人</span>
            <span>下单时间: {{ new Date(selectedOrder.createdAt).toLocaleString() }}</span>
          </div>
        </div>

        <el-divider />

        <!-- 菜品列表 -->
        <div class="items-list">
          <h4>菜品清单 ({{ selectedItems.length }}道)</h4>
          
          <div v-if="selectedItems.length > 0" class="items-container">
            <div
              v-for="item in selectedItems"
              :key="item.id"
              class="item-row"
              :class="{ 'is-paid': item.isPaid }"
            >
              <div class="item-info">
                <div class="item-name">
                  {{ item.dishName }}
                  <el-tag v-if="item.isPaid" type="success" size="small">已结账</el-tag>
                </div>
                <div class="item-meta">
                  <span>¥{{ item.price?.toFixed(2) }} x {{ item.quantity }}</span>
                  <span class="item-subtotal">¥{{ item.subtotal?.toFixed(2) }}</span>
                </div>
                <div v-if="item.remark" class="item-remark">
                  备注: {{ item.remark }}
                </div>
              </div>
              <div class="item-status">
                <el-tag :type="getItemStatusType(item.status)" size="small">
                  {{ getItemStatusLabel(item.status) }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <el-empty v-else description="暂无菜品" />
        </div>

        <el-divider />

        <!-- 金额信息 -->
        <div class="amount-section">
          <div class="amount-row">
            <span>总计金额:</span>
            <span class="amount-value">¥{{ selectedOrder?.payAmount?.toFixed(2) }}</span>
          </div>
          <div v-if="selectedUnpaidAmount !== selectedOrder?.payAmount" class="amount-row unpaid">
            <span>未结账金额:</span>
            <span class="amount-value text-danger">¥{{ selectedUnpaidAmount.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="showAddDish">
          <el-icon><Plus /></el-icon>加菜
        </el-button>
        <el-button
          v-if="selectedUnpaidAmount > 0"
          type="success"
          @click="openCheckout"
        >
          结账 (¥{{ selectedUnpaidAmount.toFixed(2) }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 结账对话框 -->
    <el-dialog v-model="checkoutVisible" title="确认结账" width="450px">
      <div v-if="selectedOrder" class="checkout-content">
        <div class="checkout-amount">
          <div class="amount-label">应收金额</div>
          <div class="amount-value">¥{{ selectedUnpaidAmount.toFixed(2) }}</div>
        </div>

        <el-divider />

        <div class="pay-type-section">
          <div class="section-label">选择支付方式</div>
          <div class="pay-type-options">
            <div
              v-for="type in payTypes"
              :key="type.value"
              class="pay-type-option"
              :class="{ active: payType === type.value }"
              @click="payType = type.value"
            >
              <el-icon :size="28"><component :is="type.icon" /></el-icon>
              <div class="type-label">{{ type.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" size="large" @click="confirmCheckout">
          确认收款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pad-orders {
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.order-card {
  cursor: pointer;
  transition: all 0.3s;
}

.order-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info {
  text-align: center;
  padding: 10px 0;
}

.amount {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
  margin-top: 10px;
}

.order-actions {
  display: flex;
  justify-content: center;
}

/* 订单详情对话框样式 */
.detail-header {
  margin-bottom: 10px;
}

.detail-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.detail-meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
}

.items-list h4 {
  margin-bottom: 15px;
  color: #303133;
}

.items-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.item-row.is-paid {
  background: #e8f5e9;
}

.item-info {
  flex: 1;
}

.item-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  color: #606266;
  font-size: 14px;
}

.item-subtotal {
  font-weight: bold;
  color: #f56c6c;
}

.item-remark {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

.item-status {
  margin-left: 15px;
}

.amount-section {
  padding: 10px 0;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 16px;
}

.amount-row.unpaid {
  font-weight: bold;
  font-size: 18px;
}

.amount-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.text-danger {
  color: #f56c6c;
}

/* 结账对话框样式 */
.checkout-content {
  padding: 10px;
}

.checkout-amount {
  text-align: center;
  padding: 20px 0;
}

.amount-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.checkout-amount .amount-value {
  font-size: 48px;
  font-weight: bold;
  color: #f56c6c;
}

.pay-type-section {
  padding: 10px 0;
}

.section-label {
  font-size: 16px;
  color: #303133;
  margin-bottom: 15px;
  text-align: center;
}

.pay-type-options {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.pay-type-option {
  width: 100px;
  height: 100px;
  border: 2px solid #dcdfe6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.pay-type-option:hover {
  border-color: #409eff;
}

.pay-type-option.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-label {
  font-size: 14px;
  color: #606266;
}

@media (max-width: 768px) {
  .el-col {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
