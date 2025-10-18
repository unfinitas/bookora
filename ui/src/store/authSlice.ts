import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import authApi from '@/features/auth/api/auth.api'
import { type LoginRequestData, type RegisterData, type AuthState } from '@/types/authTypes'
import { type UserPublicInfo } from '@/schemas/userSchema'
import { getErrorMessage } from '@/utils/utils'
import {
  setAccessTokenToLS,
  setRefreshTokenToLS,
  setProfileToLS,
  clearLS,
  setAccessTokenToStorage,
  setRefreshTokenToStorage,
  setProfileToStorage,
  setRememberMe,
} from '@/utils/auth'

/**
 * Maps raw user data from API response to UserPublicInfo type
 * Validates user data structure and role before returning
 * @param user - Raw user data from API (unknown type for safety)
 * @returns Validated UserPublicInfo object
 * @throws Error if user data is invalid or role is not supported
 */
const mapUserData = (user: unknown): UserPublicInfo => {
  if (!user || typeof user !== 'object' || user === null) {
    throw new Error('Invalid user data received')
  }

  const userObj = user as Record<string, unknown>
  if (!userObj.id || !userObj.username) {
    throw new Error('Invalid user data received')
  }

  const validRoles = ['USER', 'PROVIDER', 'ADMIN'] as const
  if (!validRoles.includes(userObj.role as (typeof validRoles)[number])) {
    throw new Error(`Invalid user role: ${userObj.role}`)
  }

  return {
    id: userObj.id as string,
    username: userObj.username as string,
    role: userObj.role as 'USER' | 'PROVIDER' | 'ADMIN',
  }
}

/**
 * Extracts authentication data from API response payload
 * Validates response structure and extracts user, access token, and refresh token
 * @param payload - Raw API response payload (unknown type for safety)
 * @returns Object containing validated user data and tokens
 * @throws Error if payload structure is invalid or required data is missing
 */
const extractAuthData = (payload: unknown) => {
  if (!payload || typeof payload !== 'object' || payload === null) {
    throw new Error('Invalid response structure')
  }

  const payloadObj = payload as Record<string, unknown>
  if (!payloadObj.data) {
    throw new Error('Invalid response structure')
  }

  const data = payloadObj.data as Record<string, unknown>

  if (data.id && data.username && data.accessToken && data.refreshToken) {
    return {
      user: mapUserData(data),
      accessToken: data.accessToken as string,
      refreshToken: data.refreshToken as string,
    }
  }

  if (data.user && data.access_token && data.refresh_token) {
    return {
      user: mapUserData(data.user),
      accessToken: data.access_token as string,
      refreshToken: data.refresh_token as string,
    }
  }

  throw new Error('Missing required auth data')
}

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
}

export const loginUser = createAsyncThunk(
  'auth/loginUser',
  async (credentials: LoginRequestData & { rememberMe?: boolean }, { rejectWithValue }) => {
    try {
      const response = await authApi.login(credentials)
      return { ...response.data, rememberMe: credentials.rememberMe || false } // Include remember me flag
    } catch (error: unknown) {
      const message = getErrorMessage(error, 'Login failed')
      return rejectWithValue(message)
    }
  },
)

export const registerUser = createAsyncThunk(
  'auth/registerUser',
  async (userData: RegisterData, { rejectWithValue }) => {
    try {
      const response = await authApi.registerAccount(userData)
      return response.data
    } catch (error: unknown) {
      const message = getErrorMessage(error, 'Registration failed')
      return rejectWithValue(message)
    }
  },
)

export const refreshTokenUser = createAsyncThunk(
  'auth/refreshTokenUser',
  async (refreshToken: string, { rejectWithValue }) => {
    try {
      console.log('refreshTokenUser called with token:', refreshToken.substring(0, 20) + '...')
      const response = await authApi.refreshToken(refreshToken)
      console.log('refreshTokenUser response:', response)
      return response.data
    } catch (error: unknown) {
      console.log('refreshTokenUser error:', error)
      const message = getErrorMessage(error, 'Token refresh failed')
      console.log('refreshTokenUser error message:', message)
      return rejectWithValue(message)
    }
  },
)

