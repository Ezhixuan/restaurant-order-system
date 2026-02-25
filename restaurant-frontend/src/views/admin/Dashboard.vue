<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTodayStats, getTopDishes, getTableStats } from '@/api/report'
import { getActiveOrders } from '@/api/order'
import * as echarts from 'echarts'

const router = useRouter()

const todayStats = ref({
  totalRevenue: 0,
  orderCount: 0,
  avgAmount: 0
})

const topDishes = ref<any[]>([])
const tableStats = ref<any[]>([])
const recentOrders = ref<any[]>([])
const loading = ref(false)

// 图表实例
let revenueChart: echarts.ECharts | null = null

const loadData = async () => {
  loading.value = true
  try {
    const [stats, dishes, tables, orders] = await Promise.all([
      getTodayStats(),
      getTopDishes(5),
      getTableStats(),
      getActiveOrders()
    ])
    todayStats.value = stats
    topDishes.value = dishes
    tableStats.value = tables
    recentOrders.value = orders.slice(0, 10) // 最近10条
    
    // 初始化图表
    initRevenueChart()
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const initRevenueChart = () => {
  const chartDom = document.getElementById('revenueChart')
  if (!chartDom) return
  
  revenueChart = echarts.init(chartDom)
  const option = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00'],
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      data: [120, 280, 580, 920, 680, 420, 380, 520, 880, 1200, 980, 650],
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#409EFF',
        width: 3
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ]
        }
      },
      itemStyle: { color: '#409EFF' }
    }]
  }
  revenueChart.setOption(option)
}

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

const goToOrders = () => {
  router.push('/admin/orders')
}

const goToKitchen = () => {
  router.push('/admin/kitchen')
}

// 自动刷新
let refreshTimer: number | null = null

