import {
  AuthToken,
  RefreshToken,
  Token,
  ExpiresIn,
  Message,
  UserId,
  Username,
  UserRole,
} from './base'

export interface AuthResponse {
  id: UserId
  username: Username
  role: UserRole
  accessToken: AuthToken
  refreshToken: RefreshToken
  tokenType: Token
  expiresIn: ExpiresIn
}

export interface RegisterResponse {
  id: UserId
  username: Username
  role: UserRole
}

export interface MessageResponse {
  message: Message
}
