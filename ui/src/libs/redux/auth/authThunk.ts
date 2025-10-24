'use client'

import { createAsyncThunk } from '@reduxjs/toolkit'
import {
  LoginByUsername,
  RegisterUser,
  RefreshToken,
  LogoutUser,
  VerifyEmail,
  ResendVerificationEmail,
  RequestPasswordReset,
  ResetPassword,
  ChangePassword,
} from '@/endpoints/bookora/authApi'
import {
  LocalAuthParams,
  RegisterParams,
  VerifyEmailParams,
  ResendVerificationEmailParams,
  RequestPasswordResetParams,
  ResetPasswordParams,
  ChangePasswordParams,
} from '@/endpoints/models/auth/authRequest'
import { AuthResponse, MessageResponse, RegisterResponse } from '@/endpoints/models/auth/authResponse'
import type { RootState } from '@/libs/redux/types'

/**
 * Utility to unwrap API tuple response.
 * Throws an error if request failed.
 */
function unwrap<T>(error: unknown, data: T | null, message: string): T {
  if (error || !data) {
    throw new Error(message)
  }
  return data
}

/**
 * Login thunk
 */
export const LoginByUsernameThunk = createAsyncThunk(
  'auth/loginByUsername',
  async (args: LocalAuthParams): Promise<AuthResponse> => {
    const [error, data, message] = await LoginByUsername(args)
    return unwrap(error, data, message)
  },
)

/**
 * Register thunk
 */
export const RegisterThunk = createAsyncThunk(
  'auth/register',
  async (args: RegisterParams): Promise<RegisterResponse> => {
    const [error, data, message] = await RegisterUser(args)
    return unwrap(error, data, message)
  },
)

/**
 * Logout thunk
 */
export const LogoutThunk = createAsyncThunk(
  'auth/logout',
  async (_, { getState }): Promise<void> => {
    const state = getState() as RootState
    const token = state.auth.token
    if (token) {
      const [error, , message] = await LogoutUser(token)
      if (error) {
        console.warn('Logout API failed:', message)
      }
    }
  },
)

/**
 * Refresh token thunk
 */
export const RefreshTokenThunk = createAsyncThunk(
  'auth/refreshToken',
  async (_, { getState }): Promise<AuthResponse> => {
    const state = getState() as RootState
    const refreshToken = state.auth.refreshToken

    if (!refreshToken) {
      throw new Error('No refresh token available')
    }

    const [error, data, message] = await RefreshToken(refreshToken)
    return unwrap(error, data, message)
  },
)

/**
 * Verify email thunk
 */
export const VerifyEmailThunk = createAsyncThunk(
  'auth/verifyEmail',
  async (args: VerifyEmailParams): Promise<MessageResponse> => {
    const [error, data, message] = await VerifyEmail(args)
    return unwrap(error, data, message)
  },
)

/**
 * Resend verification email thunk
 */
export const ResendVerificationEmailThunk = createAsyncThunk(
  'auth/resendVerificationEmail',
  async (args: ResendVerificationEmailParams): Promise<MessageResponse> => {
    const [error, data, message] = await ResendVerificationEmail(args)
    return unwrap(error, data, message)
  },
)

/**
 * Request password reset thunk
 */
export const RequestPasswordResetThunk = createAsyncThunk(
  'auth/requestPasswordReset',
  async (args: RequestPasswordResetParams): Promise<MessageResponse> => {
    const [error, data, message] = await RequestPasswordReset(args)
    return unwrap(error, data, message)
  },
)

/**
 * Reset password thunk
 */
export const ResetPasswordThunk = createAsyncThunk(
  'auth/resetPassword',
  async (args: ResetPasswordParams): Promise<MessageResponse> => {
    const [error, data, message] = await ResetPassword(args)
    return unwrap(error, data, message)
  },
)

/**
 * Change password thunk (requires token)
 */
export const ChangePasswordThunk = createAsyncThunk(
  'auth/changePassword',
  async (args: ChangePasswordParams, { getState }): Promise<MessageResponse> => {
    const state = getState() as RootState
    const token = state.auth.token

    if (!token) {
      throw new Error('Not authenticated')
    }

    const [error, data, message] = await ChangePassword(token, args)
    return unwrap(error, data, message)
  },
)

/**
 * Grouped exports for convenience
 */
const AuthThunk = {
  LoginByUsernameThunk,
  RegisterThunk,
  LogoutThunk,
  RefreshTokenThunk,
  VerifyEmailThunk,
  ResendVerificationEmailThunk,
  RequestPasswordResetThunk,
  ResetPasswordThunk,
  ChangePasswordThunk,
}

export default AuthThunk
