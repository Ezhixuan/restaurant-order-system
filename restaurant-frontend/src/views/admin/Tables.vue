<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTables, createTable, updateTable, deleteTable } from '@/api/table'

const tables = ref<any[]>([])
const loading = ref(false)
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
    await ElMessageBox.confirm('确定删除该桌台吗？', '提示', { type: 'warning' })
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

const getTypeTag = (type: number) => {
  return type === 1 ? 'primary' : 'warning'
}

const getTypeLabel = (type: number) => {
  return type === 1 ? '固定卡座' : '临时座位'
}

const getStatusTag = (status: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'danger', 2: 'warning' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '空闲', 1: '使用中', 2: '待清台' }
  return map[status] || '未知'
}

onMounted(loadTables)
</script>

<template>
  <div class="tables-page">
    <div class="header">
      <h2>桌台管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>添加桌台
      </el-button>
    </div>

    <el-table :data="tables" v-loading="loading" border>
      <el-table-column prop="tableNo" label="桌号" width="100" />
      <el-table-column prop="name" label="桌台名称" />
      
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="capacity" label="容纳人数" width="100" />
      
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑桌台' : '添加桌台'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="桌号">
          <el-input v-model="form.tableNo" placeholder="如：A01、临1" :disabled="isEdit" />
        </el-form-item>
        
        <el-form-item label="桌台名称">
          <el-input v-model="form.name" placeholder="如：包厢1、路边1号桌" />
        </el-form-item>
        
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">固定卡座</el-radio>
            <el-radio :label="2">临时座位</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="容纳人数">
          <el-input-number v-model="form.capacity" :min="1" :max="20" />
        </el-form-item>
        
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tables-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
