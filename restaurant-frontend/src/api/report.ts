import request from '@/utils/request'
import type { TodayStats, TopDish, TableStat } from '@/types'

export function getTodayStats(): Promise<TodayStats> {
  return request.get('/reports/today')
}

export function getTopDishes(limit: number = 10): Promise<TopDish[]> {
  return request.get('/reports/top-dishes', { params: { limit } })
}

export function getTableStats(): Promise<TableStat[]> {
  return request.get('/reports/tables')
}
