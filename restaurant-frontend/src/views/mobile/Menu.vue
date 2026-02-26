<script setup lang="ts">import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant'
import { getDishesByCategory } from '@/api/dish'
import { getTables } from '@/api/table'
import { getOrderByTable, addDishToOrder, createOrder } from '@/api/order'
import { useCartStore } from '@/stores/cart'
import type { SpecItem } from '@/api/dishSpec'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const tableNo = route.params.tableNo as string
categories.value = []
const activeCategory = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const showSearch = ref(false)

// 桌台和订单信息
const tableId = ref<number | null>(null)
const existingOrder = ref<any>(null)
const checkingOrder = ref(false)

// 顾客人数选择弹窗
const showCustomerDialog = ref(false)
const tempCustomerCount = ref(1)

// 订单备注
const orderRemark = ref('')
const showRemarkDialog = ref(false)

// 规格选择弹窗
const showSpecDialog = ref(false)
const currentDish = ref<any>(null)
const selectedSpec = ref<SpecItem | null>(null)
const specQuantity = ref(1)

// 加载菜品
const loadDishes = async () => {
  loading.value = true
  try {
    const data = await getDishesByCategory()
    categories.value = data
    if (categories.value.length > 0) {
      activeCategory.value = categories.value[0].id
    }
  } catch (error) {
    showToast('加载菜品失败')
  } finally {
    loading.value = false
  }
}

// 获取桌台信息并检查是否有活跃订单
const loadTableInfo = async () => {
  try {
    const tables = await getTables()
    const currentTable = tables.find((t: any) => t.tableNo === tableNo)
    if (currentTable) {
      tableId.value = currentTable.id
      cartStore.setTableInfo(currentTable.id, tableNo, cartStore.customerCount || 1)
      
      // 检查是否有活跃订单
      await checkExistingOrder(currentTable.id)
      
      // 如果没有活跃订单且是首次进入（customerCount为1），显示人数选择
      if (!existingOrder.value && cartStore.customerCount === 1) {
        showCustomerDialog.value = true
      }
    }
  } catch (error) {
    console.error('获取桌台信息失败', error)
  }
}

// 检查是否有未完成订单
const checkExistingOrder = async (tid: number) => {
  checkingOrder.value = true
  try {
    const orderDetail = await getOrderByTable(tid)
    if (orderDetail && orderDetail.order) {
      existingOrder.value = orderDetail.order
    }
  } catch (error) {
    // 没有订单是正常的
    existingOrder.value = null
  } finally {
    checkingOrder.value = false
  }
}

// 确认顾客人数
const confirmCustomerCount = () => {
  cartStore.setTableInfo(tableId.value || 0, tableNo, tempCustomerCount.value)
  showCustomerDialog.value = false
  showToast(`已设置 ${tempCustomerCount.value} 位用餐`)
}

// 修改顾客人数
const changeCustomerCount = () => {
  tempCustomerCount.value = cartStore.customerCount || 1
  showCustomerDialog.value = true
}

// 查看订单
const viewOrder = () => {
  if (!existingOrder.value || !tableId.value) {
    showToast('没有可查看的订单')
    return
  }
  
  router.push({
    path: '/m/success',
    query: {
      tableId: tableId.value.toString(),
      tableNo: tableNo
    }
  })
}

// 搜索过滤后的菜品
const filteredDishes = computed(() => {
  if (!searchKeyword.value) return []
  
  const keyword = searchKeyword.value.toLowerCase()
  const result: any[] = []
  
  categories.value.forEach(cat => {
    cat.dishes.forEach((dish: any) => {
      if (dish.name.toLowerCase().includes(keyword)) {
        result.push({ ...dish, categoryName: cat.name })
      }
    })
  })
  
  return result
})

// 当前分类的菜品
const currentDishes = computed(() => {
  if (searchKeyword.value) return filteredDishes.value
  
  const category = categories.value.find(c => c.id === activeCategory.value)
  return category?.dishes || []
})

const cartCount = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.quantity, 0)
})

const cartTotal = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

// 点击菜品
const handleDishClick = (dish: any) => {
  if (dish.hasSpecs === 1 && dish.specs?.length > 0) {
    // 需要选择规格
    currentDish.value = dish
    selectedSpec.value = dish.specs[0]
    specQuantity.value = 1
    showSpecDialog.value = true
  } else {
    // 直接加入购物车
    addToCart(dish)
  }
}

// 直接添加到购物车（无规格）
const addToCart = (dish: any) => {
  cartStore.addItem({
    dishId: dish.id,
    name: dish.name,
    price: dish.price,
    image: dish.image,
    quantity: 1
  })
  showToast('已加入购物车')
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
    quantity: specQuantity.value
  })
  
  showSpecDialog.value = false
  showToast(`已添加 ${currentDish.value.name} (${selectedSpec.value.name})`)
}

