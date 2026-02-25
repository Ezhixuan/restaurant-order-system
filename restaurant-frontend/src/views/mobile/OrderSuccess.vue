<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useCartStore } from '@/stores/cart'
import { getOrderByTable, addDishToOrder, getActiveOrders } from '@/api/order'
import { getDishes } from '@/api/dish'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const order = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(false)
const showAddDish = ref(false)
const dishes = ref<any[]>([])
const dishesLoading = ref(false)
const selectedDish = ref<any>(null)
const quantity = ref(1)
const remark = ref('')

const tableId = ref(Number(route.query.tableId) || cartStore.tableId)
const tableNo = ref((route.query.tableNo as string) || cartStore.tableNo)

// 如果没有桌台信息，尝试从订单列表获取
const loadTableInfoFromOrder = async () => {
  if (tableId.value) return
  
  // 尝试获取最近的一个活跃订单
  try {
    const orders = await getActiveOrders()
    if (orders && orders.length > 0) {
      // 使用最新的订单
      const latestOrder = orders[0]
      tableId.value = latestOrder.tableId
      tableNo.value = latestOrder.tableNo
      // 保存到 store
      cartStore.setTableInfo(latestOrder.tableId, latestOrder.tableNo, 1)
    }
  } catch (e) {
    console.error('获取订单失败', e)
  }
}

// 加载订单详情
const loadOrder = async () => {
  if (!tableId.value) {
    showToast('桌台信息缺失')
    return
  }
  
  loading.value = true
  try {
    const detail = await getOrderByTable(tableId.value)
    if (detail) {
      order.value = detail.order
      items.value = detail.items || []
    }
  } catch (error) {
    console.error('加载订单失败', error)
  } finally {
    loading.value = false
  }
}

// 获取菜品列表（用于加菜）
const loadDishes = async () => {
  dishesLoading.value = true
  try {
    const res = await getDishes()
    dishes.value = res || []
  } catch (error) {
    showToast('加载菜品失败')
  } finally {
    dishesLoading.value = false
  }
}

// 打开加菜弹窗
const openAddDish = () => {
  showAddDish.value = true
  loadDishes()
}

// 选择菜品
const selectDish = (dish: any) => {
  selectedDish.value = dish
  quantity.value = 1
  remark.value = ''
}

// 添加菜品到订单
const confirmAddDish = async () => {
  if (!selectedDish.value) {
    showToast('请选择菜品')
    return
  }
  if (!order.value) {
    showToast('订单信息错误')
    return
  }
  
  showLoadingToast({ message: '添加中...', forbidClick: true })
  
  try {
    await addDishToOrder(order.value.id, {
      dishId: selectedDish.value.id,
      quantity: quantity.value,
      remark: remark.value
    })
    
    closeToast()
    showToast('加菜成功')
    showAddDish.value = false
    selectedDish.value = null
    
    // 刷新订单
    await loadOrder()
  } catch (error: any) {
    closeToast()
    showToast(error.message || '加菜失败')
  }
}

// 返回首页
const goHome = () => {
  router.push('/m/' + (tableNo.value || ''))
}

// 获取状态文本
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待制作',
    1: '制作中',
    2: '已完成'
  }
  return map[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  const map: Record<number, string> = {
    0: 'gray',
    1: 'orange',
    2: 'green'
  }
  return map[status] || 'gray'
}

// 计算订单总额
const totalAmount = computed(() => {
  return items.value.reduce((sum, item) => sum + (item.subtotal || 0), 0)
})

import { computed } from 'vue'

onMounted(async () => {
  await loadTableInfoFromOrder()
  await loadOrder()
})
</script>

