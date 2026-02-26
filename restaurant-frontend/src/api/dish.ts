import request from '@/utils/request'
import type { SpecItem } from './dishSpec'

export interface Dish {
  id: number
  categoryId: number
  name: string
  description?: string
  price: number
  image?: string
  stock: number
  isRecommend: number
  status: number
  sortOrder: number
  hasSpecs: number
  specs?: SpecItem[]
}

export interface Category {
  id: number
  name: string
  sortOrder: number
  status: number
}

// 获取分类列表
export function getCategories(): Promise<Category[]> {
  return request.get('/dishes/categories')
}

// 创建分类
export function createCategory(data: { name: string; sortOrder?: number }): Promise<void> {
  return request.post('/dishes/categories', data)
}

// 更新分类
export function updateCategory(id: number, data: { name: string; sortOrder?: number }): Promise<void> {
  return request.put(`/dishes/categories/${id}`, data)
}

// 删除分类
export function deleteCategory(id: number): Promise<void> {
  return request.delete(`/dishes/categories/${id}`)
}

// 获取所有菜品
export function getDishes(categoryId?: number, status?: number): Promise<Dish[]> {
  return request.get('/dishes', { params: { categoryId, status } })
}

// 按分类获取菜品(含规格)
export function getDishesByCategory(): Promise<{ id: number; name: string; dishes: Dish[] }[]> {
  return request.get('/dishes/by-category')
}

// 获取菜品详情(含规格)
export function getDishDetail(id: number): Promise<Dish> {
  return request.get(`/dishes/${id}/detail`)
}

// 创建菜品
export function createDish(data: Partial<Dish>): Promise<void> {
  return request.post('/dishes', data)
}

// 更新菜品
export function updateDish(id: number, data: Partial<Dish>): Promise<void> {
  return request.put(`/dishes/${id}`, data)
}

// 删除菜品
export function deleteDish(id: number): Promise<void> {
  return request.delete(`/dishes/${id}`)
}

// 切换菜品上下架
export function toggleDishStatus(id: number): Promise<void> {
  return request.post(`/dishes/${id}/toggle`)
}

// 快速修改价格
export function updateDishPrice(id: number, price: number): Promise<void> {
  return request.post(`/dishes/${id}/price`, null, { params: { price } })
}

// 切换规格模式
export function toggleDishSpecMode(id: number): Promise<void> {
  return request.post(`/dishes/${id}/toggle-specs`)
}
