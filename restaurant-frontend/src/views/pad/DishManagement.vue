<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as dishApi from '@/api/dish'
import * as dishSpecApi from '@/api/dishSpec'
import type { SpecItem } from '@/api/dishSpec'

const router = useRouter()
const loading = ref(false)
const categories = ref<any[]>([])
const dishes = ref<any[]>([])
const activeCategory = ref(0)

// 分类编辑
const categoryDialogVisible = ref(false)
const isEditCategory = ref(false)
const categoryForm = ref({ id: 0, name: '', sortOrder: 0 })

// 菜品编辑
const dishDialogVisible = ref(false)
const isEditDish = ref(false)
const dishForm = ref({
  id: 0,
  categoryId: 0,
  name: '',
  description: '',
  price: 0,
  image: '',
  stock: 999,
  sortOrder: 0
})

// 规格配置
const specDialogVisible = ref(false)
const currentDish = ref<any>(null)
const specList = ref<SpecItem[]>([])
const hasSpecs = ref(false)

// 计算属性
const filteredDishes = computed(() => {
  if (activeCategory.value === 0) return dishes.value
  return dishes.value.filter(d => d.categoryId === activeCategory.value)
})

// 加载数据
const loadCategories = async () => {
  categories.value = await dishApi.getCategories()
}

const loadDishes = async () => {
  const data = await dishApi.getDishesByCategory()
  dishes.value = data.flatMap((cat: any) => 
    (cat.dishes || []).map((dish: any) => ({
      ...dish,
      categoryId: cat.id,
      categoryName: cat.name
    }))
  )
}

// 返回桌台页面
const goBack = () => {
  router.push('/pad/tables')
}

// 初始化
onMounted(async () => {
  loading.value = true
  await Promise.all([loadCategories(), loadDishes()])
  loading.value = false
})

// 分类操作
const showAddCategory = () => {
  isEditCategory.value = false
  categoryForm.value = { id: 0, name: '', sortOrder: 0 }
  categoryDialogVisible.value = true
}

const showEditCategory = (cat: any) => {
  isEditCategory.value = true
  categoryForm.value = { ...cat }
  categoryDialogVisible.value = true
}

