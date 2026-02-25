<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tableNo = route.params.tableNo as string

// TODO: 实现菜品列表和购物车
const dishes = ref([
  { id: 1, name: '宫保鸡丁', price: 38, image: '', category: '招牌菜' },
  { id: 2, name: '麻婆豆腐', price: 18, image: '', category: '热菜' }
])

const cart = ref<{ id: number; name: string; price: number; quantity: number }[]>([])

const addToCart = (dish: any) => {
  const item = cart.value.find(i => i.id === dish.id)
  if (item) {
    item.quantity++
  } else {
    cart.value.push({ ...dish, quantity: 1 })
  }
}

const goToCart = () => {
  router.push('/m/cart')
}
</script>

<template>
  <div class="mobile-menu">
    <van-nav-bar :title="`桌号 ${tableNo}`" fixed />
    
    <div class="menu-content">
      <van-card
        v-for="dish in dishes"
        :key="dish.id"
        :price="dish.price.toFixed(2)"
        :desc="dish.category"
        :title="dish.name"
        thumb="https://img.yzcdn.cn/vant/ipad.jpeg"
      >
        <template #footer>
          <van-button size="mini" type="primary" @click="addToCart(dish)">
            加入购物车
          </van-button>
        </template>
      </van-card>
    </div>
    
    <van-submit-bar
      :price="cart.reduce((sum, item) => sum + item.price * item.quantity * 100, 0)"
      button-text="去结算"
      @submit="goToCart"
    >
      <van-icon name="cart-o" :badge="cart.reduce((sum, item) => sum + item.quantity, 0)" />
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
  padding: 10px;
}
</style>
