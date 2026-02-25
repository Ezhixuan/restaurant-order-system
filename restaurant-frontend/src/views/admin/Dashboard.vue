<script setup lang="ts">
import { ref } from 'vue'

// TODO: 实现Dashboard数据看板
const todayStats = ref({
  totalAmount: 3580,
  orderCount: 12,
  avgAmount: 298
})

const topDishes = ref([
  { name: '宫保鸡丁', count: 15, amount: 450 },
  { name: '麻婆豆腐', count: 12, amount: 216 },
  { name: '水煮鱼', count: 10, amount: 380 }
])
</script>

<template>
  <div class="dashboard">
    <h2>数据看板</h2>
    
    <!-- 今日统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card>
          <div class="stat-title">今日营业额</div>
          <div class="stat-value" style="color: #67C23A">¥{{ todayStats.totalAmount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">订单数量</div>
          <div class="stat-value" style="color: #409EFF">{{ todayStats.orderCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-title">平均客单价</div>
          <div class="stat-value" style="color: #E6A23C">¥{{ todayStats.avgAmount }}</div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 热销菜品 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>热销菜品 TOP5</span>
      </template>
      
      <el-table :data="topDishes" style="width: 100%">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="name" label="菜品名称" />
        <el-table-column prop="count" label="销量" width="120" />
        <el-table-column prop="amount" label="销售额" width="120">
          <template #default="{ row }">
            ¥{{ row.amount }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
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
