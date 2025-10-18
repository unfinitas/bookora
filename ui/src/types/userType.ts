export interface User {
  id: string
  username: string
  firstName: string
  lastName: string
  email: string
  role: string
  isGuest: boolean
  phoneNumber?: string
  createdAt: string
  updatedAt: string
}

export interface UserPublicInfo {
  id: string
  username: string
  role: string
}

export const UserRole = {
  USER: 'USER',
  PROVIDER: 'PROVIDER',
  ADMIN: 'ADMIN',
} as const

export type UserRoleType = (typeof UserRole)[keyof typeof UserRole]

export interface UpdateUserData {
  firstName?: string
  lastName?: string
  email?: string
  phoneNumber?: string
}

export interface UserProfile {
  id: string
  username: string
  firstName: string
  lastName: string
  email: string
  role: string
  phoneNumber?: string
  fullName: string
}
