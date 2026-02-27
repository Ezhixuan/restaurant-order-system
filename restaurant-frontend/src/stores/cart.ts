import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem } from '@/types'

export type { CartItem } from '@/types'

export const useCartStore = defineStore('cart', () => {
  // State
  const items = ref<CartItem[]>([])
  const tableNo = ref('')
  const tableId = ref(0)
  const customerCount = ref(1)

  // Getters
  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalAmount = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  // Actions
  const addItem = (item: CartItem) => {
    // 检查是否已存在相同菜品+规格
    const key = item.specId ? `${item.dishId}-${item.specId}` : `${item.dishId}`
    const existingIndex = items.value.findIndex(i => {
      const existingKey = i.specId ? `${i.dishId}-${i.specId}` : `${i.dishId}`
      return existingKey === key
    })

    if (existingIndex > -1) {
      items.value[existingIndex].quantity += item.quantity
    } else {
      items.value.push({ ...item })
    }
  }

  const updateQuantity = (dishId: number, quantity: number, specId?: number) => {
    const key = specId ? `${dishId}-${specId}` : `${dishId}`
    const index = items.value.findIndex(i => {
      const existingKey = i.specId ? `${i.dishId}-${i.specId}` : `${i.dishId}`
      return existingKey === key
    })

    if (index > -1) {
      if (quantity <= 0) {
        items.value.splice(index, 1)
      } else {
        items.value[index].quantity = quantity
      }
    }
  }

  const updateRemark = (dishId: number, remark: string, specId?: number) => {
    const key = specId ? `${dishId}-${specId}` : `${dishId}`
    const item = items.value.find(i => {
      const existingKey = i.specId ? `${i.dishId}-${i.specId}` : `${i.dishId}`
      return existingKey === key
    })
    if (item) {
      item.remark = remark
    }
  }

  const removeItem = (dishId: number, specId?: number) => {
    const key = specId ? `${dishId}-${specId}` : `${dishId}`
    const index = items.value.findIndex(i => {
      const existingKey = i.specId ? `${i.dishId}-${i.specId}` : `${i.dishId}`
      return existingKey === key
    })
    if (index > -1) {
      items.value.splice(index, 1)
    }
  }

  const clearCart = () => {
    items.value = []
    tableNo.value = ''
    tableId.value = 0
    customerCount.value = 1
  }

  const setTableInfo = (id: number, no: string, count: number = 1) => {
    tableId.value = id
    tableNo.value = no
    customerCount.value = count
  }

  return {
    items,
    tableNo,
    tableId,
    customerCount,
    totalCount,
    totalAmount,
    addItem,
    updateQuantity,
    updateRemark,
    removeItem,
    clearCart,
    setTableInfo,
  }
})
