import request from '@/utils/request'

export function getTables(params?: { type?: number }) {
  return request.get('/tables', { params })
}

export function createTable(data: any) {
  return request.post('/tables', data)
}

export function updateTable(id: number, data: any) {
  return request.put(`/tables/${id}`, data)
}

export function deleteTable(id: number) {
  return request.delete(`/tables/${id}`)
}

export function openTable(id: number, customerCount?: number) {
  return request.post(`/tables/${id}/open`, null, { params: { customerCount } })
}

export function clearTable(id: number) {
  return request.post(`/tables/${id}/clear`)
}
