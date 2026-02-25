<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveOrders, getOrderDetail, addDishToOrder, completeOrder } from '@/api/order'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)

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

const showDetail = async (order: any) => {
  // 可以显示订单详情或跳转到加菜页面
  router.push({
    path: '/pad/order',
    query: {
      tableId: order.tableId,
      orderId: order.id,
      customerCount: order.customerCount
    }
  })
}

const handleCheckout = async (order: any) => {
  try {
    await completeOrder(order.id)
    ElMessage.success('结账成功')
    loadOrders()
  } catch (error: any) {
    ElMessage.error(error.message || '结账失败')
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'primary',
    3: 'info'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '制作中',
    3: '待上菜'
  }
  return map[status] || '未知'
}

onMounted(loadOrders)
</script>

<template>
  <div class="pad-orders" v-loading="loading">
    <div class="header">
      <h2>订单管理</h2>
      <el-button type="primary" @click="router.push('/pad/tables')">返回桌台</el-button>
    </div>

    <el-row :gutter="20">
      <el-col
        v-for="order in orders"
        :key="order.id"
        :span="8"
        style="margin-bottom: 20px"
      >
        <el-card class="order-card" shadow="hover">
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
              <el-button size="small" @click="showDetail(order)">加菜</el-button>
              <el-button
                v-if="order.status >= 3"
                size="small"
                type="primary"
                @click="handleCheckout(order)"
              >
                结账
              </el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="orders.length === 0" description="暂无进行中的订单" />
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
  justify-content: space-between;
}
</style>
