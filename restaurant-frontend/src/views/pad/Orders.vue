<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActiveOrders, getOrderDetail, addDishToOrder, completeOrder, getOrderByTable } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(false)

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

const showDetail = async (order: any) => {
  // 跳转到加菜页面，携带订单的桌台信息
  router.push({
    path: '/pad/order',
    query: {
      tableId: order.tableId,
      tableNo: order.tableNo,
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

// 返回桌台或回到点餐页面
const goBack = () => {
  // 如果有桌台信息，返回到对应桌台的点餐页面；否则返回桌台列表
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
