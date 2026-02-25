import request from '@/utils/request'

export function getTodayStats(): Promise<any> {
  return request.get('/reports/today')
}

export function getTopDishes(limit: number = 10): Promise<any[]> {
  return request.get('/reports/top-dishes', { params: { limit } })
}

export function getTableStats(): Promise<any[]> {
  return request.get('/reports/tables')
}