// 提交订单
const submitOrder = async () => {
  if (cartStore.items.length === 0) {
    showToast('购物车为空')
    return
  }

  if (!tableId.value) {
    showToast('桌台信息缺失')
    return
  }

  showRemarkDialog.value = true
}

// 确认提交
const confirmSubmit = async () => {
  showRemarkDialog.value = false
  showLoadingToast({ message: '提交中...', forbidClick: true })

  try {
    await checkExistingOrder(tableId.value!)
    
    if (existingOrder.value && existingOrder.value.status < 4) {
      // 追加到现有订单
      for (const item of cartStore.items) {
        await addDishToOrder(existingOrder.value.id, {
          dishId: item.dishId,
          quantity: item.quantity,
          remark: item.remark || ''
        })
      }
      closeToast()
      showToast('加菜成功')
    } else {
      // 创建新订单
      const orderData = {
        tableId: tableId.value,
        customerCount: cartStore.customerCount || 1,
        cartItems: cartStore.items.map(item => ({
          dishId: item.dishId,
          specId: item.specId,
          dishName: item.name,
          specName: item.specName,
          dishImage: item.image,
          price: item.price,
          quantity: item.quantity,
          remark: item.remark || ''
        })),
        remark: orderRemark.value
      }
      await createOrder(orderData)
      closeToast()
      showToast('下单成功')
    }
    
    cartStore.clearCart()
    orderRemark.value = ''
    
    router.push({
      path: '/m/success',
      query: {
        tableId: tableId.value!.toString(),
        tableNo: tableNo
      }
    })
  } catch (error: any) {
    closeToast()
    showToast(error.message || '提交失败')
  }
}