onMounted(() => {
  loadData()
  refreshTimer = window.setInterval(loadData, 30000) // 30秒刷新
  
  window.addEventListener('resize', () => {
    revenueChart?.resize()
  })
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  revenueChart?.dispose()
})
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-button type="primary" size="large" @click="goToKitchen">
        <el-icon><Dish /></el-icon>
        后厨监控
      </el-button>
      <el-button type="success" size="large" @click="goToOrders">
        <el-icon><Document /></el-icon>
        订单管理
      </el-button>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon bg-success">
            <el-icon :size="32" color="#fff"><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">今日营业额</div>
            <div class="stat-value text-success">¥{{ todayStats.totalRevenue?.toFixed(2) || '0.00' }}</div>
            <div class="stat-trend">
              <el-tag size="small" type="success">↑ 12.5%</el-tag>
              <span class="trend-text">较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon bg-primary">
            <el-icon :size="32" color="#fff"><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">今日订单</div>
            <div class="stat-value text-primary">{{ todayStats.orderCount || 0 }}</div>
            <div class="stat-trend">
              <el-tag size="small" type="success">↑ 8.3%</el-tag>
              <span class="trend-text">较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon bg-warning">
            <el-icon :size="32" color="#fff"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">平均客单价</div>
            <div class="stat-value text-warning">¥{{ todayStats.avgAmount?.toFixed(2) || '0.00' }}</div>
            <div class="stat-trend">
              <el-tag size="small" type="danger">↓ 2.1%</el-tag>
              <span class="trend-text">较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 中间区域：图表 + 实时订单 -->
    <el-row :gutter="20" class="middle-row">
      <!-- 营业额趋势 -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">📈 今日营业额趋势</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="today">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div id="revenueChart" style="height: 350px;"></div>
        </el-card>
      </el-col>

      <!-- 实时订单 -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="orders-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">📋 实时订单</span>
              <el-tag type="danger" effect="dark" v-if="recentOrders.length > 0">
                {{ recentOrders.length }} 进行中
              </el-tag>
            </div>
          </template>
          
          <div class="orders-list">
            <div
              v-for="order in recentOrders"
              :key="order.id"
              class="order-item"
              :class="{ 'new-order': order.status === 1 }"
            >
              <div class="order-main">
                <span class="table-no">{{ order.tableNo }}号桌</span>
                <span class="order-amount">¥{{ order.payAmount?.toFixed(0) }}</span>
              </div>
              <div class="order-sub">
                <el-tag :type="getStatusType(order.status)" size="small">
                  {{ getStatusLabel(order.status) }}
                </el-tag>
                <span class="order-time">{{ new Date(order.createdAt).toLocaleTimeString() }}</span>
              </div>
            </div>
            
            <el-empty v-if="recentOrders.length === 0" description="暂无进行中的订单" />
          </div>
          
          <div class="orders-footer">
            <el-button text type="primary" @click="goToOrders">
              查看全部订单 →
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部区域：热销菜品 + 桌台状态 -->
    <el-row :gutter="20" class="bottom-row">
      <!-- 热销菜品 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">🔥 热销菜品 TOP5</span>
          </template>
          
          <div class="dishes-ranking">
            <div
              v-for="(dish, index) in topDishes"
              :key="index"
              class="rank-item"
            >
              <div class="rank-number" :class="`rank-${index + 1}`">
                {{ index + 1 }}
              </div>
              <div class="rank-info">
                <div class="rank-name">{{ dish.dishName }}</div>
                <div class="rank-bar">
                  <div
                    class="rank-progress"
                    :style="{ width: `${(dish.totalQuantity / topDishes[0].totalQuantity) * 100}%` }"
                  ></div>
                </div>
              </div>
              <div class="rank-stats">
                <div class="rank-quantity">{{ dish.totalQuantity }}份</div>
                <div class="rank-amount">¥{{ dish.totalAmount?.toFixed(0) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 桌台状态 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">🪑 桌台消费排行</span>
              <el-button text type="primary" @click="$router.push('/admin/tables')">
                管理桌台
              </el-button>
            </div>
          </template>
          
          <el-table :data="tableStats" size="small" stripe>
            <el-table-column type="index" width="50" />
            <el-table-column prop="tableNo" label="桌号" width="80" />
            <el-table-column prop="tableName" label="名称" />
            <el-table-column prop="orderCount" label="订单数" width="80" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.orderCount }}单</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="消费金额" width="100" align="right">
              <template #default="{ row }">
                <span class="amount-text">¥{{ row.totalAmount?.toFixed(0) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts">
export default {
  data() {
    return {
      timeRange: 'today'
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.quick-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
}

.quick-actions .el-button {
  padding: 15px 30px;
  font-size: 16px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.bg-success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.bg-primary {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.bg-warning {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
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

.stat-trend {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-text {
  font-size: 12px;
  color: #909399;
}

.middle-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.orders-card :deep(.el-card__body) {
  padding: 0;
}

.orders-list {
  max-height: 350px;
  overflow-y: auto;
  padding: 10px;
}

.order-item {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f5f7fa;
  transition: all 0.3s;
}

.order-item:hover {
  background: #e6f2ff;
}

.order-item.new-order {
  animation: pulse 2s infinite;
  background: #fff3e0;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 152, 0, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(255, 152, 0, 0); }
}

.order-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.table-no {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.order-amount {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.order-sub {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.orders-footer {
  padding: 12px;
  text-align: center;
  border-top: 1px solid #ebeef5;
}

.bottom-row {
  margin-bottom: 20px;
}

.dishes-ranking {
  padding: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 15px;
  background: #f0f2f5;
  color: #606266;
}

.rank-1 {
  background: #ffd700;
  color: #fff;
}

.rank-2 {
  background: #c0c0c0;
  color: #fff;
}

.rank-3 {
  background: #cd7f32;
  color: #fff;
}

.rank-info {
  flex: 1;
}

.rank-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.rank-bar {
  height: 6px;
  background: #ebeef5;
  border-radius: 3px;
  overflow: hidden;
}

.rank-progress {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.rank-stats {
  text-align: right;
  margin-left: 15px;
}

.rank-quantity {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.rank-amount {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.amount-text {
  font-weight: bold;
  color: #f56c6c;
}
</style>
