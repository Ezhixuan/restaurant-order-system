<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderByTable, updateItemStatus, payOrder, getUnpaidAmount } from '@/api/order'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const tableId = ref(Number(route.query.tableId) || 0)
const tableNo = ref(route.query.tableNo as string || '')
const order = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(false)
const unpaidAmount = ref(0)

// 结账对话框
const checkoutVisible = ref(false)
const payType = ref(3) // 默认现金
const payTypes = [
  { label: '现金', value: 3, icon: 'Money' },
  { label: '微信', value: 1, icon: 'ChatDotRound' },
  { label: '支付宝', value: 2, icon: 'Wallet' }
]

// 结账金额相关
const actualPayAmount = ref<number>(0)
const discountAmount = computed(() => {
  return (unpaidAmount.value || 0) - (actualPayAmount.value || 0)
})

// 处理抹零 - 去掉个位数（如164→160）
const handleRoundDown = () => {
  const originalAmount = Math.floor(unpaidAmount.value || 0)
  // 去掉个位数，向下取整到十位
  actualPayAmount.value = Math.floor(originalAmount / 10) * 10
}

// 重置实付金额
const resetPayAmount = () => {
  actualPayAmount.value = Math.floor(unpaidAmount.value || 0)
}

const loadOrder = async () => {
  if (!tableId.value) {
    ElMessage.error('桌台信息缺失')
    router.push('/pad/tables')
    return
  }

  loading.value = true
  try {
    const detail = await getOrderByTable(tableId.value)

    if (detail) {
      order.value = detail.order
      items.value = detail.items || []

      // 获取未结账金额
      if (order.value?.id) {
        try {
          const amount = await getUnpaidAmount(order.value.id)
          unpaidAmount.value = amount || 0
        } catch (e) {
          // 如果接口不存在，手动计算
          unpaidAmount.value = calculateUnpaidAmount(items.value)
        }
      }
    } else {
      ElMessage.warning('该桌台暂无未完成订单')
      items.value = []
    }
  } catch (error) {
    ElMessage.error('加载订单失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 手动计算未结账金额（备选）
const calculateUnpaidAmount = (items: any[]) => {
  return items
    .filter(item => !item.isPaid)
    .reduce((sum, item) => sum + (item.subtotal || 0), 0)
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' }
  return map[status] || '未知'
}

const getOrderStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',      // 待上菜
    1: 'warning',   // 上菜中
    2: 'success',   // 待结账
    3: 'success',   // 已完成
    4: 'danger'     // 追加订单
  }
  return map[status] || 'info'
}

const getOrderStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待上菜',
    1: '上菜中',
    2: '待结账',
    3: '已完成',
    4: '追加订单'
  }
  return map[status] || '未知'
}

