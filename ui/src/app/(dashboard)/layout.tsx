'use client'

import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useSelector, useDispatch } from 'react-redux'
import { RootState, AppDispatch } from '@/store'
import { refreshTokenUser, clearCredentials, setCredentials } from '@/store/authSlice'
import { getAccessTokenFromStorage, getRefreshTokenFromStorage } from '@/utils/auth'

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const dispatch = useDispatch<AppDispatch>()

  const { isAuthenticated, accessToken, refreshToken, isLoading } = useSelector((state: RootState) => state.auth)

  useEffect(() => {
    const checkAuthentication = async () => {
      // Get tokens from storage
      const localAccessToken = getAccessTokenFromStorage()
      const localRefreshToken = getRefreshTokenFromStorage()

      console.log('Dashboard layout check:', {
        isAuthenticated,
        accessToken: !!accessToken,
        refreshToken: !!refreshToken,
        isLoading,
        localAccessToken: !!localAccessToken,
        localRefreshToken: !!localRefreshToken,
      })

      if (isAuthenticated && accessToken) {
        console.log('Already authenticated, returning')
        return
      }

      if (localAccessToken && localRefreshToken && !isAuthenticated && !isLoading) {
        console.log('Attempting token refresh...')
        console.log('Refresh token:', localRefreshToken.substring(0, 20) + '...')
        try {
          const result = await dispatch(refreshTokenUser(localRefreshToken)).unwrap()
          console.log('Token refresh successful:', result)
          return
        } catch (error) {
          console.log('Token refresh failed with error:', error)
          console.log('Error type:', typeof error)
          console.log('Error details:', JSON.stringify(error, null, 2))

          console.log('Attempting to restore from localStorage...')
          try {
            const profileStr = localStorage.getItem('profile')
            console.log('Raw profile string from localStorage:', profileStr)
            const profile = JSON.parse(profileStr || '{}')
            console.log('Parsed profile from localStorage:', profile)
            console.log('Profile has id:', !!profile.id)
            console.log('Profile has username:', !!profile.username)
            console.log('Profile id value:', profile.id)
            console.log('Profile username value:', profile.username)

            if (profile.id && profile.username) {
              console.log('Profile validation passed, restoring authentication')
              const credentials = {
                user: profile,
                accessToken: localAccessToken,
                refreshToken: localRefreshToken,
              }
              console.log('Credentials to restore:', {
                user: credentials.user,
                accessToken: credentials.accessToken.substring(0, 20) + '...',
                refreshToken: credentials.refreshToken.substring(0, 20) + '...',
              })

              dispatch(setCredentials(credentials))
              console.log('setCredentials dispatched successfully')
              console.log('About to return from checkAuthentication')
              return
            } else {
              console.log('Profile missing required fields - id:', profile.id, 'username:', profile.username)
            }
          } catch (restoreError) {
            console.log('Failed to restore from localStorage:', restoreError)
            console.log('Restore error details:', JSON.stringify(restoreError, null, 2))
          }
          console.log('Clearing credentials and redirecting to login')
          dispatch(clearCredentials())
          router.push(`/login?redirect=${encodeURIComponent(pathname)}`)
          return
        }
      }

      if (!localAccessToken && !localRefreshToken && !isLoading) {
        console.log('No tokens found, redirecting to login')
        router.push(`/login?redirect=${encodeURIComponent(pathname)}`)
        return
      }
    }

    checkAuthentication()
  }, [isAuthenticated, accessToken, refreshToken, isLoading, dispatch, router, pathname])

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center space-y-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">Checking authentication...</p>
        </div>
      </div>
    )
  }
  if (!isAuthenticated) {
    return null
  }

  return <>{children}</>
}
