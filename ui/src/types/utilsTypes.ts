export interface SuccessResponse<Data> {
  status: string
  message: string
  data: Data
  timestamp: string
}

export interface ErrorResponse<Data> {
  status: string
  message: string
  data: Data
  timestamp: string
}

export type NoUndefinedField<T> = {
  [P in keyof T]-?: NoUndefinedField<NonNullable<T[P]>>
}
