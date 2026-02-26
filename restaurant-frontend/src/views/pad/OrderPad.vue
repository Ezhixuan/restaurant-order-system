<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDishesByCategory } from '@/api/dish'
import { createOrder, batchAddDishToOrder, getOrderDetail } from '@/api/order'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

// 从购物车store或route.query获取桌台信息
const tableIdFromQuery = Number(route.query.tableId) || 0
const tableId = computed(() => cartStore.tableId || tableIdFromQuery)
const tableNo = computed(() => cartStore.tableNo || route.query.tableNo as string || '')

// 顾客人数：优先从query获取（开台时传入），其次从store获取，默认1
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

const addToCart = (dish: any) => {
  cartStore.addItem({
    dishId: dish.id,
    name: dish.name,
    price: dish.price,
    image: dish.image,
    quantity: 1
  })
  ElMessage.success(`已添加 ${dish.name}`)
}

const removeFromCart = (dishId: number) => {
  cartStore.removeItem(dishId)
}

const updateQuantity = (dishId: number, quantity: number) => {
  if (quantity <= 0) {
    cartStore.removeItem(dishId)
  } else {
    cartStore.updateQuantity(dishId, quantity)
  }
}

// 更新菜品备注
const updateItemRemark = (dishId: number, remark: string) => {
  cartStore.updateRemark(dishId, remark)
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
    // 加菜模式：向现有订单添加菜品
    if (isAddMode.value) {
      const addDishData = {
        tableId: tableId.value,
        items: cartStore.items.map(item => ({
          dishId: item.dishId,
          quantity: item.quantity,
          remark: item.remark || ''
        }))
      }

      await batchAddDishToOrder(addDishData)
      ElMessage.success('加菜成功')
      
      cartStore.clearCart()
      orderRemark.value = ''
      
      // 跳转到订单详情页
      router.push({
        path: '/pad/orders',
        query: { 
          tableId: tableId.value,
          tableNo: tableNo.value
        }
      })
    } else {
      // 新订单模式：创建新订单
      const orderData = {
        tableId: tableId.value,
        customerCount: customerCount.value,
        cartItems: cartStore.items.map(item => ({
          dishId: item.dishId,
          dishName: item.name,
          dishImage: item.image,
          price: item.price,
          quantity: item.quantity,
          remark: item.remark || ''
        })),
        remark: orderRemark.value
      }

      const order = await createOrder(orderData)
      ElMessage.success('下单成功')
      
      // 保存桌台信息，用于后续跳转
      const currentTableId = tableId.value
      const currentTableNo = tableNo.value
      const currentCustomerCount = customerCount.value
      
      cartStore.clearCart()
      orderRemark.value = ''
      
      // 跳转到订单详情或返回桌台
      await ElMessageBox.confirm('下单成功！是否查看订单？', '提示', {
        confirmButtonText: '查看订单',
        cancelButtonText: '返回桌台'
      }).then(() => {
        // 携带桌台信息跳转到订单列表
        router.push({
          path: '/pad/orders',
          query: { 
            tableId: currentTableId,
            tableNo: currentTableNo,
            customerCount: currentCustomerCount
          }
        })
      }).catch(() => {
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
  <div class="order-pad" v-loading="loading">
    <!-- 顶部信息栏 -->
    <div class="header">
      <div class="header-left">
        <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
        <span class="table-info">
          桌号: {{ tableNo }} | 人数: {{ customerCount }}人
          <el-tag v-if="isAddMode" type="warning" style="margin-left: 10px;">加菜模式</el-tag>
        </span>
      </div>
      <div class="header-right">
        <el-tag v-if="currentOrder" type="warning">订单号: {{ currentOrder.order?.orderNo }}</el-tag>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧菜品区 -->
      <div class="dishes-area">
        <!-- 分类标签 -->
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
                @click="addToCart(dish)"
              >
                <div class="dish-image">
                  <img :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" :alt="dish.name" />
                  <el-tag v-if="dish.isRecommend" type="danger" class="recommend-tag">推荐</el-tag>
                </div>
                <div class="dish-info">
                  <div class="dish-name">{{ dish.name }}</div>
                  <div class="dish-price">¥{{ dish.price.toFixed(2) }}</div>
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
            <div v-for="item in cartStore.items" :key="item.dishId" class="cart-item">
              <div class="item-info">
                <div class="item-name">{{ item.name }}</div>
                <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
              </div>
              <div class="item-actions">
                <el-button
                  size="small"
                  circle
                  @click="updateQuantity(item.dishId, item.quantity - 1)"
                >-</el-button>
                <span class="quantity">{{ item.quantity }}</span>
                <el-button
                  size="small"
                  circle
                  @click="updateQuantity(item.dishId, item.quantity + 1)"
                >+</el-button>
              </div>
            </div>
            
            <!-- 订单备注输入框 -->
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
              @click="submitOrder"
              style="width: 100%"
            >
              {{ isAddMode ? '确认加菜' : '提交订单' }} ({{ cartStore.items.reduce((sum, i) => sum + i.quantity, 0) }})
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
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
</style>