<template>
  <div class="order-success-page">
    <!-- 成功提示 -->
    <div class="success-header">
      <div class="success-icon">
        <van-icon name="checked" size="60" color="#07c160" />
      </div>
      <div class="success-title">下单成功！</div>
      <div class="success-desc">订单已发送至后厨</div>
    </div>

    <!-- 订单信息 -->
    <div v-if="order" class="order-info">
      <div class="info-header">
        <span class="table-name">{{ order.tableNo }}号桌</span>
        <span class="order-no">{{ order.orderNo }}</span>
      </div>
      
      <div class="info-meta">
        <span>{{ order.customerCount }}人用餐</span>
        <span>{{ items.length }}道菜</span>
      </div>
    </div>

    <!-- 菜品列表 -->
    <div class="items-section">
      <div class="section-title">已点菜品</div>
      
      <div v-if="items.length > 0" class="items-list">
        <div
          v-for="item in items"
          :key="item.id"
          class="item-card"
        >
          <div class="item-image">
            <img :src="item.dishImage || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
          </div>
          
          <div class="item-info">
            <div class="item-name">{{ item.dishName }}</div>
            <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
            <div v-if="item.remark" class="item-remark">备注: {{ item.remark }}</div>
          </div>
          
          <div class="item-status">
            <van-tag :type="getStatusColor(item.status)" size="medium">
              {{ getStatusText(item.status) }}
            </van-tag>
            <div class="item-subtotal">¥{{ item.subtotal }}</div>
          </div>
        </div>
      </div>
      
      <van-empty v-else description="暂无菜品" />
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <div class="total-info">
        <span class="total-label">合计</span>
        <span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
      
      <div class="actions">
        <van-button type="default" size="small" @click="goHome">返回首页</van-button>
        <van-button type="primary" size="small" @click="openAddDish">继续加菜</van-button>
      </div>
    </div>

    <!-- 加菜弹窗 -->
    <van-popup
      v-model:show="showAddDish"
      position="bottom"
      round
      closeable
      :style="{ height: '80%' }"
    >
      <div class="add-dish-popup">
        <div class="popup-title">继续加菜</div>
        
        <!-- 选择菜品 -->
        <div v-if="!selectedDish" class="dish-list">
          <div
            v-for="dish in dishes"
            :key="dish.id"
            class="dish-item"
            @click="selectDish(dish)"
          >
            <div class="dish-image">
              <img :src="dish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
            </div>            
            <div class="dish-info">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-price">¥{{ dish.price }}</div>
            </div>
            
            <van-icon name="add" class="add-icon" />
          </div>
        </div>
        
        <!-- 确认添加 -->
        <div v-else class="confirm-add">
          <div class="selected-dish">
            <div class="dish-image">
              <img :src="selectedDish.image || 'https://img.yzcdn.cn/vant/ipad.jpeg'" />
            </div>            
            <div class="dish-info">
              <div class="dish-name">{{ selectedDish.name }}</div>
              <div class="dish-price">¥{{ selectedDish.price }}</div>
            </div>
          </div>
          
          <van-divider />
          
          <div class="form-item">
            <div class="label">数量</div>            
            <van-stepper v-model="quantity" :min="1" :max="99" />
          </div>
          
          <div class="form-item">
            <div class="label">备注</div>            
            <van-field
              v-model="remark"
              placeholder="请输入备注（如：少辣、免葱等）"
              maxlength="50"
              show-word-limit
            />
          </div>
          
          <div class="confirm-actions">
            <van-button type="default" block @click="selectedDish = null">重新选择</van-button>
            <van-button type="primary" block @click="confirmAddDish">
              确认添加 ¥{{ (selectedDish.price * quantity).toFixed(2) }}
            </van-button>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.order-success-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 80px;
}

.success-header {
  background: linear-gradient(135deg, #07c160 0%, #05a050 100%);
  padding: 40px 20px;
  text-align: center;
  color: #fff;
}

.success-icon {
  margin-bottom: 15px;
}

.success-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  opacity: 0.9;
}

.order-info {
  background: #fff;
  padding: 15px 20px;
  margin: 10px;
  border-radius: 12px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.table-name {
  font-size: 18px;
  font-weight: bold;
  color: #323233;
}

.order-no {
  font-size: 12px;
  color: #969799;
}

.info-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.items-section {
  padding: 0 10px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  padding: 15px 10px 10px;
  color: #323233;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-card {
  background: #fff;
  border-radius: 12px;
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-image {
  width: 70px;
  height: 70px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 6px;
}

.item-price {
  font-size: 13px;
  color: #969799;
}

.item-remark {
  font-size: 12px;
  color: #ff976a;
  margin-top: 4px;
}

.item-status {
  text-align: right;
  min-width: 70px;
}

.item-subtotal {
  font-size: 16px;
  font-weight: bold;
  color: #ee0a24;
  margin-top: 6px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.total-info {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.total-label {
  font-size: 14px;
  color: #969799;
}

.total-price {
  font-size: 24px;
  font-weight: bold;
  color: #ee0a24;
}

.actions {
  display: flex;
  gap: 10px;
}

/* 加菜弹窗 */
.add-dish-popup {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.popup-title {
  font-size: 18px;
  font-weight: bold;
  padding: 15px 20px;
  text-align: center;
  border-bottom: 1px solid #ebedf0;
}

.dish-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.dish-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 12px;
  margin-bottom: 10px;
  gap: 12px;
}

.dish-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-info {
  flex: 1;
}

.dish-name {
  font-size: 15px;
  font-weight: 500;
  color: #323233;
  margin-bottom: 5px;
}

.dish-price {
  font-size: 16px;
  font-weight: bold;
  color: #ee0a24;
}

.add-icon {
  font-size: 24px;
  color: #07c160;
}

.confirm-add {
  padding: 20px;
}

.selected-dish {
  display: flex;
  align-items: center;
  gap: 15px;
  padding-bottom: 15px;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  gap: 15px;
}

.form-item .label {
  width: 60px;
  font-size: 15px;
  color: #323233;
}

.form-item :deep(.van-field) {
  flex: 1;
  padding: 0;
}

.confirm-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}
</style>
