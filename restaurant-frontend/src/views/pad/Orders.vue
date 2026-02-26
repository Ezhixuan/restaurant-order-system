<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActiveOrders, getOrderDetail, updateItemStatus, payOrder, getOrderByTable, getUnpaidAmount } from '@/api/order'

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

// 活跃订单滑动视图
const showAllOrders = ref(false)
const currentOrderIndex = ref(0)
const allOrders = ref<any[]>([])
const allOrdersLoading = ref(false)

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

// 加载所有活跃订单（用于滑动视图）
const loadAllActiveOrders = async () => {
  allOrdersLoading.value = true
  try {
    const ordersList = await getActiveOrders()
    // 获取每个订单的详情（包含菜品）
    const ordersWithDetails = await Promise.all(
      ordersList.map(async (order: any) => {
        try {
          const detail = await getOrderDetail(order.id)
          return {
            ...order,
            items: detail?.items || [],
            unpaidAmount: detail?.items?.filter((i: any) => !i.isPaid).reduce((sum: number, i: any) => sum + (i.subtotal || 0), 0) || 0
          }
        } catch (e) {
          return { ...order, items: [], unpaidAmount: 0 }
        }
      })
    )
    allOrders.value = ordersWithDetails
    currentOrderIndex.value = 0
  } catch (error) {
    ElMessage.error('加载活跃订单失败')
  } finally {
    allOrdersLoading.value = false
  }
}

// 打开所有订单滑动视图
const openAllOrdersView = async () => {
  await loadAllActiveOrders()
  if (allOrders.value.length === 0) {
    ElMessage.info('暂无活跃订单')
    return
  }
  showAllOrders.value = true
}

// 切换订单
const prevOrder = () => {
  if (currentOrderIndex.value > 0) {
    currentOrderIndex.value--
  }
}

const nextOrder = () => {
  if (currentOrderIndex.value < allOrders.value.length - 1) {
    currentOrderIndex.value++
  }
}

