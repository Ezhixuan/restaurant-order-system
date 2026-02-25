<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, createCategory, updateCategory, deleteCategory, getDishes, createDish, updateDish, deleteDish, toggleDishStatus } from '@/api/dish'

// 分类管理
const categories = ref<any[]>([])
const categoryDialogVisible = ref(false)
const isEditCategory = ref(false)
const categoryForm = ref({ id: null as number | null, name: '', sortOrder: 0 })

// 菜品管理
const dishes = ref<any[]>([])
const loading = ref(false)
const dishDialogVisible = ref(false)
const isEditDish = ref(false)
const dishForm = ref({
  id: null as number | null,
  categoryId: null as number | null,
  name: '',
  description: '',
  price: 0,
  image: '',
  stock: 999,
  isRecommend: 0,
  sortOrder: 0
})

const activeTab = ref('dishes')

const loadCategories = async () => {
  categories.value = await getCategories()
}

const loadDishes = async () => {
  loading.value = true
  try {
    dishes.value = await getDishes()
  } finally {
    loading.value = false
  }
}

// 分类操作
const handleAddCategory = () => {
  isEditCategory.value = false
  categoryForm.value = { id: null, name: '', sortOrder: 0 }
  categoryDialogVisible.value = true
}

const handleEditCategory = (row: any) => {
  isEditCategory.value = true
  categoryForm.value = { ...row }
  categoryDialogVisible.value = true
}

const handleDeleteCategory = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？', '提示', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleSubmitCategory = async () => {
  if (!categoryForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (isEditCategory.value && categoryForm.value.id) {
      await updateCategory(categoryForm.value.id, categoryForm.value)
    } else {
      await createCategory(categoryForm.value)
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 菜品操作
const handleAddDish = () => {
  isEditDish.value = false
  dishForm.value = {
    id: null, categoryId: categories.value[0]?.id || null,
    name: '', description: '', price: 0, image: '',
    stock: 999, isRecommend: 0, sortOrder: 0
  }
  dishDialogVisible.value = true
}

const handleEditDish = (row: any) => {
  isEditDish.value = true
  dishForm.value = { ...row }
  dishDialogVisible.value = true
}

const handleDeleteDish = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该菜品吗？', '提示', { type: 'warning' })
    await deleteDish(row.id)
    ElMessage.success('删除成功')
    loadDishes()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleToggleStatus = async (row: any) => {
  await toggleDishStatus(row.id)
  ElMessage.success('状态更新成功')
  loadDishes()
}

const handleSubmitDish = async () => {
  if (!dishForm.value.name || !dishForm.value.categoryId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEditDish.value && dishForm.value.id) {
      await updateDish(dishForm.value.id, dishForm.value)
    } else {
      await createDish(dishForm.value)
    }
    dishDialogVisible.value = false
    loadDishes()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const getCategoryName = (id: number) => {
  return categories.value.find(c => c.id === id)?.name || '-'
}

onMounted(() => {
  loadCategories()
  loadDishes()
})
</script>

<template>
  <div class="dishes-page">
    <el-tabs v-model="activeTab">
      <!-- 菜品列表 -->
      <el-tab-pane label="菜品管理" name="dishes">
        <div class="header">
          <h3>菜品列表</h3>
          <el-button type="primary" @click="handleAddDish">添加菜品</el-button>
        </div>
        
        <el-table :data="dishes" v-loading="loading" border>
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column prop="categoryId" label="分类" width="120">
            <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="80" />
          <el-table-column prop="isRecommend" label="推荐" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isRecommend" type="success">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditDish(row)">编辑</el-button>
              <el-button size="small" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button size="small" type="danger" @click="handleDeleteDish(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 分类管理 -->
      <el-tab-pane label="分类管理" name="categories">
        <div class="header">
          <h3>分类列表</h3>
          <el-button type="primary" @click="handleAddCategory">添加分类</el-button>
        </div>
        
        <el-table :data="categories" border>
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="sortOrder" label="排序" width="100" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditCategory(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 分类对话框 -->
    <el-dialog v-model="categoryDialogVisible" :title="isEditCategory ? '编辑分类' : '添加分类'">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- 菜品对话框 -->
    <el-dialog v-model="dishDialogVisible" :title="isEditDish ? '编辑菜品' : '添加菜品'" width="600px">
      <el-form :model="dishForm" label-width="100px">
        <el-form-item label="菜品名称">
          <el-input v-model="dishForm.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="dishForm.categoryId">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="dishForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="dishForm.stock" :min="-1" />
          <span class="tip">-1表示不限</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dishForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="推荐">
          <el-switch v-model="dishForm.isRecommend" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDish">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dishes-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
