<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTables, createTable, openTable, clearTable, setPendingClear } from '@/api/table'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const tables = ref<any[]>([])
const loading = ref(false)

// 筛选条件
const areaFilter = ref('all')
const statusFilter = ref<number | null>(null)
const searchKeyword = ref('')

// 添加临时桌对话框
const tempTableDialogVisible = ref(false)
const tempTableForm = ref({
  tableNo: '',
  name: '',
  capacity: 4
})

// 使用中桌台选择对话框
const busyTableDialogVisible = ref(false)
const selectedBusyTable = ref<any>(null)

// 待清台结账对话框
const checkoutDialogVisible = ref(false)
const checkoutTable = ref<any>(null)
const checkoutOrder = ref<any>(null)

// 结账金额相关
const actualPayAmount = ref<number>(0)
const discountAmount = computed(() => {
  if (!checkoutOrder.value) return 0
  return (checkoutOrder.value.payAmount || 0) - (actualPayAmount.value || 0)
})

// 处理抹零 - 去掉个位数（如164→160）
const handleRoundDown = () => {
  if (!checkoutOrder.value) return
  const originalAmount = Math.floor(checkoutOrder.value.payAmount || 0)
  // 去掉个位数，向下取整到十位
  actualPayAmount.value = Math.floor(originalAmount / 10) * 10
}

// 重置实付金额
const resetPayAmount = () => {
  if (checkoutOrder.value) {
    actualPayAmount.value = Math.floor(checkoutOrder.value.payAmount || 0)
  }
}

// 清台确认对话框
const clearTableDialogVisible = ref(false)
const selectedClearTable = ref<any>(null)

const loadTables = async () => {
  loading.value = true
  try {
    const res = await getTables()
    tables.value = res || []
    selectedClearTable.value = null
  } catch (error) {
    ElMessage.error('加载桌台失败')
  } finally {
    loading.value = false
  }
}

