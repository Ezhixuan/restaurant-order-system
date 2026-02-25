import request from '@/utils/request'

export function login(data: { username: string; password: string }): Promise<any> {
  return request.post('/auth/login', data)
}
