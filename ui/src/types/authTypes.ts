import { SuccessResponse, ErrorResponse } from './utilsTypes'
import { User } from './userType'

export interface LoginData {
  id: string
  username: string
  role: string
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface UserPublicInfo {
  id: string
  username: string
  role: string
}

export interface RegisterData {
  username: string
  firstName: string
  lastName: string
  email: string
  password: string
  phoneNumber?: string
}

export interface LoginRequestData {
  username: string
  password: string
}

export type AuthResponse = SuccessResponse<{
  access_token: string
  refresh_token: string
  expires_refresh_token: number
  expires: number
  user: User
}>

export type LoginSuccessResponse = SuccessResponse<LoginData>
export type LoginErrorResponse = ErrorResponse<Record<string, string>>

export type RegisterSuccessResponse = SuccessResponse<UserPublicInfo>
export type RegisterErrorResponse = ErrorResponse<Record<string, string>>

export type RefreshTokenSuccessResponse = SuccessResponse<LoginData>
export type RefreshTokenErrorResponse = ErrorResponse<Record<string, string>>

export interface AuthState {
  user: UserPublicInfo | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

export interface AuthContextType {
  authState: AuthState
  login: (credentials: LoginRequestData) => Promise<void>
  register: (userData: RegisterData) => Promise<void>
  logout: () => void
  refreshToken: () => Promise<void>
  clearError: () => void
}

export interface RefreshTokenRequest {
  refresh_token: string
}

export type RefreshTokenResponse = SuccessResponse<{
  access_token: string
  refresh_token: string
  expires_refresh_token: number
  expires: number
  user: User
}>

export interface JWTPayload {
  sub: string
  iat: number
  exp: number
  role: string
  userId: string
}
