'use client'

import { Loader2, Mail, User, UserPlus, IdCard } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { PasswordStrengthInput } from '@/components/ui/PasswordStrenghtInput'
import { CustomButton } from '@/components/ui/CustomButton'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { Separator } from '@/components/ui/Separator'
import { useToast } from '@/hooks/useToast'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'
import { storeVerificationEmail } from '@/utils/emailVerification'
import React, { useEffect } from 'react'
import { RegisterFormData, registerSchema } from '@/schemas/authSchema'

export function RegisterForm() {
  const dispatch = useAppDispatch()
  const { pending, error, userId, successMessage } = useAppSelector((s) => s.auth)
  const { toast } = useToast()
  const router = useRouter()

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setError,
    clearErrors,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
  })

  const password = watch('password')
  const confirmPassword = watch('confirmPassword')

  const onSubmit = async (data: RegisterFormData) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { confirmPassword, ...payload } = data
    await dispatch(AuthThunk.RegisterThunk(payload))
  }

  useEffect(() => {
    dispatch(AuthAction.clearError())
  }, [dispatch])

  // Real-time password confirmation validation
  useEffect(() => {
    if (password !== confirmPassword) {
      setError('confirmPassword', {
        type: 'manual',
        message: 'Passwords do not match',
      })
    } else {
      clearErrors('confirmPassword')
    }
  }, [password, confirmPassword, setError, clearErrors])

  const passwordsMatch = password && confirmPassword && password === confirmPassword

  useEffect(() => {
    if (userId && successMessage) {
      toast({ title: 'Success', description: successMessage })
      storeVerificationEmail(watch('email'))
      setTimeout(() => router.push('/verify-email'), 1000)
    }
  }, [userId, successMessage, router, toast, watch])

  useEffect(() => {
    if (error) {
      toast({ title: 'Error', description: error, variant: 'destructive' })
    }
  }, [error, toast])

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1">
          <Label htmlFor="firstName">First Name</Label>
          <div className="relative">
            <IdCard className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input id="firstName" {...register('firstName')} className="pl-10" disabled={pending} />
          </div>
          {errors.firstName && <p className="text-sm text-red-500">{errors.firstName.message}</p>}
        </div>

        <div className="space-y-1">
          <Label htmlFor="lastName">Last Name</Label>
          <div className="relative">
            <IdCard className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input id="lastName" {...register('lastName')} className="pl-10" disabled={pending} />
          </div>
          {errors.lastName && <p className="text-sm text-red-500">{errors.lastName.message}</p>}
        </div>
      </div>

      <div className="space-y-1">
        <Label htmlFor="username">Username</Label>
        <div className="relative">
          <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input id="username" {...register('username')} className="pl-10" disabled={pending} />
        </div>
        {errors.username && <p className="text-sm text-red-500">{errors.username.message}</p>}
      </div>

      <div className="space-y-1">
        <Label htmlFor="email">Email</Label>
        <div className="relative">
          <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input id="email" type="email" {...register('email')} className="pl-10" disabled={pending} />
        </div>
        {errors.email && <p className="text-sm text-red-500">{errors.email.message}</p>}
      </div>

      <div className="space-y-1">
        <Label htmlFor="password">Password</Label>
        <PasswordStrengthInput id="password" {...register('password')} showStrengthIndicator disabled={pending} />
        {errors.password && <p className="text-sm text-red-500">{errors.password.message}</p>}
      </div>

      <div className="space-y-1">
        <Label htmlFor="confirmPassword">Confirm Password</Label>
        <PasswordStrengthInput
          id="confirmPassword"
          {...register('confirmPassword')}
          disabled={pending}
          className={`transition-all duration-300 ${
            errors.confirmPassword
              ? 'border-red-500 focus:border-red-500'
              : password && confirmPassword && password === confirmPassword
              ? 'border-green-500 focus:border-green-500'
              : ''
          }`}
        />
        {errors.confirmPassword && <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>}
        {password && confirmPassword && password === confirmPassword && !errors.confirmPassword && (
          <p className="text-sm text-green-500">✓ Passwords match</p>
        )}
      </div>

      <CustomButton
        type="submit"
        className="w-full bg-primary hover:bg-primary/90 transition-all duration-300 shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
        disabled={pending || !passwordsMatch}
        size="lg"
      >
        {pending ? (
          <div className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            Creating account...
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <UserPlus className="h-4 w-4" />
            Create Account
          </div>
        )}
      </CustomButton>

      <Separator className="my-4" />

      <div className="text-center text-sm text-muted-foreground">
        <div className="flex items-center justify-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full" />
          By creating an account, you agree to our terms of service
        </div>
      </div>
    </form>
  )
}
