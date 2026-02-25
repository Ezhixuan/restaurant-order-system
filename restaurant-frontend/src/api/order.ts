import request from '@/utils/request'

export function getOrders(params?: { status?: number }) {
  return request.get('/orders', { params })
}

export function getActiveOrders() {
  return request.get('/orders/active')
}

export function getOrderDetail(id: number) {
  return request.get(`/orders/${id}`)
}

export function createOrder(data: any) {
  return request.post('/orders', data)
}

export function addDishToOrder(orderId: number, data: any) {
  return request.post(`/orders/${orderId}/add`, data)
}

export function payOrder(orderId: number, data: any) {
  return request.post(`/orders/${orderId}/pay`, data)
}

export function updateItemStatus(itemId: number, status: number) {
  return request.post(`/orders/items/${itemId}/status`, null, { params: { status } })
}

export function completeOrder(orderId: number) {
  return request.post(`/orders/${orderId}/complete`)
}

export function cancelOrder(orderId: number) {
  return request.post(`/orders/${orderId}/cancel`)
}
