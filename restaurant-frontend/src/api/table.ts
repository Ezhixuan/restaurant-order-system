import request from '@/utils/request'

export function getTables(params?: { type?: number }): Promise<any[]> {
  return request.get('/tables', { params })
}

export function createTable(data: any): Promise<any> {
  return request.post('/tables', data)
}

export function updateTable(id: number, data: any): Promise<any> {
  return request.put(`/tables/${id}`, data)
}

export function deleteTable(id: number): Promise<any> {
  return request.delete(`/tables/${id}`)
}

export function openTable(id: number, customerCount?: number): Promise<any> {
  return request.post(`/tables/${id}/open`, null, { params: { customerCount } })
}

export function clearTable(id: number): Promise<any> {
  return request.post(`/tables/${id}/clear`)
}

export function setPendingClear(id: number): Promise<any> {
  return request.post(`/tables/${id}/pending-clear`)
}
