<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const cartItems = computed(() => cartStore.items)
const totalAmount = computed(() => cartStore.totalAmount)

const updateQuantity = (item: any, delta: number) => {
  const newQuantity = item.quantity + delta
  if (newQuantity <= 0) {
    showConfirmDialog({
      title: '提示',
      message: '确定删除该商品吗？'
    }).then(() => {
      cartStore.removeItem(item.dishId)
    }).catch(() => {})
  } else {
    cartStore.updateQuantity(item.dishId, newQuantity)
  }
}

const clearCart = () => {
  if (cartItems.value.length === 0) return
  showConfirmDialog({
    title: '提示',
    message: '确定清空购物车吗？'
  }).then(() => {
    cartStore.clearCart()
    showToast('购物车已清空')
  }).catch(() => {})
}

const submitOrder = () => {
  if (cartItems.value.length === 0) {
    showToast('购物车为空')
    return
  }
  router.push('/m/order')
}
</script>

<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" left-arrow @click-left="$router.back()" />
    
    <!-- 桌台信息 -->
    <van-cell-group v-if="cartStore.tableNo" style="margin-top: 10px">
      <van-cell title="桌号" :value="cartStore.tableNo" />
    </van-cell-group>
    
    <!-- 购物车列表 -->
    <div v-if="cartItems.length > 0" class="cart-list">
      <van-swipe-cell v-for="item in cartItems" :key="item.dishId">
        <van-card
          :price="item.price.toFixed(2)"
          :title="item.name"
          :thumb="item.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'"
        >
          <template #desc>
            <div v-if="item.remark" class="remark">备注: {{ item.remark }}</div>
          </template>
          
          <template #num>
            <van-stepper
              v-model="item.quantity"
              :min="1"
              @change="(val) => cartStore.updateQuantity(item.dishId, val)"
            />
          </template>
        </van-card>
        
        <template #right>
          <van-button
            square
            text="删除"
            type="danger"
            class="delete-button"
            @click="cartStore.removeItem(item.dishId)"
          />
        </template>
      </van-swipe-cell>
    </div>
    
    <!-- 空购物车 -->
    <van-empty v-else description="购物车是空的" />
    
    <!-- 底部操作栏 -->
    <van-submit-bar
      :price="totalAmount * 100"
      button-text="提交订单"
      @submit="submitOrder"
    >
      <van-button
        v-if="cartItems.length > 0"
        size="small"
        type="danger"
        plain
        @click="clearCart"
        style="margin-left: 10px"
      >
        清空
      </van-button>
    </van-submit-bar>
  </div>
</template>

<style scoped>
.cart-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 100px;
}

.cart-list {
  margin-top: 10px;
}

.remark {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.delete-button {
  height: 100%;
}
</style>
