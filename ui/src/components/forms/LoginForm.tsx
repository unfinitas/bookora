'use client'

import { Loader2, LogIn, Mail } from 'lucide-react'
import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { PasswordInput } from '@/components/ui/PasswordInput'
import { CustomButton } from '@/components/ui/CustomButton'
import { Input } from '@/components/ui/Input'
import { Label } from '@/components/ui/Label'
import { Separator } from '@/components/ui/Separator'
import { useToast } from '@/hooks/useToast'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import AuthThunk from '@/libs/redux/auth/authThunk'
import { AuthAction } from '@/libs/redux/auth/authReducer'
import { LoginFormData, loginSchema } from '@/schemas/authSchema'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { FcGoogle } from 'react-icons/fc'

export function LoginForm() {
  const dispatch = useAppDispatch()
  const { error, pending, isAuthenticated } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()
  const [hasShownSuccessToast, setHasShownSuccessToast] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur', // shows validation after user leaves field
  })

  const onSubmit = async (data: LoginFormData) => {
    setHasShownSuccessToast(false)
    dispatch(AuthAction.clearError())
    await dispatch(AuthThunk.LoginByUsernameThunk(data))
  }

  useEffect(() => {
    if (isAuthenticated && !error && !pending && !hasShownSuccessToast) {
      setHasShownSuccessToast(true)
      toast({
        title: 'Success',
        description: 'Logged in successfully! Welcome back.',
      })
      setTimeout(() => router.push('/workplace'), 1000)
    }
  }, [isAuthenticated, error, pending, hasShownSuccessToast, toast, router])

  useEffect(() => {
    if (error) {
      toast({
        title: 'Error',
        description: error,
        variant: 'destructive',
      })
    }
  }, [error, toast])

  const handleGoogleLogin = () => {
    toast({
      title: 'Coming Soon',
      description: 'Google login will be available soon.',
    })
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Username */}
      <div className="space-y-2">
        <Label htmlFor="username">Username</Label>
        <div className="relative group">
          <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
          <Input
            id="username"
            placeholder="your_username"
            disabled={pending}
            {...register('username')}
            className={`pl-10 h-11 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20 ${
              errors.username ? 'border-red-500' : ''
            }`}
          />
        </div>
        {errors.username && <p className="text-sm text-red-500">{errors.username.message}</p>}
      </div>

      {/* Password */}
      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <PasswordInput
          id="password"
          placeholder="••••••••"
          disabled={pending}
          {...register('password')}
          className={`h-11 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20 ${
            errors.password ? 'border-red-500' : ''
          }`}
        />
        {errors.password && <p className="text-sm text-red-500">{errors.password.message}</p>}
      </div>

      {/* Forgot password */}
      <div className="flex items-center justify-end">
        <Link
          href="/forgot-password"
          className="text-sm text-primary hover:text-primary/80 font-medium transition-colors"
        >
          Forgot password?
        </Link>
      </div>

      {/* Submit */}
      <CustomButton
        type="submit"
        className="w-full bg-primary hover:bg-primary/90 transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
        disabled={!isValid || pending}
        size="lg"
      >
        {pending ? (
          <div className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            Signing in...
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <LogIn className="h-4 w-4" />
            Sign In
          </div>
        )}
      </CustomButton>

      {/* Divider */}
      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <Separator className="w-full" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-background px-2 text-muted-foreground">Or continue with</span>
        </div>
      </div>

      {/* Google login */}
      <CustomButton
        type="button"
        variant="outline"
        className="w-full"
        onClick={handleGoogleLogin}
        size="lg"
        disabled={pending}
      >
        <FcGoogle className="mr-2 h-5 w-5" />
        Continue with Google
      </CustomButton>
    </form>
  )
}
