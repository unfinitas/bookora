'use server'
import { StatusCodes } from 'http-status-codes'
import { AuthApiErrorEnum, RequestEnum } from '@/endpoints/requestEnum'
import { sendRequest } from '@/endpoints/gateway'
import { ApiResponse } from '@/types/typeUtils'
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
import { AuthToken } from '@/endpoints/models/auth/base'

/**
 * Error response type for failed requests
 */
interface ErrorResponseData {
  message: string
}

/**
 * Login by username/email
 */
export async function LoginByUsername(
  auth: LocalAuthParams,
): Promise<[null, AuthResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/login',
    auth,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({
      message: 'Login failed'
    })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.FORBIDDEN) {
      return [AuthApiErrorEnum.FORBIDDEN, null, errorBody.message || 'Incorrect email or password']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'An error occurred']
  }

  const responseBody = await response.json() as ApiResponse<AuthResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Register new user
 */
export async function RegisterUser(
  params: RegisterParams,
): Promise<[null, RegisterResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/register',
    params,
  )

  if (response.status !== StatusCodes.CREATED && response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Registration failed' })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.CONFLICT) {
      return [AuthApiErrorEnum.CONFLICT, null, errorBody.message || 'Email or username already exists']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Registration failed']
  }

  const responseBody = await response.json() as ApiResponse<RegisterResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Refresh access token
 */
export async function RefreshToken(
  refreshToken: string,
): Promise<[null, AuthResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/refresh',
    { refreshToken },
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Token refresh failed' })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.UNAUTHORIZED) {
      return [AuthApiErrorEnum.UNAUTHORIZED, null, errorBody.message || 'Session expired']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Failed to refresh token']
  }

  const responseBody = await response.json() as ApiResponse<AuthResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Logout user
 */
export async function LogoutUser(
  token: AuthToken,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    token,
    RequestEnum.POST,
    '/api/auth/logout',
    {},
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Logout failed' })) as ApiResponse<ErrorResponseData>
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Logout failed']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Verify email with token
 */
export async function VerifyEmail(
  params: VerifyEmailParams,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/verify-email',
    params,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Verification failed' })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.BAD_REQUEST) {
      return [AuthApiErrorEnum.INVALID_TOKEN, null, errorBody.message || 'Invalid or expired token']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Email verification failed']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Resend verification email
 */
export async function ResendVerificationEmail(
  params: ResendVerificationEmailParams,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/resend-verification',
    params,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Failed to resend' })) as ApiResponse<ErrorResponseData>
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Failed to resend verification email']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Request password reset
 */
export async function RequestPasswordReset(
  params: RequestPasswordResetParams,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/forgot-password',
    params,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Request failed' })) as ApiResponse<ErrorResponseData>
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Failed to request password reset']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Reset password with token
 */
export async function ResetPassword(
  params: ResetPasswordParams,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    null,
    RequestEnum.POST,
    '/api/auth/reset-password',
    params,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Reset failed' })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.BAD_REQUEST) {
      return [AuthApiErrorEnum.INVALID_TOKEN, null, errorBody.message || 'Invalid or expired reset token']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Failed to reset password']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}

/**
 * Change password (authenticated)
 */
export async function ChangePassword(
  token: AuthToken,
  params: ChangePasswordParams,
): Promise<[null, MessageResponse, string] | [AuthApiErrorEnum, null, string]> {
  const response = await sendRequest(
    token,
    RequestEnum.POST,
    '/api/auth/change-password',
    params,
  )

  if (response.status !== StatusCodes.OK) {
    const errorBody = await response.json().catch(() => ({ message: 'Change failed' })) as ApiResponse<ErrorResponseData>

    if (response.status === StatusCodes.FORBIDDEN) {
      return [AuthApiErrorEnum.FORBIDDEN, null, errorBody.message || 'Current password is incorrect']
    }
    return [AuthApiErrorEnum.UNKNOWN, null, errorBody.message || 'Failed to change password']
  }

  const responseBody = await response.json() as ApiResponse<MessageResponse>
  return [null, responseBody.data, responseBody.message]
}