// 修改菜品状态
const handleUpdateStatus = async (item: any, newStatus: number) => {
  try {
    await updateItemStatus(item.id, newStatus)
    ElMessage.success('状态更新成功')
    loadOrder() // 刷新
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 打开结账对话框
const openCheckout = () => {
  if (!order.value) return

  // 计算应付金额
  const shouldPay = unpaidAmount.value
  if (shouldPay <= 0) {
    ElMessage.warning('没有待结账的金额')
    return
  }

  // 初始化实付金额（直接取整数）
  actualPayAmount.value = Math.floor(shouldPay)
  checkoutVisible.value = true
}

// 确认结账
const confirmCheckout = async () => {
  if (!order.value) return

  try {
    await payOrder(order.value.id, {
      payType: payType.value,
      amount: actualPayAmount.value // 使用实付金额
    })

    ElMessage.success('结账成功')
    checkoutVisible.value = false
    loadOrder() // 刷新
  } catch (error: any) {
    ElMessage.error(error.message || '结账失败')
  }
}

// 加菜
const addMore = () => {
  if (!order.value) return

  cartStore.setTableInfo(order.value.tableId, order.value.tableNo, order.value.customerCount)
  cartStore.clearCart()

  router.push({
    path: '/pad/order',
    query: {
      mode: 'add',
      orderId: order.value.id,
      tableId: order.value.tableId,
      tableNo: order.value.tableNo
    }
  })
}

// 返回
const goBack = () => {
  router.push('/pad/tables')
}

onMounted(loadOrder)
</script>

<template>
  <div class="order-detail-page" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="header">
      <div class="header-left">
        <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
        <div v-if="order" class="order-info">
          <span class="table-name">{{ order.tableNo }}号桌</span>
          <el-tag :type="getOrderStatusType(order.status)" effect="dark" size="large">
            {{ getOrderStatusLabel(order.status) }}
          </el-tag>
        </div>
      </div>

      <div class="header-right" v-if="order">
        <span class="order-no">订单: {{ order.orderNo }}</span>
      </div>
    </div>

    <!-- 订单内容 -->
    <div v-if="order" class="order-content">
      <!-- 订单信息卡片 -->
      <el-card class="info-card" shadow="never">
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">用餐人数</div>
            <div class="info-value">{{ order.customerCount }}人</div>
          </div>

          <div class="info-item">
            <div class="info-label">下单时间</div>
            <div class="info-value">{{ new Date(order.createdAt).toLocaleString() }}</div>
          </div>

          <div class="info-item">
            <div class="info-label">菜品数量</div>
            <div class="info-value">{{ items.reduce((sum, i) => sum + i.quantity, 0) }}份</div>
          </div>

          <div class="info-item amount">
            <div class="info-label">应收金额</div>
            <div class="info-value" :class="{ 'text-danger': unpaidAmount > 0 }">
              ¥{{ unpaidAmount.toFixed(2) }}
              <span v-if="unpaidAmount !== order.payAmount" class="total-amount">
                (总计: ¥{{ order.payAmount?.toFixed(2) }})
              </span>
            </div>
          </div>
        </div>

        <!-- 订单备注 -->
        <div v-if="order.remark" class="order-remark">
          <el-icon><Info-Filled /></el-icon>
          <span>订单备注: {{ order.remark }}</span>
        </div>
      </el-card>

      <!-- 菜品列表 -->
      <el-card class="items-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>菜品清单</span>
            <el-button type="primary" size="small" @click="addMore">
              <el-icon><Plus /></el-icon>加菜
            </el-button>
          </div>
        </template>

        <div v-if="items.length > 0" class="items-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="item-row"
            :class="{ 'finished': item.status === 2, 'paid': item.isPaid }"
          >
            <div class="item-main">
              <div class="item-image">
                <img :src="item.dishImage || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
                <div v-if="item.isPaid" class="paid-badge">已结账</div>
              </div>

              <div class="item-info">
                <div class="item-name">
                  {{ item.dishName }}
                  <el-tag v-if="item.specName" type="info" size="small">{{ item.specName }}</el-tag>
                </div>
                <div class="item-meta">
                  <span class="item-price">¥{{ item.price?.toFixed(2) }} x {{ item.quantity }}</span>
                  <span class="item-subtotal" :class="{ 'is-paid': item.isPaid }">¥{{ item.subtotal?.toFixed(2) }}</span>
                </div>

                <div v-if="item.remark" class="item-remark">
                  备注: {{ item.remark }}
                </div>
              </div>
            </div>

            <div class="item-status">
              <el-tag :type="getStatusType(item.status)" size="large" effect="dark">
                {{ getStatusLabel(item.status) }}
              </el-tag>
            </div>

            <div class="item-actions">
              <el-button-group v-if="!item.isPaid">
                <el-button
                  v-if="item.status === 0"
                  type="warning"
                  size="small"
                  @click="handleUpdateStatus(item, 1)"
                >
                  开始制作
                </el-button>
                <el-button
                  v-if="item.status === 1"
                  type="success"
                  size="small"
                  @click="handleUpdateStatus(item, 2)"
                >
                  制作完成
                </el-button>
                <el-button
                  v-if="item.status === 2"
                  type="primary"
                  size="small"
                  disabled
                >
                  已完成
                </el-button>
              </el-button-group>

              <div v-else class="completed-text">✓ 已结账</div>
            </div>
          </div>
        </div>

        <van-empty v-else description="暂无菜品" />
      </el-card>

      <!-- 底部操作栏 -->
      <div class="bottom-bar">
        <div class="total-section">
          <div class="total-label">未结账金额</div>
          <div class="total-value" :class="{ 'text-danger': unpaidAmount > 0 }">
            ¥{{ unpaidAmount.toFixed(2) }}
          </div>
        </div>

        <el-button
          type="success"
          size="large"
          :disabled="unpaidAmount <= 0"
          @click="openCheckout"
        >
          结账
        </el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无订单信息" />

    <!-- 结账对话框 -->
    <el-dialog v-model="checkoutVisible" title="确认结账" width="500px">
      <div v-if="order" class="checkout-content">
        <!-- 应收金额 -->
        <div class="checkout-row">
          <span class="label">应收金额</span>
          <span class="value original">¥{{ Math.floor(unpaidAmount) }}</span>
        </div>

        <!-- 实付金额输入 -->
        <div class="checkout-row pay-input-row">
          <span class="label">实付金额</span>
          <div class="pay-input-wrapper">
            <el-input-number
              v-model="actualPayAmount"
              :min="0"
              :max="Math.floor(unpaidAmount * 2)"
              :precision="0"
              :step="1"
              size="large"
              style="width: 150px"
            />
            <el-button type="warning" size="small" @click="handleRoundDown">
              <el-icon><Delete /></el-icon>抹零
            </el-button>
            <el-button size="small" @click="resetPayAmount">重置</el-button>
          </div>
        </div>

        <!-- 优惠金额 -->
        <div class="checkout-row discount-row" v-if="discountAmount > 0">
          <span class="label">优惠金额</span>
          <span class="value discount">-¥{{ Math.floor(discountAmount) }}</span>
        </div>
        <div class="checkout-row discount-row" v-else-if="discountAmount < 0">
          <span class="label">溢收金额</span>
          <span class="value extra">+¥{{ Math.floor(Math.abs(discountAmount)) }}</span>
        </div>

        <el-divider />

        <!-- 支付方式 -->
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

        <el-divider />

        <!-- 确认金额 -->
        <div class="checkout-row final-row">
          <span class="label">确认收款</span>
          <span class="value final">¥{{ Math.floor(actualPayAmount) }}</span>
        </div>

        <el-alert
          title="确认后该批次菜品将标记为已结账"
          type="info"
          :closable="false"
          center
        />
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
.order-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 80px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.order-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.table-name {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.order-no {
  font-size: 14px;
  color: #909399;
}

.order-content {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.info-card {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.info-item {
  text-align: center;
}

.info-item.amount .info-value {
  font-size: 24px;
  font-weight: bold;
}

.text-danger {
  color: #f56c6c;
}

.total-amount {
  font-size: 14px;
  color: #909399;
  margin-left: 5px;
}

.order-remark {
  margin-top: 15px;
  padding: 12px 15px;
  background: #fdf6ec;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e6a23c;
  font-size: 14px;
}

.info-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.info-value {
  font-size: 18px;
  color: #303133;
  font-weight: 500;
}

.items-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 12px;
  gap: 15px;
}

.item-row.paid {
  opacity: 0.7;
  background: #e8f5e9;
}

.item-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 15px;
}

.item-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.paid-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(103, 194, 58, 0.8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.item-price {
  font-size: 14px;
  color: #909399;
}

.item-subtotal {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.item-subtotal.is-paid {
  color: #67c23a;
  text-decoration: line-through;
}

.item-remark {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

.item-status {
  min-width: 100px;
  text-align: center;
}

.item-actions {
  min-width: 120px;
  text-align: right;
}

.completed-text {
  color: #67c23a;
  font-weight: bold;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.total-section {
  display: flex;
  flex-direction: column;
}

.total-label {
  font-size: 14px;
  color: #606266;
}

.total-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

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

.amount-value {
  font-size: 48px;
  font-weight: bold;
  color: #f56c6c;
}

/* 结账金额行样式 */
.checkout-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 16px;
}

.checkout-row .label {
  color: #606266;
}

.checkout-row .value {
  font-weight: 500;
  color: #303133;
}

.checkout-row .value.original {
  font-size: 18px;
  text-decoration: line-through;
  color: #909399;
}

.checkout-row .value.discount {
  font-size: 18px;
  color: #67c23a;
}

.checkout-row .value.extra {
  font-size: 18px;
  color: #e6a23c;
}

.checkout-row .value.final {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}

.pay-input-row {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin: 10px 0;
}

.pay-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.discount-row {
  padding: 8px 0;
}

.final-row {
  padding: 15px 0;
  border-top: 2px dashed #dcdfe6;
  margin-top: 10px;
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
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .item-row {
    flex-wrap: wrap;
  }

  .item-actions {
    width: 100%;
    text-align: left;
    margin-top: 10px;
  }
}
</style>