// Logout is now handled locally without API call since backend doesn't have logout
export const logoutUser = createAsyncThunk('auth/logoutUser', async () => {
  return { success: true }
})

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null
    },
    setCredentials: (
      state,
      action: PayloadAction<{ user: UserPublicInfo; accessToken: string; refreshToken: string }>,
    ) => {
      console.log('setCredentials called with:', {
        user: action.payload.user,
        accessToken: action.payload.accessToken.substring(0, 20) + '...',
        refreshToken: action.payload.refreshToken.substring(0, 20) + '...',
      })
      state.user = action.payload.user
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      state.isAuthenticated = true
      console.log('setCredentials completed - isAuthenticated:', state.isAuthenticated)
    },
    clearCredentials: (state) => {
      state.user = null
      state.accessToken = null
      state.refreshToken = null
      state.isAuthenticated = false

      // Clear localStorage
      clearLS()
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(loginUser.pending, (state) => {
        state.isLoading = true
        state.error = null
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.isLoading = false
        try {
          // Login response data is directly in the payload
          const payload = action.payload
          if (!payload?.data) {
            throw new Error('Invalid response structure')
          }

          const data = payload.data as Record<string, unknown>
          // Extract user info and tokens directly from data
          const user = mapUserData(data)
          const accessToken = data.accessToken as string
          const refreshToken = data.refreshToken as string

          state.user = user
          state.accessToken = accessToken
          state.refreshToken = refreshToken
          state.isAuthenticated = true
          state.error = null

          // Save tokens and user data based on remember me setting
          const rememberMe = payload.rememberMe || false
          setAccessTokenToStorage(accessToken, rememberMe)
          setRefreshTokenToStorage(refreshToken, rememberMe)
          setProfileToStorage(user, rememberMe)
          setRememberMe(rememberMe)
        } catch (error) {
          state.error = error instanceof Error ? error.message : 'Invalid login response'
          state.isAuthenticated = false
        }
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.payload ? String(action.payload) : 'Login failed'
        state.isAuthenticated = false
      })
      // Register
      .addCase(registerUser.pending, (state) => {
        state.isLoading = true
        state.error = null
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.isLoading = false
        try {
          // Registration response data is directly in the payload
          const payload = action.payload
          if (!payload?.data) {
            throw new Error('Invalid response structure')
          }

          const user = mapUserData(payload.data)
          state.user = user
          state.error = null
        } catch (error) {
          state.error = error instanceof Error ? error.message : 'Invalid registration response'
        }
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.payload ? String(action.payload) : 'Registration failed'
        state.isAuthenticated = false
      })
      // Refresh Token
      .addCase(refreshTokenUser.pending, (state) => {
        state.isLoading = true
        state.error = null
      })
      .addCase(refreshTokenUser.fulfilled, (state, action) => {
        state.isLoading = false
        try {
          const { user, accessToken, refreshToken } = extractAuthData(action.payload)
          state.user = user
          state.accessToken = accessToken
          state.refreshToken = refreshToken
          state.isAuthenticated = true
          state.error = null

          // Save tokens and user data to localStorage
          setAccessTokenToLS(accessToken)
          setRefreshTokenToLS(refreshToken)
          setProfileToLS(user)
        } catch (error) {
          state.error = error instanceof Error ? error.message : 'Invalid refresh token response'
          state.isAuthenticated = false
        }
      })
      .addCase(refreshTokenUser.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.payload ? String(action.payload) : 'Token refresh failed'
        state.isAuthenticated = false
      })
      // Logout
      .addCase(logoutUser.pending, (state) => {
        state.isLoading = true
        state.error = null
      })
      .addCase(logoutUser.fulfilled, (state) => {
        state.isLoading = false
        state.user = null
        state.accessToken = null
        state.refreshToken = null
        state.isAuthenticated = false
        state.error = null

        // Clear localStorage
        clearLS()
      })
      .addCase(logoutUser.rejected, (state, action) => {
        state.isLoading = false
        state.error = action.payload ? String(action.payload) : 'Logout failed'
      })
  },
})

export const { clearError, setCredentials, clearCredentials } = authSlice.actions
export default authSlice.reducer
