<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDishesByCategory } from '@/api/dish'
import { createOrder, batchAddDishToOrder, getOrderDetail } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import type { SpecItem } from '@/api/dishSpec'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

// 从购物车store或route.query获取桌台信息
const tableIdFromQuery = Number(route.query.tableId) || 0
const tableId = computed(() => cartStore.tableId || tableIdFromQuery)
const tableNo = computed(() => cartStore.tableNo || (route.query.tableNo as string) || '')

// 顾客人数
const customerCount = computed(() => {
  const queryCount = Number(route.query.customerCount)
  if (queryCount > 0) return queryCount
  return cartStore.customerCount || 1
})

const currentOrderId = ref(Number(route.query.orderId) || 0)
const isAddMode = computed(() => route.query.mode === 'add' && !!currentOrderId.value)

const categories = ref<any[]>([])
const activeCategory = ref(0)
const loading = ref(false)
const submitting = ref(false)

// 订单备注
const orderRemark = ref('')

// 当前订单详情
const currentOrder = ref<any>(null)

// 规格选择弹窗
const specDialogVisible = ref(false)
const currentDish = ref<any>(null)
const selectedSpec = ref<SpecItem | null>(null)
const specQuantity = ref(1)
const specRemark = ref('')

const loadDishes = async () => {
  loading.value = true
  try {
    categories.value = await getDishesByCategory()
    if (categories.value.length > 0) {
      activeCategory.value = categories.value[0].id
    }
  } catch (error) {
    ElMessage.error('加载菜品失败')
  } finally {
    loading.value = false
  }
}

const loadCurrentOrder = async () => {
  if (currentOrderId.value) {
    try {
      currentOrder.value = await getOrderDetail(currentOrderId.value)
    } catch (error) {
      console.error('加载订单失败', error)
    }
  }
}

const currentDishes = computed(() => {
  const category = categories.value.find(c => c.id === activeCategory.value)
  return category?.dishes || []
})

// 点击菜品 - 检查是否需要选择规格
const handleDishClick = (dish: any) => {
  if (dish.hasSpecs === 1 && dish.specs?.length > 0) {
    // 需要选择规格
    currentDish.value = dish
    selectedSpec.value = dish.specs[0] // 默认选中第一个
    specQuantity.value = 1
    specRemark.value = ''
    specDialogVisible.value = true
  } else {
    // 直接加入购物车
    addToCart(dish)
  }
}

// 添加规格商品到购物车
const addSpecToCart = () => {
  if (!currentDish.value || !selectedSpec.value) return

  cartStore.addItem({
    dishId: currentDish.value.id,
    specId: selectedSpec.value.id,
    name: currentDish.value.name,
    specName: selectedSpec.value.name,
    price: selectedSpec.value.price,
    image: currentDish.value.image,
    quantity: specQuantity.value,
    remark: specRemark.value,
  })

  specDialogVisible.value = false
  ElMessage.success(`已添加 ${currentDish.value.name} (${selectedSpec.value.name})`)
}

// 直接添加到购物车（无规格）
const addToCart = (dish: any) => {
  cartStore.addItem({
    dishId: dish.id,
    name: dish.name,
    price: dish.price,
    image: dish.image,
    quantity: 1,
  })
  ElMessage.success(`已添加 ${dish.name}`)
}

const removeFromCart = (dishId: number, specId?: number) => {
  cartStore.removeItem(dishId, specId)
}

const updateQuantity = (dishId: number, quantity: number, specId?: number) => {
  if (quantity <= 0) {
    cartStore.removeItem(dishId, specId)
  } else {
    cartStore.updateQuantity(dishId, quantity, specId)
  }
}

