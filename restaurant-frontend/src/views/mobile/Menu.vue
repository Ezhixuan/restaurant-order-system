<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getDishesByCategory } from '@/api/dish'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const tableNo = route.params.tableNo as string
const categories = ref<any[]>([])
const activeCategory = ref(0)
const loading = ref(false)

const loadDishes = async () => {
  loading.value = true
  try {
    categories.value = await getDishesByCategory()
    if (categories.value.length > 0) {
      activeCategory.value = 0
    }
  } catch (error) {
    showToast('加载菜品失败')
  } finally {
    loading.value = false
  }
}

const currentDishes = computed(() => {
  if (categories.value.length === 0) return []
  return categories.value[activeCategory.value]?.dishes || []
})

const cartCount = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.quantity, 0)
})

const cartTotal = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

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

const goToCart = () => {
  if (cartStore.items.length === 0) {
    showToast('购物车为空')
    return
  }
  router.push('/m/cart')
}

onMounted(() => {
  loadDishes()
  // 设置当前桌台
  cartStore.tableNo = tableNo
})
</script>

<template>
  <div class="mobile-menu">
    <van-nav-bar :title="`桌号 ${tableNo}`" fixed />
    
    <!-- 分类侧边栏 + 菜品列表 -->
    <van-tree-select
      v-model:active="activeCategory"
      :items="categories.map(c => ({ text: c.name, children: c.dishes }))"
      height="calc(100vh - 100px)"
      class="menu-content"
    >
      <template #content>
        <div v-for="dish in currentDishes" :key="dish.id" class="dish-item">
          <van-card
            :price="dish.price.toFixed(2)"
            :desc="dish.description"
            :title="dish.name"
            :thumb="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'"
          >
            <template #tags>
              <van-tag v-if="dish.isRecommend" type="danger">推荐</van-tag>
            </template>
            <template #footer>
              <van-button size="small" type="primary" round @click="addToCart(dish)">
                加入购物车
              </van-button>
            </template>
          </van-card>
        </div>
      </template>
    </van-tree-select>
    
    <!-- 底部购物车栏 -->
    <van-submit-bar
      :price="cartTotal * 100"
      button-text="去结算"
      @submit="goToCart"
    >
      <van-badge :content="cartCount" v-if="cartCount > 0">
        <van-icon name="cart-o" size="30" />
      </van-badge>
      <van-icon v-else name="cart-o" size="30" />
      <span class="cart-text">购物车</span>
    </van-submit-bar>
  </div>
</template>

<style scoped>
.mobile-menu {
  min-height: 100vh;
  background: #f5f5f5;
  padding-top: 46px;
  padding-bottom: 50px;
}

.menu-content {
  margin-top: 10px;
}

.dish-item {
  margin-bottom: 10px;
}

.cart-text {
  margin-left: 8px;
  font-size: 14px;
  color: #666;
}
</style>
