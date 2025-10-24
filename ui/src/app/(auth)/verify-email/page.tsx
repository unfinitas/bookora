'use client'

import Link from 'next/link'
import { ArrowLeft, Mail, CheckCircle, RefreshCw, Info } from 'lucide-react'
import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useToast } from '@/hooks/useToast'
import { getVerificationEmail } from '@/utils/emailVerification'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'
import { CustomButton } from '@/components/ui/CustomButton'

export default function VerifyEmailPage() {
  const [userEmail, setUserEmail] = useState('')
  const [isLoading, setIsLoading] = useState(true)

  const dispatch = useAppDispatch()
  const { pending, successMessage, error } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()

  useEffect(() => {
    const email = getVerificationEmail()

    if (email) {
      setUserEmail(email)
      setIsLoading(false)
    } else {
      toast({
        title: 'No Verification Email Found',
        description: 'Please register first to verify your email.',
        variant: 'destructive',
      })
      router.push('/register')
    }
  }, [router, toast])

  useEffect(() => {
    if (successMessage) {
      toast({
        title: 'Email Sent!',
        description: successMessage,
      })
      dispatch(AuthAction.clearSuccessMessage())
    }
  }, [successMessage, toast, dispatch])

  useEffect(() => {
    if (error) {
      toast({
        title: 'Error',
        description: error,
        variant: 'destructive',
      })
      dispatch(AuthAction.clearError())
    }
  }, [error, toast, dispatch])

  const handleResendEmail = async () => {
    if (!userEmail) {
      toast({
        title: 'Error',
        description: 'No email address found',
        variant: 'destructive',
      })
      return
    }

    await dispatch(AuthThunk.RequestPasswordResetThunk({ email: userEmail }))
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex items-center justify-center p-4">
        <div className="text-center space-y-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex items-center justify-center p-4">
      <div className="w-full max-w-lg">
        <div className="text-center mb-6">
          <div className="flex items-center justify-center mb-4">
            <div className="relative">
              <div className="absolute inset-0 bg-primary/20 rounded-full blur-xl"></div>
              <Mail className="h-12 w-12 text-primary relative z-10" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-foreground mb-2 tracking-tight">Check Your Email</h1>
          <p className="text-muted-foreground text-base font-light">
            We&apos;ve sent a verification link to your email address.
          </p>
          {userEmail && (
            <div className="mt-3 p-2 rounded-lg bg-muted/50 border border-border">
              <p className="text-sm text-muted-foreground">
                Verification email sent to: <span className="font-medium text-foreground">{userEmail}</span>
              </p>
            </div>
          )}
        </div>

        <div className="bg-card/80 backdrop-blur-sm border border-border/50 shadow-2xl p-6 md:p-8 rounded-2xl">
          <div className="space-y-4">
            <div className="text-center space-y-3">
              <div className="flex items-center justify-center">
                <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Mail className="h-5 w-5 text-primary" />
                </div>
              </div>

              <div className="space-y-1">
                <h2 className="text-lg font-semibold text-foreground">Verification Email Sent</h2>
                <p className="text-muted-foreground text-sm">Please check your email inbox and spam folder.</p>
                {userEmail && <p className="text-sm font-medium text-primary">Check: {userEmail}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2 p-2 rounded-lg bg-muted/50">
                <CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" />
                <span className="text-sm text-foreground">Click the verification link in your email</span>
              </div>

              <div className="flex items-center gap-2 p-2 rounded-lg bg-muted/50">
                <CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" />
                <span className="text-sm text-foreground">Your account will be verified automatically</span>
              </div>

              <div className="flex items-center gap-2 p-2 rounded-lg bg-muted/50">
                <CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" />
                <span className="text-sm text-foreground">You can then log in to access the dashboard</span>
              </div>
            </div>

            <div className="pt-3 border-t border-border">
              <p className="text-center text-sm text-muted-foreground mb-3">
                Didn&apos;t receive the email? Check your spam folder or resend.
              </p>

              <div className="space-y-2">
                <CustomButton
                  onClick={handleResendEmail}
                  disabled={pending}
                  className="w-full bg-primary hover:bg-primary/90 transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98]"
                  size="lg"
                >
                  {pending ? (
                    <div className="flex items-center gap-2">
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      Sending...
                    </div>
                  ) : (
                    <div className="flex items-center gap-2">
                      <RefreshCw className="h-4 w-4" />
                      Resend Verification Email
                    </div>
                  )}
                </CustomButton>

                <div className="flex items-center gap-2 p-2 rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800">
                  <Info className="h-4 w-4 text-blue-600 dark:text-blue-400 flex-shrink-0" />
                  <p className="text-sm text-blue-700 dark:text-blue-300">
                    You can resend verification emails every hour to prevent spam.
                  </p>
                </div>

                <div className="flex flex-col sm:flex-row gap-2">
                  <Link
                    href="/login"
                    className="flex-1 bg-secondary hover:bg-secondary/90 text-secondary-foreground px-3 py-2 rounded-md text-sm font-medium transition-colors text-center"
                  >
                    Back to Login
                  </Link>
                  <Link
                    href="/register"
                    className="flex-1 border border-border hover:bg-muted px-3 py-2 rounded-md text-sm font-medium transition-colors text-center"
                  >
                    Back to Register
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 space-y-4">
          <div className="flex items-center justify-center">
            <Link
              href="/"
              className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-all duration-300 group"
            >
              <ArrowLeft className="h-4 w-4 group-hover:-translate-x-1 transition-transform" />
              Back to Home
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
