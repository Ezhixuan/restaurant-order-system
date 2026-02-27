import request from '@/utils/request'
import type { LoginResult } from '@/types'

export function login(data: { username: string; password: string }): Promise<LoginResult> {
  return request.post('/auth/login', data)
}
