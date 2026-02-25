import request from '@/utils/request'

export function getCategories() {
  return request.get('/dishes/categories')
}

export function createCategory(data: any) {
  return request.post('/dishes/categories', data)
}

export function updateCategory(id: number, data: any) {
  return request.put(`/dishes/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete(`/dishes/categories/${id}`)
}

export function getDishes(params?: { categoryId?: number; status?: number }) {
  return request.get('/dishes', { params })
}

export function getDishesByCategory() {
  return request.get('/dishes/by-category')
}

export function createDish(data: any) {
  return request.post('/dishes', data)
}

export function updateDish(id: number, data: any) {
  return request.put(`/dishes/${id}`, data)
}

export function deleteDish(id: number) {
  return request.delete(`/dishes/${id}`)
}

export function toggleDishStatus(id: number) {
  return request.post(`/dishes/${id}/toggle`)
}
