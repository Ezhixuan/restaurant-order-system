import request from '@/utils/request'
import type { SpecItem } from '@/types'

export type { SpecItem } from '@/types'

// 获取菜品的所有规格
export function getDishSpecs(dishId: number): Promise<SpecItem[]> {
  return request.get(`/dish-specs/dish/${dishId}`)
}

// 获取菜品的所有规格(包含禁用)
export function getAllDishSpecs(dishId: number): Promise<SpecItem[]> {
  return request.get(`/dish-specs/dish/${dishId}/all`)
}

// 获取单个规格
export function getSpecById(id: number): Promise<SpecItem> {
  return request.get(`/dish-specs/${id}`)
}

// 创建规格
export function createSpec(data: {
  dishId: number
  name: string
  price: number
  sortOrder?: number
}): Promise<SpecItem> {
  return request.post('/dish-specs', data)
}

// 更新规格
export function updateSpec(id: number, data: Partial<SpecItem>): Promise<void> {
  return request.put(`/dish-specs/${id}`, data)
}

// 删除规格
export function deleteSpec(id: number): Promise<void> {
  return request.delete(`/dish-specs/${id}`)
}

// 批量更新规格 (Pad 端专用)
export function batchUpdateSpecs(dishId: number, specs: SpecItem[]): Promise<void> {
  return request.post(`/dish-specs/batch/${dishId}`, specs)
}

// 启用/禁用规格
export function toggleSpecStatus(id: number): Promise<void> {
  return request.post(`/dish-specs/${id}/toggle`)
}

// 切换菜品规格模式
export function toggleDishSpecMode(dishId: number): Promise<void> {
  return request.post(`/dishes/${dishId}/toggle-specs`)
}
