<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTables, createTable, updateTable, deleteTable, openTable, clearTable } from '@/api/table'
import { useRouter } from 'vue-router'

const router = useRouter()
const tables = ref<any[]>([])
const loading = ref(false)

// 筛选条件
const areaFilter = ref('all')
const statusFilter = ref<number | null>(null)
const searchKeyword = ref('')

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null as number | null,
  tableNo: '',
  name: '',
  type: 1,
  capacity: 4,
  sortOrder: 0
})

// 开台对话框
const openTableVisible = ref(false)
const selectedTable = ref<any>(null)
const customerCount = ref(2)

const loadTables = async () => {
  loading.value = true
  try {
    const res = await getTables()
    tables.value = res || []
  } catch (error) {
    ElMessage.error('加载桌台失败')
  } finally {
    loading.value = false
  }
}

// 筛选后的桌台
const filteredTables = computed(() => {
  let result = tables.value
  
  // 区域筛选
  if (areaFilter.value !== 'all') {
    if (areaFilter.value === 'fixed') {
      result = result.filter(t => t.type === 1)
    } else if (areaFilter.value === 'temp') {
      result = result.filter(t => t.type === 2)
    } else if (areaFilter.value === 'vip') {
      result = result.filter(t => t.tableNo.startsWith('A'))
    } else if (areaFilter.value === 'hall') {
      result = result.filter(t => t.tableNo.startsWith('B'))
    }
  }
  
  // 状态筛选
  if (statusFilter.value !== null) {
    result = result.filter(t => t.status === statusFilter.value)
  }
  
  // 搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(t => 
      t.tableNo.toLowerCase().includes(keyword) ||
      t.name.toLowerCase().includes(keyword)
    )
  }
  
  return result.sort((a, b) => a.sortOrder - b.sortOrder)
})

