'use client'
import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const dispatch = useAppDispatch()
  const { isAuthenticated, token, refreshToken, pending } = useAppSelector((state) => state.auth)

  useEffect(() => {
    const checkAuthentication = async () => {
      try {
        console.log('Dashboard layout check:', {
          isAuthenticated,
          hasToken: !!token,
          hasRefreshToken: !!refreshToken,
          pending,
        })

        // Already authenticated with valid tokens
        if (isAuthenticated && token && refreshToken) {
          console.log('Already authenticated')
          return
        }

        // Not authenticated and not loading - redirect to login
        if (!isAuthenticated && !pending) {
          console.log('Not authenticated, redirecting to login')
          router.push(`/login?redirect=${encodeURIComponent(pathname)}`)
          return
        }

        // If we have refresh token but no access token, try to refresh
        if (refreshToken && !token && !pending) {
          console.log('Attempting token refresh...')
          await dispatch(AuthThunk.RefreshTokenThunk()).unwrap()
          console.log('Token refresh successful')
        }
      } catch (error) {
        console.error('Authentication check failed:', error)
        dispatch(AuthAction.logout())
        router.push(`/login?redirect=${encodeURIComponent(pathname)}`)
      }
    }

    checkAuthentication().catch(console.error);
  }, [isAuthenticated, token, refreshToken, pending, dispatch, router, pathname])

  if (pending) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center space-y-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">Checking authentication...</p>
        </div>
      </div>
    )
  }

  // Don't render anything if not authenticated
  if (!isAuthenticated) {
    return null
  }

  return <>{children}</>
}
