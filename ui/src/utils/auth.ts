import { UserPublicInfo } from '@/schemas/userSchema'

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const PROFILE_KEY = 'profile'
const REMEMBER_ME_KEY = 'remember_me'

const isBrowser = typeof window !== 'undefined'

export const getAccessTokenFromLS = (): string => {
  if (!isBrowser) return ''
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export const getRefreshTokenFromLS = (): string => {
  if (!isBrowser) return ''
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export const getProfileFromLS = (): UserPublicInfo | null => {
  if (!isBrowser) return null
  const profile = localStorage.getItem(PROFILE_KEY)
  return profile ? JSON.parse(profile) : null
}

export const setAccessTokenToLS = (accessToken: string): void => {
  if (!isBrowser) return
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export const setRefreshTokenToLS = (refreshToken: string): void => {
  if (!isBrowser) return
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export const setProfileToLS = (profile: UserPublicInfo): void => {
  if (!isBrowser) return
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export const saveProfileToLS = (profile: UserPublicInfo): void => {
  if (!isBrowser) return
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export const clearLS = (): void => {
  if (!isBrowser) return
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(PROFILE_KEY)
  localStorage.removeItem(REMEMBER_ME_KEY)

  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(PROFILE_KEY)
}

// Remember me function
export const setRememberMe = (remember: boolean): void => {
  if (!isBrowser) return
  if (remember) {
    localStorage.setItem(REMEMBER_ME_KEY, 'true')
  } else {
    localStorage.removeItem(REMEMBER_ME_KEY)
  }
}

export const getRememberMe = (): boolean => {
  if (!isBrowser) return false
  return localStorage.getItem(REMEMBER_ME_KEY) === 'true'
}

// Storage functions that respect remember me setting
export const setAccessTokenToStorage = (accessToken: string, remember: boolean = false): void => {
  if (!isBrowser) return
  if (remember) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  } else {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  }
}

export const setRefreshTokenToStorage = (refreshToken: string, remember: boolean = false): void => {
  if (!isBrowser) return
  if (remember) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } else {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
}

export const setProfileToStorage = (profile: UserPublicInfo, remember: boolean = false): void => {
  if (!isBrowser) return
  if (remember) {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
  } else {
    sessionStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
  }
}

// Get tokens from either localStorage or sessionStorage
export const getAccessTokenFromStorage = (): string => {
  if (!isBrowser) return ''
  return localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export const getRefreshTokenFromStorage = (): string => {
  if (!isBrowser) return ''
  return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export const getProfileFromStorage = (): UserPublicInfo | null => {
  if (!isBrowser) return null
  const profile = localStorage.getItem(PROFILE_KEY) || sessionStorage.getItem(PROFILE_KEY)
  return profile ? JSON.parse(profile) : null
}
