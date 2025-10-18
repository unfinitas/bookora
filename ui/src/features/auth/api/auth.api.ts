import {
  AuthResponse,
  RefreshTokenResponse,
  RegisterData,
  LoginRequestData,
  RefreshTokenRequest,
  RegisterSuccessResponse,
} from '@/types/authTypes'
import http from '@/utils/http'

export const URL_LOGIN = '/api/auth/login'
export const URL_LOGOUT = '/api/auth/logout' // TODO: add logout endpoint
export const URL_REFRESH_TOKEN = '/api/auth/refresh'
export const URL_REGISTER = '/api/auth/register'

const authApi = {
  registerAccount(body: RegisterData) {
    return http.post<RegisterSuccessResponse>(URL_REGISTER, body)
  },

  login(body: LoginRequestData) {
    const loginPayload = {
      username: body.username,
      password: body.password,
    }
    return http.post<AuthResponse>(URL_LOGIN, loginPayload)
  },

  refreshToken(refreshToken: string) {
    return http.post<RefreshTokenResponse>(
      URL_REFRESH_TOKEN,
      {},
      {
        headers: {
          Authorization: `Bearer ${refreshToken}`,
        },
      },
    )
  },
  // TODO: add logout endpoint
  logout(body: RefreshTokenRequest) {
    return http.post<AuthResponse>(URL_LOGOUT, body)
  },
}

export default authApi
