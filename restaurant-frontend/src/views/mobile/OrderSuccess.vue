<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const goHome = () => {
  cartStore.clearCart()
  router.push('/m/' + (cartStore.tableNo || ''))
}

const viewOrder = () => {
  // 可以跳转到订单详情（如果有）
  showToast('订单查询功能开发中')
}
</script>

<template>
  <div class="order-success-page">
    <div class="success-icon">
      <van-icon name="checked" size="80" color="#07c160" />
    </div>
    
    <div class="success-title">订单提交成功！</div>
    
    <div class="success-desc">
      您的订单已发送至后厨，请耐心等待
    </div>
    
    <div class="tips">
      <van-icon name="info-o" />
      如需加菜，请扫描桌台二维码重新进入
    </div>
    
    <div class="actions">
      <van-button type="primary" block round size="large" @click="goHome">
        返回首页
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.order-success-page {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}

.success-icon {
  margin-bottom: 30px;
}

.success-title {
  font-size: 24px;
  font-weight: bold;
  color: #07c160;
  margin-bottom: 15px;
}

.success-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 40px;
  line-height: 1.6;
}

.tips {
  font-size: 14px;
  color: #999;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.actions {
  width: 100%;
  max-width: 300px;
}
</style>
