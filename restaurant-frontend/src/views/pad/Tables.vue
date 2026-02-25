<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTables, createTable, openTable, clearTable } from '@/api/table'

const router = useRouter()
const tables = ref<any[]>([])
const loading = ref(false)

// 添加临时桌
const tempTableDialogVisible = ref(false)
const tempTableForm = ref({
  tableNo: '',
  name: '',
  capacity: 4
})

const loadTables = async () => {
  loading.value = true
  try {
    tables.value = await getTables()
  } catch (error) {
    ElMessage.error('加载桌台失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '空闲', 1: '使用中', 2: '待清台' }
  return map[status] || '未知'
}

const handleTableClick = async (table: any) => {
  if (table.status === 0) {
    // 开台
    try {
      const result = await ElMessageBox.prompt('请输入用餐人数', '开台', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入有效人数',
        inputValue: '2'
      })
      const value = (result as any).value
      await openTable(table.id, parseInt(value))
      ElMessage.success('开台成功')
      router.push(`/pad/order?tableId=${table.id}&customerCount=${value}`)
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '开台失败')
      }
    }
  } else if (table.status === 1) {
    // 继续点餐
    router.push(`/pad/order?tableId=${table.id}`)
  } else if (table.status === 2) {
    // 待清台
    try {
      await ElMessageBox.confirm('该桌台待清台，是否完成清台？', '提示', { type: 'warning' })
      await clearTable(table.id)
      ElMessage.success('清台成功')
      loadTables()
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '清台失败')
      }
    }
  }
}

const handleAddTempTable = () => {
  const tempCount = tables.value.filter(t => t.type === 2).length
  tempTableForm.value = {
    tableNo: `临${tempCount + 1}`,
    name: `临时${tempCount + 1}号桌`,
    capacity: 4
  }
  tempTableDialogVisible.value = true
}

const submitTempTable = async () => {
  try {
    await createTable({
      ...tempTableForm.value,
      type: 2,
      sortOrder: 999
    })
    ElMessage.success('添加成功')
    tempTableDialogVisible.value = false
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  }
}

onMounted(loadTables)
</script>

<template>
  <div class="pad-tables" v-loading="loading">
    <div class="header">
      <h2>桌台管理</h2>
      <el-button type="primary" @click="handleAddTempTable">
        <el-icon><Plus /></el-icon>添加临时桌
      </el-button>
    </div>
    
    <el-divider content-position="left">固定卡座</el-divider>
    
    <el-row :gutter="20">
      <el-col
        v-for="table in tables.filter(t => t.type === 1)"
        :key="table.id"
        :xs="12" :sm="8" :md="6"
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
        :xs="12" :sm="8" :md="6"
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

    <!-- 添加临时桌对话框 -->
    <el-dialog v-model="tempTableDialogVisible" title="添加临时桌" width="400px">
      <el-form :model="tempTableForm" label-width="80px">
        <el-form-item label="桌号">
          <el-input v-model="tempTableForm.tableNo" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="tempTableForm.name" />
        </el-form-item>
        <el-form-item label="容纳人数">
          <el-input-number v-model="tempTableForm.capacity" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tempTableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTempTable">确定</el-button>
      </template>
    </el-dialog>
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