const submitCategory = async () => {
  if (!categoryForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (isEditCategory.value) {
      await dishApi.updateCategory(categoryForm.value.id, categoryForm.value)
    } else {
      await dishApi.createCategory(categoryForm.value)
    }
    categoryDialogVisible.value = false
    await loadCategories()
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleDeleteCategory = async (cat: any) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？分类下的菜品将无分类', '提示', { type: 'warning' })
    await dishApi.deleteCategory(cat.id)
    await loadCategories()
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 菜品操作
const showAddDish = () => {
  isEditDish.value = false
  dishForm.value = {
    id: 0,
    categoryId: activeCategory.value || categories.value[0]?.id || 0,
    name: '',
    description: '',
    price: 0,
    image: '',
    stock: 999,
    sortOrder: 0
  }
  dishDialogVisible.value = true
}

const showEditDish = (dish: any) => {
  isEditDish.value = true
  dishForm.value = { ...dish }
  dishDialogVisible.value = true
}

const submitDish = async () => {
  if (!dishForm.value.name || !dishForm.value.categoryId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEditDish.value) {
      await dishApi.updateDish(dishForm.value.id, dishForm.value)
    } else {
      await dishApi.createDish(dishForm.value)
    }
    dishDialogVisible.value = false
    await loadDishes()
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleDeleteDish = async (dish: any) => {
  try {
    await ElMessageBox.confirm(`确定删除菜品 "${dish.name}" 吗？`, '提示', { type: 'warning' })
    await dishApi.deleteDish(dish.id)
    await loadDishes()
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleToggleStatus = async (dish: any) => {
  try {
    await dishApi.toggleDishStatus(dish.id)
    await loadDishes()
    ElMessage.success(dish.status === 1 ? '已下架' : '已上架')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 快速修改价格
const handlePriceChange = async (dish: any, newPrice: number) => {
  try {
    await dishApi.updateDishPrice(dish.id, newPrice)
    dish.price = newPrice
    ElMessage.success('价格已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '修改失败')
  }
}

// 规格配置
const showSpecConfig = async (dish: any) => {
  currentDish.value = dish
  hasSpecs.value = dish.hasSpecs === 1
  
  if (hasSpecs.value) {
    try {
      const detail = await dishApi.getDishDetail(dish.id)
      specList.value = detail.specs || []
    } catch (error) {
      specList.value = []
    }
  } else {
    specList.value = []
  }
  
  specDialogVisible.value = true
}

const addSpec = () => {
  specList.value.push({
    name: '',
    price: 0,
    sortOrder: specList.value.length,
    status: 1
  })
}

const removeSpec = (index: number) => {
  specList.value.splice(index, 1)
  specList.value.forEach((spec, idx) => {
    spec.sortOrder = idx
  })
}

const moveSpec = (index: number, direction: number) => {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= specList.value.length) return
  
  const temp = specList.value[index]
  specList.value[index] = specList.value[newIndex]
  specList.value[newIndex] = temp
  
  specList.value.forEach((spec, idx) => {
    spec.sortOrder = idx
  })
}

const submitSpecs = async () => {
  if (hasSpecs.value) {
    const validSpecs = specList.value.filter(s => s.name && s.price > 0)
    if (validSpecs.length === 0) {
      ElMessage.warning('请至少添加一个有效的规格')
      return
    }
    
    const names = validSpecs.map(s => s.name)
    if (new Set(names).size !== names.length) {
      ElMessage.warning('规格名称不能重复')
      return
    }
    
    try {
      await dishSpecApi.batchUpdateSpecs(currentDish.value.id, validSpecs)
      specDialogVisible.value = false
      ElMessage.success('规格配置已保存')
      await loadDishes()
    } catch (error: any) {
      ElMessage.error(error.message || '保存失败')
    }
  } else {
    try {
      await dishSpecApi.toggleDishSpecMode(currentDish.value.id)
      specDialogVisible.value = false
      ElMessage.success('已切换为单价格模式')
      await loadDishes()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const getCategoryName = (id: number) => {
  return categories.value.find(c => c.id === id)?.name || '-'
}
</script>

<template>
  <div class="pad-dish-management" v-loading="loading">
    <!-- 顶部操作栏 -->
    <div class="header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回桌台
        </el-button>
        <h2>菜品管理</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showAddCategory">
          <el-icon><Plus /></el-icon>分类
        </el-button>
        <el-button type="success" @click="showAddDish">
          <el-icon><Plus /></el-icon>菜品
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧分类 -->
      <aside class="category-sidebar">
        <div 
          class="category-item" 
          :class="{ active: activeCategory === 0 }"
          @click="activeCategory = 0"
        >
          <span>全部菜品</span>
          <span class="count">{{ dishes.length }}</span>
        </div>
        <div 
          v-for="cat in categories" 
          :key="cat.id"
          class="category-item"
          :class="{ active: activeCategory === cat.id }"
          @click="activeCategory = cat.id"
        >
          <span>{{ cat.name }}</span>
          <span class="count">
            {{ dishes.filter(d => d.categoryId === cat.id).length }}
          </span>
        </div>
      </aside>

      <!-- 右侧菜品列表 -->
      <main class="dish-list">
        <el-empty v-if="filteredDishes.length === 0" description="暂无菜品" />
        
        <div v-else class="dish-grid">
          <el-card 
            v-for="dish in filteredDishes" 
            :key="dish.id"
            class="dish-card"
            :class="{ 'is-off': dish.status === 0 }"
          >
            <div class="dish-image">
              <img :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
              <div v-if="dish.status === 0" class="off-badge">已下架</div>
            </div>
            
            <div class="dish-info">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-category">{{ getCategoryName(dish.categoryId) }}</div>
              
              <!-- 价格显示 -->
              <div class="dish-price-section">
                <div v-if="dish.hasSpecs === 1" class="specs-info">
                  <div class="price-range">
                    ¥{{ Math.min(...dish.specs?.map((s: any) => s.price) || [0]) }} 
                    ~ 
                    ¥{{ Math.max(...dish.specs?.map((s: any) => s.price) || [0]) }}
                  </div>
                  <div class="spec-tags">
                    <el-tag 
                      v-for="spec in dish.specs?.slice(0, 3)" 
                      :key="spec.id"
                      size="small"
                      type="info"
                    >
                      {{ spec.name }}
                    </el-tag>
                    <el-tag v-if="(dish.specs?.length || 0) > 3" size="small">+{{ dish.specs.length - 3 }}</el-tag>
                  </div>
                </div>
                <div v-else class="single-price">
                  <el-input-number 
                    v-model="dish.price" 
                    :min="0" 
                    :precision="2"
                    size="small"
                    @change="(val: number) => handlePriceChange(dish, val)"
                  />
                </div>
              </div>
              
              <div class="dish-actions">
                <el-button size="small" @click="showEditDish(dish)">编辑</el-button>
                <el-button 
                  size="small" 
                  :type="dish.hasSpecs === 1 ? 'primary' : 'default'"
                  @click="showSpecConfig(dish)"
                >
                  {{ dish.hasSpecs === 1 ? '配置规格' : '添加规格' }}
                </el-button>
                <el-button 
                  size="small" 
                  :type="dish.status === 1 ? 'danger' : 'success'"
                  @click="handleToggleStatus(dish)"
                >
                  {{ dish.status === 1 ? '下架' : '上架' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </main>
    </div>

    <!-- 分类编辑弹窗 -->
    <el-dialog v-model="categoryDialogVisible" :title="isEditCategory ? '编辑分类' : '添加分类'" width="400px">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- 菜品编辑弹窗 -->
    <el-dialog v-model="dishDialogVisible" :title="isEditDish ? '编辑菜品' : '添加菜品'" width="500px">
      <el-form :model="dishForm" label-width="80px">
        <el-form-item label="菜品名称">
          <el-input v-model="dishForm.name" placeholder="请输入菜品名称" />
        </el-form-item>
        
        <el-form-item label="分类">
          <el-select v-model="dishForm.categoryId" placeholder="选择分类">
            <el-option 
              v-for="cat in categories" 
              :key="cat.id" 
              :label="cat.name" 
              :value="cat.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="基础价格">
          <el-input-number v-model="dishForm.price" :min="0" :precision="2" />
          <span class="tip">添加规格后，此价格将被规格价格替代</span>
        </el-form-item>
        
        <el-form-item label="库存">
          <el-input-number v-model="dishForm.stock" :min="-1" />
          <span class="tip">-1 表示不限</span>
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input v-model="dishForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDish">确定</el-button>
      </template>
    </el-dialog>

    <!-- 规格配置弹窗 -->
    <el-dialog v-model="specDialogVisible" title="配置规格" width="600px">
      <div v-if="currentDish" class="spec-config">
        <div class="spec-mode">
          <span>规格模式：</span>
          <el-radio-group v-model="hasSpecs">
            <el-radio :label="false">单价格</el-radio>
            <el-radio :label="true">多规格</el-radio>
          </el-radio-group>
        </div>

        <div v-if="hasSpecs" class="spec-list">
          <div v-for="(spec, index) in specList" :key="index" class="spec-item">
            <el-input v-model="spec.name" placeholder="规格名称(如:小份)" style="width: 150px" />
            <el-input-number v-model="spec.price" :min="0" :precision="2" placeholder="价格" />
            
            <el-button-group>
              <el-button size="small" :disabled="index === 0" @click="moveSpec(index, -1)">
                <el-icon><ArrowUp /></el-icon>
              </el-button>
              <el-button size="small" :disabled="index === specList.length - 1" @click="moveSpec(index, 1)">
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <el-button size="small" type="danger" @click="removeSpec(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-button-group>
          </div>
          
          <el-button type="primary" plain @click="addSpec" class="add-spec-btn">
            <el-icon><Plus /></el-icon>添加规格
          </el-button>
          
          <el-alert 
            v-if="specList.length === 0" 
            title="请至少添加一个规格" 
            type="info" 
            :closable="false" 
          />
        </div>
        
        <div v-else class="single-mode-tip">
          <el-alert
            title="当前为单价格模式，使用菜品基础价格"
            type="info"
            :closable="false"
          />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="specDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSpecs">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pad-dish-management {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
  margin: 0;
}

.main-content {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 100px);
}

.category-sidebar {
  width: 200px;
  background: #fff;
  border-radius: 8px;
  padding: 10px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.category-item:hover {
  background: #f5f7fa;
}

.category-item.active {
  background: #409eff;
  color: #fff;
}

.category-item .count {
  font-size: 12px;
  background: #e4e7ed;
  color: #606266;
  padding: 2px 8px;
  border-radius: 10px;
}

.category-item.active .count {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.dish-list {
  flex: 1;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.dish-card {
  transition: all 0.3s;
}

.dish-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.dish-card.is-off {
  opacity: 0.7;
}

.dish-image {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 4px;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.off-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
}

.dish-info {
  padding: 15px;
}

.dish-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.dish-category {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.dish-price-section {
  margin-bottom: 15px;
}

.single-price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.price-range {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
  margin-bottom: 5px;
}

.spec-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.dish-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spec-config {
  padding: 10px;
}

.spec-mode {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;
}

.add-spec-btn {
  margin-top: 10px;
  width: 100%;
}

.single-mode-tip {
  padding: 20px 0;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
