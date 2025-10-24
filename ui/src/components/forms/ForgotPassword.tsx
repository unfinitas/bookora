'use client'

import { Loader2, Mail } from 'lucide-react'
import React, { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { CustomButton } from '@/components/ui/CustomButton'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { useToast } from '@/hooks/useToast'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'

export function ForgotPasswordForm() {
  const [email, setEmail] = useState('')
  const [hasShownSuccessToast, setHasShownSuccessToast] = useState(false)

  const dispatch = useAppDispatch()
  const { pending, error, successMessage } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()

  // Clear error on mount
  useEffect(() => {
    dispatch(AuthAction.clearError())
    dispatch(AuthAction.clearSuccessMessage())
  }, [dispatch])

  // Handle success
  useEffect(() => {
    if (successMessage && !error && !pending && !hasShownSuccessToast) {
      setHasShownSuccessToast(true)
      toast({
        title: 'Success',
        description: successMessage,
      })
      setTimeout(() => {
        router.push('/login')
      }, 3000)
    }
  }, [successMessage, error, pending, hasShownSuccessToast, router, toast])

  // Show error toast
  useEffect(() => {
    if (error) {
      toast({
        title: 'Error',
        description: error,
        variant: 'destructive',
      })
    }
  }, [error, toast])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setHasShownSuccessToast(false)
    dispatch(AuthAction.clearError())
    dispatch(AuthAction.clearSuccessMessage())

    await dispatch(AuthThunk.RequestPasswordResetThunk({ email }))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="email" className="text-sm font-medium">
          Email Address
        </Label>
        <div className="relative group">
          <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
          <Input
            id="email"
            type="email"
            placeholder="your@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={pending}
            className="pl-10 h-11 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20 focus:scale-[1.02]"
            required
          />
        </div>
      </div>

      <CustomButton
        type="submit"
        className="w-full bg-primary hover:bg-primary/90 transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98]"
        disabled={pending}
        size="lg"
      >
        {pending ? (
          <div className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            Sending reset link...
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Mail className="h-4 w-4" />
            Send Reset Link
          </div>
        )}
      </CustomButton>

      <div className="text-center text-sm text-muted-foreground">
        <div className="flex items-center justify-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full" />
          We'll send you a secure password reset link
        </div>
      </div>
    </form>
  )
}