// 更新菜品状态
const handleUpdateItemStatus = async (item: any, newStatus: number) => {
  try {
    await updateItemStatus(item.id, newStatus)
    ElMessage.success('状态更新成功')
    // 刷新当前订单数据
    await loadAllActiveOrders()
    // 保持当前索引
    if (currentOrderIndex.value >= allOrders.value.length) {
      currentOrderIndex.value = Math.max(0, allOrders.value.length - 1)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 点击订单卡片 - 显示详情
const handleCardClick = async (order: any) => {
  detailLoading.value = true
  detailVisible.value = true
  selectedOrder.value = order
  
  try {
    const detail = await getOrderDetail(order.id)
    if (detail) {
      selectedItems.value = detail.items || []
      try {
        selectedUnpaidAmount.value = await getUnpaidAmount(order.id)
      } catch (e) {
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
  const order = allOrders.value[currentOrderIndex.value] || selectedOrder.value
  if (!order) return
  
  showAllOrders.value = false
  detailVisible.value = false
  router.push({
    path: '/pad/order',
    query: {
      tableId: order.tableId,
      tableNo: order.tableNo,
      orderId: order.id,
      customerCount: order.customerCount,
      mode: 'add'
    }
  })
}

// 打开结账对话框
const openCheckout = () => {
  const order = allOrders.value[currentOrderIndex.value]
  if (!order) return
  if (order.unpaidAmount <= 0) {
    ElMessage.warning('没有待结账的金额')
    return
  }
  selectedOrder.value = order
  selectedUnpaidAmount.value = order.unpaidAmount
  checkoutVisible.value = true
}

// 确认结账
const confirmCheckout = async () => {
  const order = allOrders.value[currentOrderIndex.value] || selectedOrder.value
  if (!order) return
  
  try {
    await payOrder(order.id, {
      payType: payType.value,
      amount: order.unpaidAmount || selectedUnpaidAmount.value
    })
    
    ElMessage.success('结账成功')
    checkoutVisible.value = false
    showAllOrders.value = false
    await loadOrders()
    await loadAllActiveOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '结账失败')
  }
}

// 返回
const goBack = () => {
  router.push('/pad/tables')
}

// 获取状态样式
const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'success',
    4: 'danger'
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

const getItemStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

const getItemStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

onMounted(loadOrders)
</script>

<template>
  <div class="pad-orders" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <el-button type="success" size="large" @click="openAllOrdersView">
          <el-icon><View /></el-icon>
          查看所有活跃订单
        </el-button>
        <el-button @click="goBack">返回桌台</el-button>
      </div>
    </div>

    <!-- 订单卡片网格 -->
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

    <!-- 所有活跃订单滑动视图 -->
    <el-dialog
      v-model="showAllOrders"
      title="活跃订单管理"
      width="90%"
      :close-on-click-modal="false"
      class="orders-slider-dialog"
    >
      <div v-loading="allOrdersLoading" class="orders-slider-container">
        <!-- 订单指示器 -->
        <div class="order-indicator">
          <span class="current-index">{{ currentOrderIndex + 1 }}</span>
          <span class="total">/ {{ allOrders.length }}</span>
          <span class="table-info" v-if="allOrders[currentOrderIndex]">
            {{ allOrders[currentOrderIndex].tableNo }}号桌
          </span>
        </div>

        <!-- 滑动控制按钮 -->
        <div class="slider-controls">
          <el-button
            circle
            size="large"
            :disabled="currentOrderIndex === 0"
            @click="prevOrder"
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          
          <el-button
            circle
            size="large"
            :disabled="currentOrderIndex === allOrders.length - 1"
            @click="nextOrder"
          >
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <!-- 当前订单详情 -->
        <div v-if="allOrders[currentOrderIndex]" class="current-order">
          <el-card class="order-detail-card">
            <template #header>
              <div class="detail-header">
                <div class="header-left">
                  <h3>{{ allOrders[currentOrderIndex].tableNo }}号桌</h3>
                  <el-tag :type="getStatusType(allOrders[currentOrderIndex].status)" effect="dark">
                    {{ getStatusLabel(allOrders[currentOrderIndex].status) }}
                  </el-tag>
                </div>
                <div class="header-right">
                  <div class="order-meta">
                    <span>{{ allOrders[currentOrderIndex].customerCount }}人用餐</span>
                    <span>{{ allOrders[currentOrderIndex].items?.length || 0 }}道菜</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 菜品列表 -->
            <div class="items-list">
              <h4>菜品清单</h4>
              <div
                v-for="item in allOrders[currentOrderIndex].items"
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
                  <div v-if="item.remark" class="item-remark">备注: {{ item.remark }}</div>
                </div>
                
                <div class="item-status-section">
                  <el-tag :type="getItemStatusType(item.status)" size="small">
                    {{ getItemStatusLabel(item.status) }}
                  </el-tag>
                  
                  <!-- 状态操作按钮 -->
                  <div v-if="!item.isPaid" class="status-actions">
                    <el-button
                      v-if="item.status === 0"
                      type="warning"
                      size="small"
                      @click="handleUpdateItemStatus(item, 1)"
                    >
                      开始制作
                    </el-button>
                    <el-button
                      v-if="item.status === 1"
                      type="success"
                      size="small"
                      @click="handleUpdateItemStatus(item, 2)"
                    >
                      制作完成
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 金额信息 -->
            <div class="amount-section">
              <div class="amount-row">
                <span>总计金额:</span>
                <span class="amount-value">¥{{ allOrders[currentOrderIndex].payAmount?.toFixed(2) }}</span>
              </div>
              <div v-if="allOrders[currentOrderIndex].unpaidAmount > 0" class="amount-row unpaid">
                <span>未结账金额:</span>
                <span class="amount-value text-danger">¥{{ allOrders[currentOrderIndex].unpaidAmount.toFixed(2) }}</span>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 底部操作栏 -->
        <div class="slider-actions">
          <el-button @click="showAllOrders = false">关闭</el-button>
          <el-button type="primary" @click="showAddDish">
            <el-icon><Plus /></el-icon>加菜
          </el-button>
          <el-button
            v-if="allOrders[currentOrderIndex]?.unpaidAmount > 0"
            type="success"
            @click="openCheckout"
          >
            结账 (¥{{ allOrders[currentOrderIndex].unpaidAmount.toFixed(2) }})
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 结账对话框 -->
    <el-dialog v-model="checkoutVisible" title="确认结账" width="450px">
      <div class="checkout-content">
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

.header-actions {
  display: flex;
  gap: 10px;
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

/* 滑动视图样式 */
.orders-slider-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.orders-slider-container {
  position: relative;
  min-height: 500px;
}

.order-indicator {
  text-align: center;
  margin-bottom: 20px;
  font-size: 18px;
}

.current-index {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.total {
  color: #909399;
}

.table-info {
  margin-left: 15px;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.slider-controls {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  transform: translateY(-50%);
  z-index: 10;
  pointer-events: none;
}

.slider-controls .el-button {
  pointer-events: auto;
}

.current-order {
  padding: 0 60px;
}

.order-detail-card {
  margin-bottom: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h3 {
  margin: 0;
  font-size: 24px;
}

.order-meta {
  display: flex;
  gap: 20px;
  color: #909399;
}

.items-list {
  margin-top: 20px;
}

.items-list h4 {
  margin-bottom: 15px;
  color: #303133;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.item-row.is-paid {
  background: #e8f5e9;
  opacity: 0.7;
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

.item-status-section {
  text-align: center;
  margin-left: 15px;
}

.status-actions {
  margin-top: 8px;
}

.amount-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
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

.slider-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
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
  .current-order {
    padding: 0 40px;
  }
  
  .slider-controls {
    padding: 0 10px;
  }
}
</style>