const onSearch = () => {
  if (!searchKeyword.value) {
    showSearch.value = false
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  showSearch.value = false
}

onMounted(() => {
  loadDishes()
  loadTableInfo()
})
</script>

<template>
  <div class="mobile-menu">
    <!-- 顶部导航 -->
    <div class="header">
      <div class="header-left">
        <span class="table-badge">{{ tableNo }}号桌</span>
        <span v-if="!existingOrder" class="customer-count" @click="changeCustomerCount">
          {{ cartStore.customerCount }}人 <van-icon name="arrow-down" />
        </span>
      </div>
      <div class="header-right">
        <van-button
          v-if="existingOrder"
          type="success"
          size="small"
          round
          icon="orders-o"
          @click="viewOrder"
          style="margin-right: 10px;"
        >
          查看订单
        </van-button>
        <van-icon name="search" size="20" @click="showSearch = true" />
      </div>
    </div>

    <!-- 搜索弹窗 -->
    <van-search
      v-if="showSearch"
      v-model="searchKeyword"
      placeholder="搜索菜品名称"
      show-action
      autofocus
      @search="onSearch"
      @cancel="clearSearch"
    />

    <!-- 分类标签 -->
    <div v-if="!searchKeyword" class="category-tabs">
      <van-tabs v-model:active="activeCategory" sticky swipeable color="#1989fa">
        <van-tab
          v-for="category in categories"
          :key="category.id"
          :title="category.name"
          :name="category.id"
        >
          <div class="dish-list">
            <div
              v-for="dish in category.dishes"
              :key="dish.id"
              class="dish-card"
              @click="handleDishClick(dish)"
            >
              <div class="dish-image">
                <img :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
                <div v-if="dish.isRecommend" class="recommend-badge">推荐</div>
                <div v-if="dish.hasSpecs === 1" class="spec-badge">多规格</div>
              </div>
              
              <div class="dish-info">
                <div class="dish-name">{{ dish.name }}</div>
                <div class="dish-desc" v-if="dish.description">{{ dish.description }}</div>
                <div class="dish-bottom">
                  <div class="dish-price">
                    <span class="price-symbol">¥</span>
                    <span v-if="dish.hasSpecs === 1 && dish.specs?.length > 0" class="price-num">
                      {{ Math.min(...dish.specs.map((s: any) => s.price)) }}起
                    </span>
                    <span v-else class="price-num">{{ dish.price }}</span>
                  </div>
                  <div class="add-btn">
                    <van-icon name="plus" />
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <van-empty v-if="category.dishes.length === 0" description="暂无菜品" />
        </van-tab>
      </van-tabs>
    </div>

    <!-- 搜索结果 -->
    <div v-else class="search-results">
      <div class="search-header">
        搜索结果 ({{ filteredDishes.length }})
      </div>
      
      <div class="dish-list">
        <div
          v-for="dish in filteredDishes"
          :key="dish.id"
          class="dish-card"
          @click="handleDishClick(dish)"
        >
          <div class="dish-image">
            <img :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
          </div>
          
          <div class="dish-info">
            <div class="dish-name">{{ dish.name }}</div>
            <div class="dish-category">{{ dish.categoryName }}</div>
            
            <div class="dish-bottom">
              <div class="dish-price">
                <span class="price-symbol">¥</span>
                <span class="price-num">{{ dish.price }}</span>
              </div>
              <div class="add-btn">
                <van-icon name="plus" />
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <van-empty v-if="filteredDishes.length === 0" description="未找到相关菜品" />
    </div>

    <!-- 底部购物车栏 -->
    <div class="cart-bar" v-if="cartCount > 0">
      <div class="cart-info" @click="submitOrder">
        <div class="cart-icon">
          <van-icon name="shopping-cart-o" :badge="cartCount" />
        </div>
        <div class="cart-price">
          <span class="price-symbol">¥</span>
          <span class="price-num">{{ cartTotal.toFixed(2) }}</span>
        </div>
      </div>
      
      <van-button type="primary" round @click="submitOrder">
        {{ existingOrder ? '追加到订单' : '去结算' }}
      </van-button>
    </div>
    
    <!-- 空状态提示 -->
    <van-empty v-if="!loading && categories.length === 0" description="暂无菜品数据" />

    <!-- 顾客人数选择弹窗 -->
    <van-dialog
      v-model:show="showCustomerDialog"
      title="选择用餐人数"
      show-cancel-button
      @confirm="confirmCustomerCount"
    >
      <div class="customer-picker">
        <div class="picker-label">用餐人数</div>
        <van-stepper v-model="tempCustomerCount" :min="1" :max="50" integer />
      </div>
    </van-dialog>

    <!-- 规格选择弹窗 -->
    <van-dialog
      v-model:show="showSpecDialog"
      :title="currentDish?.name"
      show-cancel-button
      @confirm="addSpecToCart"
    >
      <div class="spec-picker">
        <div class="picker-label">选择规格</div>
        <div class="spec-options">
          <van-radio-group v-model="selectedSpec">
            <van-cell-group inset>
              <van-cell
                v-for="spec in currentDish?.specs"
                :key="spec.id"
                clickable
                @click="selectedSpec = spec"
              >
                <template #title>
                  <span class="spec-name">{{ spec.name }}</span>
                  <span class="spec-price">¥{{ spec.price }}</span>
                </template>
                <template #right-icon>
                  <van-radio :name="spec" />
                </template>
              </van-cell>
            </van-cell-group>
          </van-radio-group>
        </div>
        
        <div class="picker-label" style="margin-top: 15px;">数量</div>
        <van-stepper v-model="specQuantity" :min="1" :max="99" />
      </div>
    </van-dialog>

    <!-- 备注输入弹窗 -->
    <van-dialog
      v-model:show="showRemarkDialog"
      title="订单备注"
      show-cancel-button
      @confirm="confirmSubmit"
    >
      <div class="remark-input">
        <van-field
          v-model="orderRemark"
          rows="3"
          autosize
          label=""
          type="textarea"
          maxlength="100"
          placeholder="请输入订单备注（如：少辣、免葱、忌口等）"
          show-word-limit
        />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.mobile-menu {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 80px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-badge {
  background: linear-gradient(135deg, #1989fa, #39b9fa);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.customer-count {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 12px;
}

.dish-list {
  padding: 12px;
}

.dish-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dish-image {
  width: 110px;
  height: 110px;
  position: relative;
  flex-shrink: 0;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recommend-badge {
  position: absolute;
  top: 0;
  left: 0;
  background: linear-gradient(135deg, #ff6b6b, #ee5a5a);
  color: #fff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 0 0 10px 0;
}

.spec-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: linear-gradient(135deg, #1989fa, #39b9fa);
  color: #fff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 0 0 0 10px;
}

.dish-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.dish-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dish-desc {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dish-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.dish-price {
  color: #f44;
  font-weight: 600;
}

.price-symbol {
  font-size: 12px;
}

.price-num {
  font-size: 20px;
}

.add-btn {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #1989fa, #39b9fa);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(25, 137, 250, 0.3);
}

.search-results {
  padding: 12px;
}

.search-header {
  font-size: 14px;
  color: #969799;
  margin-bottom: 12px;
  padding: 0 4px;
}

.cart-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.cart-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-icon {
  font-size: 28px;
  color: #1989fa;
}

.cart-price {
  color: #f44;
  font-weight: 600;
}

.cart-price .price-num {
  font-size: 24px;
}

.customer-picker,
.spec-picker,
.remark-input {
  padding: 20px;
}

.picker-label {
  font-size: 14px;
  color: #969799;
  margin-bottom: 15px;
}

.spec-options {
  margin-bottom: 15px;
}

.spec-name {
  margin-right: 10px;
}

.spec-price {
  color: #f44;
  font-weight: 600;
}

:deep(.van-tabs__wrap) {
  background: #fff;
}

:deep(.van-tab) {
  font-size: 14px;
}

:deep(.van-tab--active) {
  font-weight: 600;
}

:deep(.van-badge) {
  background: #f44;
}
</style>