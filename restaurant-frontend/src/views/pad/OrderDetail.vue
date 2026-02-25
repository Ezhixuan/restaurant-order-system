<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, getOrderByTable, updateItemStatus, payOrder, completeOrder, addDishToOrder } from '@/api/order'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const tableId = ref(Number(route.query.tableId) || 0)
const order = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(false)

// 结账对话框
const checkoutVisible = ref(false)
const payType = ref(3) // 默认现金
const payTypes = [
  { label: '现金', value: 3, icon: 'Money' },
  { label: '微信', value: 1, icon: 'ChatDotRound' },
  { label: '支付宝', value: 2, icon: 'Wallet' }
]

const loadOrder = async () => {
  if (!tableId.value) {
    ElMessage.error('桌台信息缺失')
    router.push('/pad/tables')
    return
  }

  loading.value = true
  try {
    // 使用新的 API 根据桌台ID获取订单
    const detail = await getOrderByTable(tableId.value)

    if (detail) {
      order.value = detail.order
      items.value = detail.items
    } else {
      ElMessage.warning('该桌台暂无未完成订单')
      // 可选：返回桌台列表或显示空状态
    }
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
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

const getOrderStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'primary',
    3: 'info',
    4: 'success'
  }
  return map[status] || 'info'
}

const getOrderStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '制作中',
    3: '待上菜',
    4: '已完成'
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
  checkoutVisible.value = true
}

// 确认结账
const confirmCheckout = async () => {
  if (!order.value) return
  
  try {
    // 先支付
    await payOrder(order.value.id, {
      payType: payType.value,
      amount: order.value.payAmount
    })
    
    // 再完成订单
    await completeOrder(order.value.id)
    
    ElMessage.success('结账成功')
    checkoutVisible.value = false
    
    // 返回桌台列表
    setTimeout(() => {
      router.push('/pad/tables')
    }, 1000)
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
      tableId: order.value.tableId
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
            <div class="info-value">¥{{ order.payAmount?.toFixed(2) }}</div>
          </div>
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

        <div class="items-list">
          <div
            v-for="item in items"
            :key="item.id"
            class="item-row"
            :class="{ 'finished': item.status === 2 }"
          >
            <div class="item-main">
              <div class="item-image">
                <img :src="item.dishImage || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
              </div>
              
              <div class="item-info">
                <div class="item-name">{{ item.dishName }}</div>
                <div class="item-meta">
                  <span class="item-price">¥{{ item.price?.toFixed(2) }} x {{ item.quantity }}</span>
                  <span class="item-subtotal">¥{{ item.subtotal?.toFixed(2) }}</span>
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
              <el-button-group v-if="item.status !== 2">
                <el-button
                  v-if="item.status === 0"
                  type="warning"
                  @click="handleUpdateStatus(item, 1)"
                >
                  开始制作
                </el-button>
                <el-button
                  v-if="item.status === 1"
                  type="success"
                  @click="handleUpdateStatus(item, 2)"
                >
                  制作完成
                </el-button>
              </el-button-group>
              
              <div v-else class="completed-text">✓ 已完成</div>
            </div>
          </div>
        </div>
        
        <van-empty v-if="items.length === 0" description="暂无菜品" />
      </el-card>

      <!-- 底部操作栏 -->
      <div v-if="order.status !== 4" class="bottom-bar">
        <div class="total-section">
          <div class="total-label">合计</div>
          <div class="total-value">¥{{ order.payAmount?.toFixed(2) }}</div>
        </div>
        
        <el-button
          v-if="order.status >= 3"
          type="success"
          size="large"
          @click="openCheckout"
        >
          结账
        </el-button>
        
        <el-button
          v-else
          type="primary"
          size="large"
          @click="addMore"
        >
          继续点餐
        </el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无订单信息" />

    <!-- 结账对话框 -->
    <el-dialog v-model="checkoutVisible" title="确认结账" width="450px">
      <div v-if="order" class="checkout-content">
        <div class="checkout-amount">
          <div class="amount-label">应收金额</div>
          <div class="amount-value">¥{{ order.payAmount?.toFixed(2) }}</div>
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
        
        <el-divider />
        
        <el-alert
          title="确认后桌台将自动清台"
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
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
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

.item-row.finished {
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
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  align-items: baseline;
  gap: 10px;
}

.total-label {
  font-size: 16px;
  color: #606266;
}

.total-value {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
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
