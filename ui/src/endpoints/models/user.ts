export type UserId = string
export type Username = string
export type UserEmail = string

export enum UserRole {
  UNKNOWN = 'UNKNOWN',
  ADMIN = 'ADMIN',
  PROVIDER = 'PROVIDER',
  USER = 'USER',
}

export type UserPublicInfo = {
  id: UserId
  username: Username
  role: UserRole
}

export type UserInfo = UserPublicInfo

export type User = { userId: UserId } & UserInfo
