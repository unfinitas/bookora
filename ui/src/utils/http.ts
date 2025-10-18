import axios, { AxiosError, type AxiosInstance } from 'axios'
import { URL_LOGIN, URL_LOGOUT, URL_REFRESH_TOKEN, URL_REGISTER } from '@/features/auth/api/auth.api'
import config from '@/utils/config'
import HttpStatusCode from '@/utils/httpStatusCode'
import { toast } from '@/hooks/use-toast'
import { type RefreshTokenResponse } from '@/types/authTypes'
import { type ErrorResponse } from '@/types/utilsTypes'
import {
  clearLS,
  getAccessTokenFromStorage,
  getRefreshTokenFromStorage,
  setAccessTokenToStorage,
  setProfileToStorage,
  setRefreshTokenToStorage,
  getRememberMe,
} from '@/utils/auth'
import { isAxiosExpiredTokenError, isAxiosUnauthorizedError } from '@/utils/utils'

export class Http {
  instance: AxiosInstance
  private accessToken: string
  private refreshToken: string
  private refreshTokenRequest: Promise<string> | null

  constructor() {
    this.accessToken = getAccessTokenFromStorage()
    this.refreshToken = getRefreshTokenFromStorage()
    this.refreshTokenRequest = null
    this.instance = axios.create({
      baseURL: config.baseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'expire-access-token': 60 * 60 * 24, // 24 hours
        'expire-refresh-token': 60 * 60 * 24 * 7, // 7 days
      },
    })

    this.instance.interceptors.request.use(
      (config) => {
        if (this.accessToken && config.headers) {
          config.headers.authorization = this.accessToken
          return config
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      },
    )

    this.instance.interceptors.response.use(
      (response) => {
        const { url } = response.config
        if (url === URL_LOGIN) {
          const data = response.data
          if (data?.data) {
            this.accessToken = data.data.accessToken
            this.refreshToken = data.data.refreshToken
            const rememberMe = getRememberMe()
            setAccessTokenToStorage(this.accessToken, rememberMe)
            setRefreshTokenToStorage(this.refreshToken, rememberMe)
            const userPublicInfo = {
              id: data.data.id,
              username: data.data.username,
              role: data.data.role as 'USER' | 'PROVIDER' | 'ADMIN',
            }
            setProfileToStorage(userPublicInfo, rememberMe)
          }
        } else if (url === URL_REGISTER) {
          const data = response.data
          if (data?.data) {
            const userPublicInfo = {
              id: data.data.id,
              username: data.data.username,
              role: data.data.role as 'USER' | 'PROVIDER' | 'ADMIN',
            }
            setProfileToStorage(userPublicInfo, getRememberMe())
          }
        } else if (url === URL_LOGOUT) {
          this.accessToken = ''
          this.refreshToken = ''
          clearLS()
        }
        return response
      },
      (error: AxiosError) => {
        if (
          ![HttpStatusCode.UnprocessableEntity, HttpStatusCode.Unauthorized].includes(error.response?.status as number)
        ) {
          const data = error.response?.data as ErrorResponse<{ message: string }>
          const message = data?.message || error.message
          toast({
            title: 'Error',
            description: message,
            variant: 'destructive',
          })
        }
        if (isAxiosUnauthorizedError<ErrorResponse<{ message: string }>>(error)) {
          const config = error.response?.config || { headers: {}, url: '' }
          const { url } = config

          if (isAxiosExpiredTokenError(error) && url !== URL_REFRESH_TOKEN) {
            this.refreshTokenRequest = this.refreshTokenRequest
              ? this.refreshTokenRequest
              : this.handleRefreshToken().finally(() => {
                  setTimeout(() => {
                    this.refreshTokenRequest = null
                  }, 10000)
                })
            return this.refreshTokenRequest.then((access_token) => {
              return this.instance({
                ...config,
                headers: { ...config.headers, authorization: access_token },
              })
            })
          }

          // Don't clear LS here - let Redux handle it
          this.accessToken = ''
          this.refreshToken = ''

          if (url !== URL_REFRESH_TOKEN) {
            toast({
              title: 'Error',
              description: error.response?.data?.message || error.message,
              variant: 'destructive',
            })
          }
        }
        return Promise.reject(error)
      },
    )
  }

  private handleRefreshToken() {
    return this.instance
      .post<RefreshTokenResponse>(
        URL_REFRESH_TOKEN,
        {},
        {
          headers: {
            Authorization: `Bearer ${this.refreshToken}`,
          },
        },
      )
      .then((res) => {
        const { access_token } = res.data.data
        const rememberMe = getRememberMe()
        setAccessTokenToStorage(access_token, rememberMe)
        this.accessToken = access_token
        return access_token
      })
      .catch((error) => {
        // Don't clear LS here - let Redux handle it
        this.accessToken = ''
        this.refreshToken = ''
        throw error
      })
  }
}

let httpInstance: AxiosInstance | null = null

const getHttpInstance = (): AxiosInstance => {
  if (!httpInstance && typeof window !== 'undefined') {
    httpInstance = new Http().instance
  }
  if (!httpInstance) {
    return axios.create({
      baseURL: config.baseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })
  }
  return httpInstance
}

export default getHttpInstance()