// 统计
const stats = computed(() => {
  const total = tables.value.length
  const free = tables.value.filter(t => t.status === 0).length
  const busy = tables.value.filter(t => t.status === 1).length
  const pending = tables.value.filter(t => t.status === 2).length
  return { total, free, busy, pending }
})

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    tableNo: '',
    name: '',
    type: 1,
    capacity: 4,
    sortOrder: 0
  }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除桌台 ${row.name} 吗？`, '提示', { type: 'warning' })
    await deleteTable(row.id)
    ElMessage.success('删除成功')
    loadTables()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!form.value.tableNo) {
    ElMessage.warning('请输入桌号')
    return
  }
  if (!form.value.name) {
    ElMessage.warning('请输入桌台名称')
    return
  }
  try {
    if (isEdit.value && form.value.id) {
      await updateTable(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createTable(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 点击桌台卡片
const handleTableClick = (table: any) => {
  if (table.status === 0) {
    // 空闲 - 开台
    selectedTable.value = table
    customerCount.value = table.capacity > 4 ? 4 : table.capacity
    openTableVisible.value = true
  } else if (table.status === 1) {
    // 使用中 - 查看详情
    ElMessage.info(`${table.name} 正在使用中`)
  } else if (table.status === 2) {
    // 待清台 - 清台
    handleClearTable(table)
  }
}

// 开台
const handleOpenTable = async () => {
  if (!selectedTable.value) return
  try {
    await openTable(selectedTable.value.id, customerCount.value)
    ElMessage.success('开台成功')
    openTableVisible.value = false
    loadTables()
    
    // 可以跳转到点餐页面
    // router.push(`/pad/order?tableId=${selectedTable.value.id}`)
  } catch (error: any) {
    ElMessage.error(error.message || '开台失败')
  }
}

// 清台
const handleClearTable = async (table: any) => {
  try {
    await ElMessageBox.confirm(`${table.name} 待清台，是否确认清台？`, '确认清台', { type: 'warning' })
    await clearTable(table.id)
    ElMessage.success('清台成功')
    loadTables()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清台失败')
    }
  }
}

// 生成建议桌号
const generateTableNo = () => {
  const existingFixed = tables.value.filter(t => t.type === 1)
  const existingTemp = tables.value.filter(t => t.type === 2)
  
  if (form.value.type === 1) {
    // 固定桌号建议
    const usedLetters = [...new Set(existingFixed.map(t => t.tableNo.charAt(0)))]
    const letter = usedLetters.length > 0 ? usedLetters[0] : 'A'
    const maxNum = Math.max(...existingFixed.filter(t => t.tableNo.startsWith(letter)).map(t => parseInt(t.tableNo.slice(1)) || 0), 0)
    form.value.tableNo = `${letter}${String(maxNum + 1).padStart(2, '0')}`
  } else {
    // 临时桌号建议
    const maxTemp = Math.max(...existingTemp.map(t => parseInt(t.tableNo.replace('临', '')) || 0), 0)
    form.value.tableNo = `临${maxTemp + 1}`
  }
}

// 监听类型变化，自动更新名称和桌号
const onTypeChange = () => {
  if (!isEdit.value) {
    generateTableNo()
    if (form.value.type === 2) {
      form.value.name = `临时${form.value.tableNo.replace('临', '')}号桌`
    }
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '空闲', 1: '使用中', 2: '待清台' }
  return map[status] || '未知'
}

const getStatusIcon = (status: number) => {
  const map: Record<number, string> = { 0: 'CircleCheck', 1: 'UserFilled', 2: 'Warning' }
  return map[status] || 'InfoFilled'
}

onMounted(loadTables)
</script>

<template>
  <div class="tables-page">
    <!-- 统计卡片 -->
    <el-row :gutter="15" class="stats-row">
      <el-col :xs="12" :sm="6" :lg="3">
        <div class="stat-item total">
          <div class="stat-icon"><el-icon :size="24"><OfficeBuilding /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.total }}</div>
            <div class="stat-label">总桌台</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :lg="3">
        <div class="stat-item free">
          <div class="stat-icon"><el-icon :size="24"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.free }}</div>
            <div class="stat-label">空闲</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :lg="3">
        <div class="stat-item busy">
          <div class="stat-icon"><el-icon :size="24"><UserFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.busy }}</div>
            <div class="stat-label">使用中</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :lg="3">
        <div class="stat-item pending">
          <div class="stat-icon"><el-icon :size="24"><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.pending }}</div>
            <div class="stat-label">待清台</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <div class="filter-left">
          <el-radio-group v-model="areaFilter" size="large">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="vip">包厢</el-radio-button>
            <el-radio-button label="hall">大厅</el-radio-button>
            <el-radio-button label="fixed">固定座位</el-radio-button>
            <el-radio-button label="temp">临时座位</el-radio-button>
          </el-radio-group>
          
          <el-divider direction="vertical" />
          
          <el-radio-group v-model="statusFilter" size="large">
            <el-radio-button :label="null">全部状态</el-radio-button>
            <el-radio-button :label="0">空闲</el-radio-button>
            <el-radio-button :label="1">使用中</el-radio-button>
            <el-radio-button :label="2">待清台</el-radio-button>
          </el-radio-group>
        </div>
        
        <div class="filter-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索桌号/名称"
            clearable
            style="width: 200px"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          
          <el-button type="primary" size="large" @click="handleAdd">
            <el-icon><Plus /></el-icon>添加桌台
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 桌台网格 -->
    <div class="tables-grid" v-loading="loading">
      <div
        v-for="table in filteredTables"
        :key="table.id"
        class="table-card"
        :class="[`status-${table.status}`, { 'temp-table': table.type === 2 }]"
        @click="handleTableClick(table)"
      >
        <div class="table-status-bar" :class="`bg-${getStatusType(table.status)}`"></div>
        
        <div class="table-content">
          <div class="table-header">
            <span class="table-no">{{ table.tableNo }}</span>
            <el-tag :type="getStatusType(table.status)" size="small" effect="dark">
              {{ getStatusLabel(table.status) }}
            </el-tag>
          </div>
          
          <div class="table-name">{{ table.name }}</div>
          
          <div class="table-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ table.capacity }}人
            </span>
            <span class="meta-item" v-if="table.type === 2">
              <el-icon><Timer /></el-icon>
              临时
            </span>
          </div>
          
          <div class="table-icon">
            <el-icon :size="48" :class="`text-${getStatusType(table.status)}`">
              <component :is="getStatusIcon(table.status)" />
            </el-icon>
          </div>
        </div>
        
        <div class="table-actions" @click.stop>
          <el-button type="primary" link size="small" @click="handleEdit(table)">
            编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(table)">
            删除
          </el-button>
        </div>
      </div>
      
      <!-- 空状态 -->
      <el-empty v-if="filteredTables.length === 0" description="暂无桌台数据" style="grid-column: 1/-1;" />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑桌台' : '添加桌台'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type" @change="onTypeChange" :disabled="isEdit">
            <el-radio-button :label="1">固定卡座</el-radio-button>
            <el-radio-button :label="2">临时座位</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="桌号">
          <el-input v-model="form.tableNo" placeholder="如：A01、临1">
            <template #append v-if="!isEdit">
              <el-button @click="generateTableNo">自动生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="桌台名称">
          <el-input v-model="form.name" placeholder="如：包厢1、路边1号桌" />
        </el-form-item>
        
        <el-form-item label="容纳人数">
          <el-input-number v-model="form.capacity" :min="1" :max="50" style="width: 150px" />
          <span class="form-tip">人</span>
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 150px" />
          <span class="form-tip">数字越小排序越靠前</span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 开台对话框 -->
    <el-dialog v-model="openTableVisible" title="开台" width="400px">
      <div v-if="selectedTable" class="open-table-info">
        <div class="info-row">
          <span class="label">桌台：</span>
          <span class="value">{{ selectedTable.name }} ({{ selectedTable.tableNo }})</span>
        </div>
        <div class="info-row">
          <span class="label">容量：</span>
          <span class="value">{{ selectedTable.capacity }}人</span>
        </div>
      </div>
      
      <el-form label-width="100px" style="margin-top: 20px">
        <el-form-item label="用餐人数">
          <el-input-number v-model="customerCount" :min="1" :max="selectedTable?.capacity || 20" size="large" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="openTableVisible = false">取消</el-button>
        <el-button type="primary" @click="handleOpenTable">确认开台</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tables-page {
  padding: 20px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-item.total .stat-icon {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
}

.stat-item.free .stat-icon {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #388e3c;
}

.stat-item.busy .stat-icon {
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  color: #d32f2f;
}

.stat-item.pending .stat-icon {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #f57c00;
}

.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

/* 筛选栏 */
.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 桌台网格 */
.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.table-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.table-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.table-card.temp-table {
  background: linear-gradient(135deg, #fff8e1 0%, #fff 100%);
}

.table-status-bar {
  height: 4px;
  width: 100%;
}

.bg-success {
  background: #67c23a;
}

.bg-danger {
  background: #f56c6c;
}

.bg-warning {
  background: #e6a23c;
}

.table-content {
  padding: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-no {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.table-name {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #909399;
}

.table-icon {
  display: flex;
  justify-content: center;
  padding: 15px 0;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.text-warning {
  color: #e6a23c;
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 12px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
}

/* 开台信息 */
.open-table-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row .label {
  color: #909399;
  width: 60px;
}

.info-row .value {
  color: #303133;
  font-weight: 500;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 768px) {
  .tables-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-left {
    width: 100%;
  }
  
  .filter-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
