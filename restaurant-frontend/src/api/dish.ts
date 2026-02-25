import request from '@/utils/request'

export function getCategories(): Promise<any[]> {
  return request.get('/dishes/categories')
}

export function createCategory(data: any): Promise<any> {
  return request.post('/dishes/categories', data)
}

export function updateCategory(id: number, data: any): Promise<any> {
  return request.put(`/dishes/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<any> {
  return request.delete(`/dishes/categories/${id}`)
}

export function getDishes(params?: { categoryId?: number; status?: number }): Promise<any[]> {
  return request.get('/dishes', { params })
}

export function getDishesByCategory(): Promise<any[]> {
  return request.get('/dishes/by-category')
}

export function createDish(data: any): Promise<any> {
  return request.post('/dishes', data)
}

export function updateDish(id: number, data: any): Promise<any> {
  return request.put(`/dishes/${id}`, data)
}

export function deleteDish(id: number): Promise<any> {
  return request.delete(`/dishes/${id}`)
}

export function toggleDishStatus(id: number): Promise<any> {
  return request.post(`/dishes/${id}/toggle`)
}
