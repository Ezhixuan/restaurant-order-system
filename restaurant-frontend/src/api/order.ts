import request from '@/utils/request'
import type {
  Order,
  OrderItem,
  OrderCreateRequest,
  AddDishRequest,
  BatchAddRequest,
  PayRequest,
} from '@/types'

export function getOrders(params?: { status?: number }): Promise<Order[]> {
  return request.get('/orders', { params })
}

export function getActiveOrders(): Promise<Order[]> {
  return request.get('/orders/active')
}

export function getOrderDetail(id: number): Promise<Order> {
  return request.get(`/orders/${id}`)
}

export function createOrder(data: OrderCreateRequest): Promise<Order> {
  return request.post('/orders', data)
}

export function addDishToOrder(orderId: number, data: AddDishRequest): Promise<Order> {
  return request.post(`/orders/${orderId}/add`, data)
}

export function batchAddDishToOrder(data: BatchAddRequest): Promise<Order> {
  return request.post('/orders/batch-add', data)
}

export function payOrder(orderId: number, data: PayRequest): Promise<Order> {
  return request.post(`/orders/${orderId}/pay`, data)
}

export function updateItemStatus(itemId: number, status: number): Promise<OrderItem> {
  return request.post(`/orders/items/${itemId}/status`, null, { params: { status } })
}

export function completeOrder(orderId: number): Promise<Order> {
  return request.post(`/orders/${orderId}/complete`)
}

export function cancelOrder(orderId: number): Promise<Order> {
  return request.post(`/orders/${orderId}/cancel`)
}

export function getOrderByTable(tableId: number): Promise<Order | null> {
  return request.get(`/orders/by-table/${tableId}`)
}

export function getUnpaidAmount(orderId: number): Promise<number> {
  return request.get(`/orders/${orderId}/unpaid-amount`)
}
