<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useCartStore } from '@/stores/cart'
import { createOrder } from '@/api/order'

const router = useRouter()
const cartStore = useCartStore()

const customerCount = ref(cartStore.customerCount)
const remark = ref('')
const submitting = ref(false)

const cartItems = cartStore.items
const totalAmount = cartStore.totalAmount

const submitOrder = async () => {
  if (cartItems.length === 0) {
    showToast('购物车为空')
    return
  }

  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true })

  try {
    // 这里需要 tableId，从购物车store中获取
    // 实际应该从路由或store中拿到tableId
    const tableId = 1 // 临时值，实际需要根据tableNo获取

    const orderData = {
      tableId,
      customerCount: customerCount.value,
      cartItems: cartItems.map(item => ({
        dishId: item.dishId,
        dishName: item.name,
        dishImage: item.image,
        price: item.price,
        quantity: item.quantity,
        remark: item.remark
      })),
      remark: remark.value
    }

    await createOrder(orderData)
    closeToast()
    showToast('下单成功')
    cartStore.clearCart()
    router.push('/m')
  } catch (error: any) {
    closeToast()
    showToast(error.message || '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="order-page">
    <van-nav-bar title="确认订单" left-arrow @click-left="$router.back()" />
    
    <!-- 用餐人数 -->
    <van-cell-group title="用餐信息" style="margin-top: 10px">
      <van-field label="用餐人数" type="digit" v-model="customerCount">
        <template #input>
          <van-stepper v-model="customerCount" :min="1" :max="50" />
        </template>
      </van-field>
    </van-cell-group>
    
    <!-- 订单商品 -->
    <van-cell-group title="订单商品">
      <van-cell
        v-for="item in cartItems"
        :key="item.dishId"
        :title="item.name"
        :label="item.remark ? `备注: ${item.remark}` : undefined"
      >
        <template #value>
          <div>x{{ item.quantity }}</div>
          <div class="price">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
        </template>
      </van-cell>
      
      <van-cell title="合计" :value="`¥${totalAmount.toFixed(2)}`" class="total" />
    </van-cell-group>
    
    <!-- 备注 -->
    <van-cell-group title="订单备注" style="margin-top: 10px">
      <van-field
        v-model="remark"
        rows="2"
        autosize
        type="textarea"
        placeholder="请输入备注，如口味偏好等"
      />
    </van-cell-group>
    
    <!-- 支付方式说明 -->
    <van-cell-group title="支付说明" style="margin-top: 10px">
      <van-cell title="支付方式" value="到店扫码支付" />
    </van-cell-group>
    
    <!-- 提交按钮 -->
    <div class="submit-area">
      <van-button
        type="primary"
        size="large"
        round
        :loading="submitting"
        @click="submitOrder"
      >
        确认下单
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.order-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 100px;
}

.price {
  color: #f44;
  font-size: 14px;
}

.total {
  font-weight: bold;
  font-size: 16px;
}

.total :deep(.van-cell__value) {
  color: #f44;
  font-size: 18px;
}

.submit-area {
  padding: 20px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
}
</style>