const cartTotal = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const submitOrder = async () => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }

  if (!tableId.value) {
    ElMessage.error('桌台信息缺失')
    return
  }

  submitting.value = true
  try {
    // 加菜模式
    if (isAddMode.value) {
      const addDishData = {
        tableId: tableId.value,
        items: cartStore.items.map(item => ({
          dishId: item.dishId,
          quantity: item.quantity,
          remark: item.remark || '',
        })),
      }

      await batchAddDishToOrder(addDishData)
      ElMessage.success('加菜成功')

      cartStore.clearCart()
      orderRemark.value = ''

      router.push({
        path: '/pad/orders',
        query: {
          tableId: tableId.value,
          tableNo: tableNo.value,
        },
      })
    } else {
      // 新订单模式
      const orderData = {
        tableId: tableId.value,
        customerCount: customerCount.value,
        cartItems: cartStore.items.map(item => ({
          dishId: item.dishId,
          specId: item.specId,
          dishName: item.name,
          specName: item.specName,
          dishImage: item.image,
          price: item.price,
          quantity: item.quantity,
          remark: item.remark || '',
        })),
        remark: orderRemark.value,
      }

      const order = await createOrder(orderData)
      ElMessage.success('下单成功')

      const currentTableId = tableId.value
      const currentTableNo = tableNo.value

      cartStore.clearCart()
      orderRemark.value = ''

      await ElMessageBox.confirm('下单成功！是否查看订单？', '提示', {
        confirmButtonText: '查看订单',
        cancelButtonText: '返回桌台',
      })
        .then(() => {
          router.push({
            path: '/pad/orders',
            query: {
              tableId: currentTableId,
              tableNo: currentTableNo,
            },
          })
        })
        .catch(() => {
          router.push('/pad/tables')
        })
    }
  } catch (error: any) {
    ElMessage.error(error.message || (isAddMode.value ? '加菜失败' : '下单失败'))
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/pad/tables')
}

onMounted(() => {
  if (!tableId.value) {
    ElMessage.error('桌台信息缺失，请先选择桌台')
    router.push('/pad/tables')
    return
  }
  loadDishes()
  loadCurrentOrder()
})
</script>

