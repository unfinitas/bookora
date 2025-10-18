import axios, { AxiosError } from 'axios'
import HttpStatusCode from '@/utils/httpStatusCode'
import { ErrorResponse } from '@/types/utilsTypes'

export function isAxiosError<T>(error: unknown): error is AxiosError<T> {
  return axios.isAxiosError(error)
}

export function isAxiosUnprocessableEntityError<FormError>(error: unknown): error is AxiosError<FormError> {
  return isAxiosError(error) && error.response?.status === HttpStatusCode.UnprocessableEntity
}

export function isAxiosUnauthorizedError<UnauthorizedError>(error: unknown): error is AxiosError<UnauthorizedError> {
  return isAxiosError(error) && error.response?.status === HttpStatusCode.Unauthorized
}

export function isAxiosExpiredTokenError<UnauthorizedError>(error: unknown): error is AxiosError<UnauthorizedError> {
  return (
    isAxiosUnauthorizedError<ErrorResponse<{ name: string; message: string }>>(error) &&
    error.response?.data?.data?.name === 'EXPIRED_TOKEN'
  )
}

export const getErrorMessage = (error: unknown, fallback: string): string => {
  if (error instanceof AxiosError) {
    const errorData = error.response?.data as ErrorResponse<{ message: string }>
    return errorData?.message || error.message || fallback
  }

  if (error && typeof error === 'object' && 'message' in error) {
    return (error as { message: string }).message || fallback
  }

  return fallback
}
