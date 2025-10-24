'use server'

import { RequestEnum } from '@/endpoints/requestEnum'
import { sendRequest as fetchApi } from '@/endpoints/request'
import { AuthToken } from '@/types/auth'

export async function fetch(method: RequestEnum, url: string, headers?: object, data?: object) {
  return fetchApi(method, url, { 'Content-Type': 'application/json', ...headers }, data)
}

export async function sendRequest(token: AuthToken | null, method: RequestEnum, url: string, data?: object) {
  if (!token) {
    return fetch(method, url, {}, data)
  }
  return fetch(method, url, { 'Authorization': `Bearer ${token}` }, data)
}
