import {
  FirstName,
  LastName,
  Username,
  UserEmail,
  Password,
  Token,
} from './base'

export interface LocalAuthParams {
  username: Username
  password: Password
}

export interface RegisterParams {
  firstName: FirstName
  lastName: LastName
  username: Username
  email: UserEmail
  password: Password
}

export interface VerifyEmailParams {
  token: Token
}

export interface ResendVerificationEmailParams {
  email: UserEmail
}

export interface RequestPasswordResetParams {
  email: UserEmail
}

export interface ResetPasswordParams {
  token: Token
  newPassword: Password
}

export interface ChangePasswordParams {
  currentPassword: Password
  newPassword: Password
}
