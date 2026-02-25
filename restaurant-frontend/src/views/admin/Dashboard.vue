<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTodayStats, getTopDishes, getTableStats } from '@/api/report'

const todayStats = ref({
  totalRevenue: 0,
  orderCount: 0,
  avgAmount: 0
})

const topDishes = ref<any[]>([])
const tableStats = ref<any[]>([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    todayStats.value = await getTodayStats()
    topDishes.value = await getTopDishes(5)
    tableStats.value = await getTableStats()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <h2>数据看板</h2>
    
    <!-- 今日统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card>
          <div class="stat-title">今日营业额</div>
          <div class="stat-value" style="color: #67C23A">¥{{ todayStats.totalRevenue?.toFixed(2) || '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">订单数量</div>
          <div class="stat-value" style="color: #409EFF">{{ todayStats.orderCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">平均客单价</div>
          <div class="stat-value" style="color: #E6A23C">¥{{ todayStats.avgAmount?.toFixed(2) || '0.00' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 热销菜品 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>热销菜品 TOP5</span>
          </template>
          
          <el-table :data="topDishes" style="width: 100%">
            <el-table-column type="index" label="排名" width="80" />
            <el-table-column prop="dishName" label="菜品名称" />
            <el-table-column prop="totalQuantity" label="销量" width="100" />
            <el-table-column prop="totalAmount" label="销售额" width="120">
              <template #default="{ row }">
                ¥{{ row.totalAmount?.toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 桌台消费 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>各桌消费统计</span>
          </template>
          
          <el-table :data="tableStats" style="width: 100%">
            <el-table-column prop="tableNo" label="桌号" width="100" />
            <el-table-column prop="tableName" label="桌台名称" />
            <el-table-column prop="orderCount" label="订单数" width="100" />
            <el-table-column prop="totalAmount" label="消费金额" width="120">
              <template #default="{ row }">
                ¥{{ row.totalAmount?.toFixed(2) || '0.00' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
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
</style>