<template>
  <div v-loading="loading" class="order-pad">
    <!-- 顶部信息栏 -->
    <div class="header">
      <div class="header-left">
        <el-button icon="ArrowLeft" @click="goBack">返回</el-button>
        <span class="table-info">
          桌号: {{ tableNo }} | 人数: {{ customerCount }}人
          <el-tag v-if="isAddMode" type="warning" style="margin-left: 10px">加菜模式</el-tag>
        </span>
      </div>
      <div class="header-right">
        <el-tag v-if="currentOrder" type="warning"
          >订单号: {{ currentOrder.order?.orderNo }}</el-tag
        >
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧菜品区 -->
      <div class="dishes-area">
        <el-tabs v-model="activeCategory" type="border-card" class="category-tabs">
          <el-tab-pane
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :name="category.id"
          >
            <div class="dishes-grid">
              <el-card
                v-for="dish in category.dishes"
                :key="dish.id"
                class="dish-card"
                shadow="hover"
                @click="handleDishClick(dish)"
              >
                <div class="dish-image">
                  <img
                    :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'"
                    :alt="dish.name"
                  />
                  <el-tag v-if="dish.isRecommend" type="danger" class="recommend-tag">推荐</el-tag>
                  <el-tag v-if="dish.hasSpecs === 1" type="primary" class="spec-tag">多规格</el-tag>
                </div>
                <div class="dish-info">
                  <div class="dish-name">{{ dish.name }}</div>
                  <div class="dish-price">
                    <template v-if="dish.hasSpecs === 1 && dish.specs?.length > 0">
                      ¥{{ Math.min(...dish.specs.map((s: any) => s.price)).toFixed(0) }} 起
                    </template>
                    <template v-else> ¥{{ dish.price.toFixed(2) }} </template>
                  </div>
                </div>
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧购物车 -->
      <div class="cart-area">
        <el-card class="cart-card">
          <template #header>
            <div class="cart-header">
              <span>购物车</span>
              <el-button
                v-if="cartStore.items.length > 0"
                size="small"
                type="danger"
                link
                @click="cartStore.clearCart()"
              >
                清空
              </el-button>
            </div>
          </template>

          <!-- 购物车列表 -->
          <div v-if="cartStore.items.length > 0" class="cart-list">
            <div
              v-for="item in cartStore.items"
              :key="item.dishId + '-' + (item.specId || 0)"
              class="cart-item"
            >
              <div class="item-info">
                <div class="item-name">
                  {{ item.name }}
                  <el-tag v-if="item.specName" size="small" type="info">{{ item.specName }}</el-tag>
                </div>
                <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
              </div>
              <div class="item-actions">
                <el-button
                  size="small"
                  circle
                  @click="updateQuantity(item.dishId, item.quantity - 1, item.specId)"
                  >-</el-button
                >
                <span class="quantity">{{ item.quantity }}</span>
                <el-button
                  size="small"
                  circle
                  @click="updateQuantity(item.dishId, item.quantity + 1, item.specId)"
                  >+</el-button
                >
              </div>
            </div>

            <!-- 订单备注 -->
            <div class="remark-section">
              <div class="remark-label">订单备注</div>
              <el-input
                v-model="orderRemark"
                type="textarea"
                :rows="2"
                placeholder="请输入订单备注（如：少辣、免葱等）"
                maxlength="100"
                show-word-limit
              />
            </div>
          </div>

          <el-empty v-else description="购物车是空的" />

          <!-- 底部合计 -->
          <div class="cart-footer">
            <div class="total">
              <span>合计: </span>
              <span class="total-price">¥{{ cartTotal.toFixed(2) }}</span>
            </div>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="cartStore.items.length === 0"
              style="width: 100%"
              @click="submitOrder"
            >
              {{ isAddMode ? '确认加菜' : '提交订单' }} ({{
                cartStore.items.reduce((sum, i) => sum + i.quantity, 0)
              }})
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 规格选择弹窗 -->
    <el-dialog v-model="specDialogVisible" :title="currentDish?.name" width="400px">
      <div v-if="currentDish" class="spec-dialog-content">
        <div class="spec-section">
          <div class="section-label">选择规格</div>
          <div class="spec-options">
            <el-radio-group v-model="selectedSpec">
              <el-radio-button v-for="spec in currentDish.specs" :key="spec.id" :label="spec">
                {{ spec.name }} ¥{{ spec.price.toFixed(0) }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="quantity-section">
          <div class="section-label">数量</div>
          <el-input-number v-model="specQuantity" :min="1" :max="99" />
        </div>

        <div class="remark-section">
          <div class="section-label">备注</div>
          <el-input v-model="specRemark" placeholder="口味要求等" maxlength="50" />
        </div>

        <div v-if="selectedSpec" class="spec-summary">
          <div class="summary-row">
            <span>单价: ¥{{ selectedSpec.price.toFixed(2) }}</span>
            <span>× {{ specQuantity }}</span>
          </div>
          <div class="summary-total">
            小计: ¥{{ (selectedSpec.price * specQuantity).toFixed(2) }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="specDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedSpec" @click="addSpecToCart">
          加入购物车
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-pad {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.table-info {
  font-size: 16px;
  color: #606266;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 15px;
  gap: 15px;
}

.dishes-area {
  flex: 1;
  overflow: hidden;
}

.category-tabs {
  height: 100%;
}

.category-tabs :deep(.el-tabs__content) {
  height: calc(100% - 55px);
  overflow-y: auto;
}

.dishes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
  padding: 10px;
}

.dish-card {
  cursor: pointer;
  transition: all 0.3s;
}

.dish-card:hover {
  transform: translateY(-5px);
}

.dish-image {
  position: relative;
  height: 150px;
  overflow: hidden;
  border-radius: 4px;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recommend-tag {
  position: absolute;
  top: 5px;
  left: 5px;
}

.spec-tag {
  position: absolute;
  top: 5px;
  right: 5px;
}

.dish-info {
  padding: 10px;
  text-align: center;
}

.dish-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}

.cart-area {
  width: 350px;
  display: flex;
  flex-direction: column;
}

.cart-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.cart-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.item-price {
  font-size: 14px;
  color: #f56c6c;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity {
  min-width: 30px;
  text-align: center;
  font-weight: bold;
}

.remark-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #ebeef5;
}

.remark-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.cart-footer {
  padding: 15px;
  border-top: 1px solid #ebeef5;
  background: #f5f7fa;
}

.total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 16px;
}

.total-price {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
}

/* 规格弹窗 */
.spec-dialog-content {
  padding: 10px 0;
}

.spec-section,
.quantity-section,
.remark-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-summary {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
}

.summary-total {
  text-align: right;
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}
</style>
