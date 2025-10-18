'use client'

import { Loader2, LogIn, User } from 'lucide-react'
import type React from 'react'
import { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { PasswordInput } from '@/components/ui/passwordInput'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useToast } from '@/hooks/use-toast'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { loginUser, clearError } from '@/store/authSlice'
import { getRememberMe } from '@/utils/auth'
import { type LoginRequestData } from '@/types/authTypes'

export function LoginForm() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(getRememberMe())
  const [hasShownSuccessToast, setHasShownSuccessToast] = useState(false)

  const dispatch = useAppDispatch()
  const { isLoading, error, isAuthenticated } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirectTo = searchParams.get('redirect') || '/workPlace'

  useEffect(() => {
    dispatch(clearError())
  }, [dispatch])

  useEffect(() => {
    if (isAuthenticated && !error && !isLoading && !hasShownSuccessToast) {
      setHasShownSuccessToast(true)
      toast({
        title: 'Success',
        description: 'Logged in successfully! Welcome back.',
      })
      setTimeout(() => {
        router.push(redirectTo)
      }, 1000)
    }
  }, [isAuthenticated, error, isLoading, hasShownSuccessToast, router, toast, redirectTo])

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

    const loginData: LoginRequestData & { rememberMe: boolean } = {
      username: username,
      password,
      rememberMe,
    }

    setHasShownSuccessToast(false)
    dispatch(loginUser(loginData))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="username" className="text-sm font-medium">
          Username
        </Label>
        <div className="relative group">
          <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
          <Input
            id="username"
            type="text"
            placeholder="Enter your username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="password" className="text-sm font-medium">
          Password
        </Label>
        <PasswordInput
          id="password"
          placeholder="Enter your password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          required
        />
      </div>

      <div className="flex items-center space-x-2">
        <input
          id="remember-me"
          type="checkbox"
          checked={rememberMe}
          onChange={(e) => setRememberMe(e.target.checked)}
          className="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"
        />
        <Label htmlFor="remember-me" className="text-sm font-medium">
          Remember me
        </Label>
      </div>

      <Button
        type="submit"
        className="w-full bg-gradient-to-r from-primary to-accent hover:from-primary/90 hover:to-accent/90 transition-all duration-300 shadow-lg hover:shadow-xl hover:shadow-primary/25"
        disabled={isLoading}
        size="lg"
      >
        {isLoading ? (
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
      </Button>

      <div className="text-center text-sm text-muted-foreground">
        <div className="flex items-center justify-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
          Secure authentication with enterprise-grade security
        </div>
      </div>
    </form>
  )
}
