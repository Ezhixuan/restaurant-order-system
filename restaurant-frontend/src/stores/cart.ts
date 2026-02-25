import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface CartItem {
  dishId: number
  name: string
  price: number
  image?: string
  quantity: number
  remark?: string
}

export const useCartStore = defineStore('cart', () => {
  // State
  const items = ref<CartItem[]>([])
  const tableNo = ref('')
  const customerCount = ref(1)

  // Getters
  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

  // Actions
  const addItem = (item: CartItem) => {
    const existing = items.value.find(i => i.dishId === item.dishId)
    if (existing) {
      existing.quantity += item.quantity
    } else {
      items.value.push({ ...item })
    }
  }

  const updateQuantity = (dishId: number, quantity: number) => {
    const item = items.value.find(i => i.dishId === dishId)
    if (item) {
      if (quantity <= 0) {
        removeItem(dishId)
      } else {
        item.quantity = quantity
      }
    }
  }

  const updateRemark = (dishId: number, remark: string) => {
    const item = items.value.find(i => i.dishId === dishId)
    if (item) {
      item.remark = remark
    }
  }

  const removeItem = (dishId: number) => {
    const index = items.value.findIndex(i => i.dishId === dishId)
    if (index > -1) {
      items.value.splice(index, 1)
    }
  }

  const clearCart = () => {
    items.value = []
    tableNo.value = ''
    customerCount.value = 1
  }

  return {
    items,
    tableNo,
    customerCount,
    totalCount,
    totalAmount,
    addItem,
    updateQuantity,
    updateRemark,
    removeItem,
    clearCart
  }
}, {
  persist: true  // 持久化到 localStorage
})
