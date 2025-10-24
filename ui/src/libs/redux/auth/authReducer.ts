import { createSlice } from '@reduxjs/toolkit'
import AuthThunk from '@/libs/redux/auth/authThunk'

export interface AuthState {
  isAuthenticated: boolean
  userId?: string
  username?: string
  email?: string
  role?: string
  token?: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
  pending: boolean
  error?: string
  successMessage?: string
}

const initialState: AuthState = {
  isAuthenticated: false,
  pending: false,
}

// Helpers
const setPending = (state: AuthState) => {
  state.pending = true
  state.error = undefined
}

const setError = (state: AuthState, message: string) => {
  state.pending = false
  state.error = message
}

const setSuccess = (state: AuthState, message: string) => {
  state.pending = false
  state.successMessage = message
}

const AuthSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: () => initialState,
    clearError: (state) => {
      state.error = undefined
    },
    clearSuccessMessage: (state) => {
      state.successMessage = undefined
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(AuthThunk.LoginByUsernameThunk.pending, setPending)
      .addCase(AuthThunk.LoginByUsernameThunk.fulfilled, (state, action) => {
        Object.assign(state, {
          isAuthenticated: true,
          userId: action.payload.id,
          username: action.payload.username,
          role: action.payload.role,
          token: action.payload.accessToken,
          refreshToken: action.payload.refreshToken,
          tokenType: action.payload.tokenType,
          expiresIn: action.payload.expiresIn,
          pending: false,
          error: undefined,
        })
      })
      .addCase(AuthThunk.LoginByUsernameThunk.rejected, (state, action) => {
        state.isAuthenticated = false
        setError(state, action.error.message || 'Login failed')
      })

      // Register
      .addCase(AuthThunk.RegisterThunk.pending, setPending)
      .addCase(AuthThunk.RegisterThunk.fulfilled, (state, action) => {
        state.userId = action.payload.id
        state.username = action.payload.username
        setSuccess(state, 'Registration successful! Please check your email.')
      })
      .addCase(AuthThunk.RegisterThunk.rejected, (state, action) => {
        setError(state, action.error.message || 'Registration failed')
      })

      // Logout
      .addCase(AuthThunk.LogoutThunk.pending, setPending)
      .addCase(AuthThunk.LogoutThunk.fulfilled, () => initialState)
      .addCase(AuthThunk.LogoutThunk.rejected, () => initialState)

      // Refresh Token
      .addCase(AuthThunk.RefreshTokenThunk.pending, setPending)
      .addCase(AuthThunk.RefreshTokenThunk.fulfilled, (state, action) => {
        state.token = action.payload.accessToken
        state.refreshToken = action.payload.refreshToken
        state.tokenType = action.payload.tokenType
        state.expiresIn = action.payload.expiresIn
        state.pending = false
      })
      .addCase(AuthThunk.RefreshTokenThunk.rejected, (state, action) => {
        state.isAuthenticated = false
        setError(state, action.error.message || 'Session expired')
      })

      // Verify Email
      .addCase(AuthThunk.VerifyEmailThunk.pending, setPending)
      .addCase(AuthThunk.VerifyEmailThunk.fulfilled, (state, action) => {
        setSuccess(state, action.payload.message)
      })
      .addCase(AuthThunk.VerifyEmailThunk.rejected, (state, action) => {
        setError(state, action.error.message || 'Email verification failed')
      })

      // Request Password Reset
      .addCase(AuthThunk.RequestPasswordResetThunk.pending, setPending)
      .addCase(AuthThunk.RequestPasswordResetThunk.fulfilled, (state, action) => {
        setSuccess(state, action.payload.message)
      })
      .addCase(AuthThunk.RequestPasswordResetThunk.rejected, (state, action) => {
        setError(state, action.error.message || 'Failed to request password reset')
      })

      // Reset Password
      .addCase(AuthThunk.ResetPasswordThunk.pending, setPending)
      .addCase(AuthThunk.ResetPasswordThunk.fulfilled, (state, action) => {
        setSuccess(state, action.payload.message)
      })
      .addCase(AuthThunk.ResetPasswordThunk.rejected, (state, action) => {
        setError(state, action.error.message || 'Failed to reset password')
      })

      // Change Password
      .addCase(AuthThunk.ChangePasswordThunk.pending, setPending)
      .addCase(AuthThunk.ChangePasswordThunk.fulfilled, (state, action) => {
        setSuccess(state, action.payload.message)
      })
      .addCase(AuthThunk.ChangePasswordThunk.rejected, (state, action) => {
        setError(state, action.error.message || 'Failed to change password')
      })
  },
})

export const AuthAction = AuthSlice.actions
export default AuthSlice.reducer
