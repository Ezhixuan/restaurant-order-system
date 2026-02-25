<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// TODO: 实现桌台列表
const tables = ref([
  { id: 1, tableNo: 'A01', name: '包厢1', type: 1, status: 0, capacity: 8 },
  { id: 2, tableNo: 'A02', name: '包厢2', type: 1, status: 1, capacity: 6 },
  { id: 3, tableNo: 'B01', name: '卡座1', type: 1, status: 0, capacity: 4 },
  { id: 4, tableNo: '临1', name: '临时1号桌', type: 2, status: 0, capacity: 4 }
])

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'success',
    1: 'danger',
    2: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '空闲',
    1: '使用中',
    2: '待清台'
  }
  return map[status] || '未知'
}

const handleTableClick = (table: any) => {
  if (table.status === 0) {
    // 开台
    router.push(`/pad/order?tableId=${table.id}`)
  }
}

const addTempTable = () => {
  // TODO: 添加临时桌
  const nextNum = tables.value.filter(t => t.type === 2).length + 1
  tables.value.push({
    id: Date.now(),
    tableNo: `临${nextNum}`,
    name: `临时${nextNum}号桌`,
    type: 2,
    status: 0,
    capacity: 4
  })
}
</script>

<template>
  <div class="pad-tables">
    <div class="header">
      <h2>桌台管理</h2>
      <el-button type="primary" @click="addTempTable">
        <el-icon><Plus /></el-icon>
        添加临时桌
      </el-button>
    </div>
    
    <el-divider content-position="left">固定卡座</el-divider>
    
    <el-row :gutter="20">
      <el-col
        v-for="table in tables.filter(t => t.type === 1)"
        :key="table.id"
        :span="6"
        style="margin-bottom: 20px"
      >
        <el-card
          :class="['table-card', { 'table-busy': table.status === 1 }]"
          shadow="hover"
          @click="handleTableClick(table)"
        >
          <div class="table-header">
            <span class="table-name">{{ table.name }}</span>
            <el-tag :type="getStatusType(table.status)" size="small">
              {{ getStatusText(table.status) }}
            </el-tag>
          </div>
          <div class="table-info">
            桌号: {{ table.tableNo }} | 容纳: {{ table.capacity }}人
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-divider content-position="left">临时座位</el-divider>
    
    <el-row :gutter="20">
      <el-col
        v-for="table in tables.filter(t => t.type === 2)"
        :key="table.id"
        :span="6"
        style="margin-bottom: 20px"
      >
        <el-card
          :class="['table-card', 'temp-table', { 'table-busy': table.status === 1 }]"
          shadow="hover"
          @click="handleTableClick(table)"
        >
          <div class="table-header">
            <span class="table-name">{{ table.name }}</span>
            <el-tag :type="getStatusType(table.status)" size="small">
              {{ getStatusText(table.status) }}
            </el-tag>
          </div>
          <div class="table-info">
            桌号: {{ table.tableNo }} | 容纳: {{ table.capacity }}人
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.pad-tables {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-card {
  cursor: pointer;
  transition: all 0.3s;
}

.table-card:hover {
  transform: translateY(-5px);
}

.table-busy {
  border: 2px solid #F56C6C;
}

.temp-table {
  background: #fdf6ec;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.table-name {
  font-size: 18px;
  font-weight: bold;
}

.table-info {
  font-size: 14px;
  color: #909399;
}
</style>
