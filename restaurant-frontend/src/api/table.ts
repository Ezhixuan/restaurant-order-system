import request from '@/utils/request'
import type { Table, TableCreateRequest, TableUpdateRequest } from '@/types'

export function getTables(params?: { type?: number }): Promise<Table[]> {
  return request.get('/tables', { params })
}

export function createTable(data: TableCreateRequest): Promise<Table> {
  return request.post('/tables', data)
}

export function updateTable(id: number, data: TableUpdateRequest): Promise<Table> {
  return request.put(`/tables/${id}`, data)
}

export function deleteTable(id: number): Promise<void> {
  return request.delete(`/tables/${id}`)
}

export function openTable(id: number, customerCount?: number): Promise<Table> {
  return request.post(`/tables/${id}/open`, null, { params: { customerCount } })
}

export function clearTable(id: number): Promise<Table> {
  return request.post(`/tables/${id}/clear`)
}

export function setPendingClear(id: number): Promise<Table> {
  return request.post(`/tables/${id}/pending-clear`)
}
