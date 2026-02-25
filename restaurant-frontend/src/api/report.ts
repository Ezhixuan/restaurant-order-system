import request from '@/utils/request'

export function getTodayStats() {
  return request.get('/reports/today')
}

export function getTopDishes(limit: number = 10) {
  return request.get('/reports/top-dishes', { params: { limit } })
}

export function getTableStats() {
  return request.get('/reports/tables')
}
