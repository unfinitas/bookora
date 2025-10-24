'use client'

import { Loader2, Lock } from 'lucide-react'
import React, { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useToast } from '@/hooks/useToast'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'
import { Label } from '@/components/ui/Label'
import { PasswordStrengthInput } from '@/components/ui/PasswordStrenghtInput'
import { CustomButton } from '@/components/ui/CustomButton'

export function ResetPasswordForm() {
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [hasShownSuccessToast, setHasShownSuccessToast] = useState(false)

  const searchParams = useSearchParams()
  const token = searchParams.get('token') || ''

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
      }, 2000)
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

    if (newPassword !== confirmPassword) {
      toast({
        title: 'Error',
        description: 'Passwords do not match',
        variant: 'destructive',
      })
      return
    }

    if (newPassword.length < 8) {
      toast({
        title: 'Error',
        description: 'Password must be at least 8 characters long',
        variant: 'destructive',
      })
      return
    }

    if (!token) {
      toast({
        title: 'Error',
        description: 'Invalid reset token',
        variant: 'destructive',
      })
      return
    }

    setHasShownSuccessToast(false)
    dispatch(AuthAction.clearError())
    dispatch(AuthAction.clearSuccessMessage())

    await dispatch(AuthThunk.ResetPasswordThunk({
      token,
      newPassword,
    }))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="new-password" className="text-sm font-medium">
          New Password
        </Label>
        <PasswordStrengthInput
          id="new-password"
          placeholder="Enter your new password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          disabled={pending}
          className="transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          showStrengthIndicator={true}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="confirm-new-password" className="text-sm font-medium">
          Confirm New Password
        </Label>
        <PasswordStrengthInput
          id="confirm-new-password"
          placeholder="Confirm your new password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          disabled={pending}
          className="transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          required
        />
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
            Resetting password...
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Lock className="h-4 w-4" />
            Reset Password
          </div>
        )}
      </CustomButton>

      <div className="text-center text-sm text-muted-foreground">
        <div className="flex items-center justify-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full" />
          Secure password reset with enterprise-grade encryption
        </div>
      </div>
    </form>
  )
}