// 筛选后的桌台
const filteredTables = computed(() => {
  let result = tables.value
  
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
  
  if (statusFilter.value !== null) {
    result = result.filter(t => t.status === statusFilter.value)
  }
  
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

// 点击桌台
const handleTableClick = async (table: any) => {
  if (table.status === 0) {
    // 空闲 - 直接开台
    handleOpenTableDialog(table)
  } else if (table.status === 1) {
    // 使用中 - 显示选项对话框
    selectedBusyTable.value = table
    busyTableDialogVisible.value = true
  } else if (table.status === 2) {
    // 待清台 - 显示结账对话框
    handleCheckoutDialog(table)
  }
}

// 处理清台
const handleClearTable = async (table: any) => {
  selectedClearTable.value = table
  
  if (table.status === 1) {
    // 使用中 -> 调用 setPendingClear 进入待清台状态
    try {
      await setPendingClear(table.id)
      ElMessage.success('已进入待清台状态，请再次点击确认清台')
      loadTables()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  } else if (table.status === 2) {
    // 已经是待清台状态 -> 显示确认对话框
    clearTableDialogVisible.value = true
  }
}

// 确认清台
const confirmClearTable = async () => {
  if (!selectedClearTable.value) return
  
  try {
    // 1. 获取该桌台的所有未完成订单并标记为完成
    const { getActiveOrders, completeOrder } = await import('@/api/order')
    const orders = await getActiveOrders()
    const tableOrders = orders.filter((o: any) => o.tableId === selectedClearTable.value.id && o.status < 3)
    
    // 批量完成订单
    for (const order of tableOrders) {
      try {
        await completeOrder(order.id)
      } catch (e) {
        console.log('订单可能已完成:', order.id)
      }
    }
    
    // 2. 调用清台接口恢复桌台为空闲状态
    await clearTable(selectedClearTable.value.id)
    
    ElMessage.success('清台成功')
    clearTableDialogVisible.value = false
    selectedClearTable.value = null
    loadTables()
  } catch (error: any) {
    ElMessage.error(error.message || '清台失败')
  }
}

// 开台对话框
const openTableDialogVisible = ref(false)
const selectedTable = ref<any>(null)
const customerCount = ref(2)

const handleOpenTableDialog = (table: any) => {
  selectedTable.value = table
  customerCount.value = table.capacity > 4 ? 4 : table.capacity
  openTableDialogVisible.value = true
}

const confirmOpenTable = async () => {
  if (!selectedTable.value) return
  try {
    await openTable(selectedTable.value.id, customerCount.value)
    
    // 先清空购物车，再设置新的桌台信息
    cartStore.clearCart()
    // 设置购物车信息（在clearCart之后设置，避免被重置）
    cartStore.setTableInfo(selectedTable.value.id, selectedTable.value.tableNo, customerCount.value)
    
    ElMessage.success('开台成功')
    openTableDialogVisible.value = false
    
    // 跳转到点餐页面（新建订单模式）
    router.push({
      path: '/pad/order',
      query: { 
        mode: 'new',
        tableId: selectedTable.value.id,
        tableNo: selectedTable.value.tableNo,
        customerCount: customerCount.value
      }
    })
  } catch (error: any) {
    ElMessage.error(error.message || '开台失败')
  }
}

// 继续点餐（加菜）
const continueOrder = async () => {
  if (!selectedBusyTable.value) return
  
  // 获取当前桌台的订单信息
  try {
    const { getOrderByTable } = await import('@/api/order')
    const orderData = await getOrderByTable(selectedBusyTable.value.id)
    
    if (!orderData || !orderData.order) {
      ElMessage.warning('未找到该桌台的订单信息')
      return
    }
    
    cartStore.setTableInfo(selectedBusyTable.value.id, selectedBusyTable.value.tableNo, orderData.order.customerCount || 1)
    cartStore.clearCart()
    
    busyTableDialogVisible.value = false
    router.push({
      path: '/pad/order',
      query: { 
        mode: 'add',
        orderId: orderData.order.id,
        tableId: selectedBusyTable.value.id,
        tableNo: selectedBusyTable.value.tableNo,
        customerCount: orderData.order.customerCount || 1
      }
    })
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单信息失败')
  }
}

// 查看订单
const viewOrder = () => {
  if (!selectedBusyTable.value) return
  
  busyTableDialogVisible.value = false
  router.push({
    path: '/pad/order-detail',
    query: { tableId: selectedBusyTable.value.id }
  })
}

// 结账对话框
const handleCheckoutDialog = async (table: any) => {
  checkoutTable.value = table
  checkoutDialogVisible.value = true
  
  // 获取该桌台的订单信息
  try {
    const { getOrderByTable, getUnpaidAmount } = await import('@/api/order')
    const orderData = await getOrderByTable(table.id)
    if (orderData && orderData.order) {
      const unpaidAmount = await getUnpaidAmount(orderData.order.id)
      const payAmount = unpaidAmount || orderData.order.payAmount
      checkoutOrder.value = {
        orderNo: orderData.order.orderNo,
        totalAmount: orderData.order.totalAmount,
        payAmount: payAmount
      }
      // 初始化实付金额（直接取整数）
      actualPayAmount.value = Math.floor(payAmount)
    } else {
      ElMessage.warning('未找到该桌台的订单信息')
      checkoutDialogVisible.value = false
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取订单信息失败')
    checkoutDialogVisible.value = false
  }
}

// 确认结账
const confirmCheckout = async () => {
  try {
    // 调用清台接口
    if (checkoutTable.value) {
      await clearTable(checkoutTable.value.id)
      ElMessage.success('结账成功，桌台已清台')
      checkoutDialogVisible.value = false
      loadTables()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '结账失败')
  }
}

// 跳转到菜品管理
const goToDishManagement = () => {
  router.push('/pad/dishes')
}

// 跳转到订单页面
const goToOrders = () => {
  router.push('/pad/orders')
}

// 其他原有方法...
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

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 
    0: 'success',   // 空闲
    1: 'danger',    // 使用中
    2: 'warning'    // 待清台
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 
    0: '空闲', 
    1: '使用中', 
    2: '待清台'
  }
  return map[status] || '未知'
}

onMounted(loadTables)
</script>

<template>
  <div class="pad-tables" v-loading="loading">
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
          
          <el-button type="primary" size="large" @click="handleAddTempTable">
            <el-icon><Plus /></el-icon>添加临时桌
          </el-button>
          
          <el-button type="success" size="large" @click="goToDishManagement">
            <el-icon><Food /></el-icon>菜品管理
          </el-button>
          
          <el-button type="warning" size="large" @click="goToOrders">
            <el-icon><Document /></el-icon>查看订单
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
          
          <!-- 使用中和待清台状态显示清台按钮 -->
          <div v-if="table.status === 1 || table.status === 2" class="clear-table-btn">
            <el-button
              :type="table.status === 2 ? 'danger' : 'warning'"
              size="small"
              @click.stop="handleClearTable(table)"
            >
              {{ table.status === 2 ? '确认清台' : '清台' }}
            </el-button>
          </div>
        </div>
      </div>
      
      <el-empty v-if="filteredTables.length === 0" description="暂无桌台数据" style="grid-column: 1/-1;" />
    </div>

    <!-- 开台对话框 -->
    <el-dialog v-model="openTableDialogVisible" title="开台" width="400px">
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
        <el-button @click="openTableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOpenTable">确认开台</el-button>
      </template>
    </el-dialog>

    <!-- 使用中桌台选择对话框 -->
    <el-dialog v-model="busyTableDialogVisible" title="桌台操作中" width="400px">
      <div v-if="selectedBusyTable" class="busy-table-info">
        <div class="info-header">
          <el-icon :size="48" color="#f56c6c"><UserFilled /></el-icon>
          <div class="info-title">{{ selectedBusyTable.name }}</div>
          <div class="info-subtitle">{{ selectedBusyTable.tableNo }} · {{ selectedBusyTable.capacity }}人桌</div>
        </div>
        
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="continueOrder">
            <el-icon><Plus /></el-icon>
            继续点餐（加菜）
          </el-button>
          
          <el-button type="success" size="large" @click="viewOrder">
            <el-icon><Document /></el-icon>
            查看订单详情
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 结账对话框 -->
    <el-dialog v-model="checkoutDialogVisible" title="结账" width="500px">
      <div v-if="checkoutTable && checkoutOrder" class="checkout-info">
        <div class="checkout-header">
          <div class="checkout-title">{{ checkoutTable.name }} 账单</div>
          <div class="checkout-order">订单号：{{ checkoutOrder.orderNo }}</div>
        </div>
        
        <el-divider />
        
        <!-- 应收金额 -->
        <div class="checkout-row">
          <span class="label">应收金额</span>
          <span class="value original">¥{{ Math.floor(checkoutOrder.payAmount || 0) }}</span>
        </div>
        
        <!-- 实付金额输入 -->
        <div class="checkout-row pay-input-row">
          <span class="label">实付金额</span>
          <div class="pay-input-wrapper">
            <el-input-number
              v-model="actualPayAmount"
              :min="0"
              :max="Math.floor((checkoutOrder.payAmount || 0) * 2)"
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
        
        <!-- 确认金额 -->
        <div class="checkout-row final-row">
          <span class="label">确认收款</span>
          <span class="value final">¥{{ Math.floor(actualPayAmount) }}</span>
        </div>
        
        <div class="checkout-actions">
          <el-alert
            title="确认收款后，桌台将自动清台"
            type="warning"
            :closable="false"
            style="margin-bottom: 15px"
          />
          
          <el-button type="primary" size="large" style="width: 100%" @click="confirmCheckout">
            确认收款并清台
          </el-button>
        </div>
      </div>
    </el-dialog>

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

    <!-- 清台确认对话框 -->
    <el-dialog v-model="clearTableDialogVisible" title="确认清台" width="450px" center>
      <div class="clear-table-confirm">
        <el-icon :size="64" color="#e6a23c"><Warning /></el-icon>
        <div class="confirm-title">确认要清台吗？</div>
        <div class="confirm-subtitle">{{ selectedClearTable?.name }} ({{ selectedClearTable?.tableNo }})</div>
        <el-alert
          title="清台后将完成该桌台所有未结账订单，并将桌台恢复为空闲状态"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 15px; text-align: left;"
        />
      </div>
      
      <template #footer>
        <el-button @click="clearTableDialogVisible = false">取消</el-button>
        <el-button type="danger" size="large" @click="confirmClearTable">确认清台</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pad-tables {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
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
}

/* 桌台网格 */
.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
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

.bg-success { background: #67c23a; }
.bg-danger { background: #f56c6c; }
.bg-warning { background: #e6a23c; }

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
}

.table-meta {
  display: flex;
  gap: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #909399;
}

.clear-table-btn {
  margin-top: 12px;
  text-align: center;
}

.clear-table-btn .el-button {
  width: 100%;
}

/* 清台确认对话框 */
.clear-table-confirm {
  text-align: center;
  padding: 20px;
}

.confirm-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-top: 15px;
}

.confirm-subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

/* 对话框样式 */
.open-table-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
}

.info-row .label {
  color: #909399;
  width: 60px;
}

.info-row .value {
  color: #303133;
  font-weight: 500;
}

.busy-table-info {
  text-align: center;
}

.info-header {
  margin-bottom: 30px;
}

.info-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-top: 15px;
}

.info-subtitle {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-buttons .el-button {
  padding: 20px;
  font-size: 16px;
}

.checkout-info {
  padding: 10px;
}

.checkout-header {
  text-align: center;
  margin-bottom: 20px;
}

.checkout-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.checkout-order {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
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

.checkout-actions {
  margin-top: 20px;
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

@media (max-width: 768px) {
  .tables-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
