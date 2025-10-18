'use client'

import { Loader2, Mail, User, UserPlus, Phone } from 'lucide-react'
import type React from 'react'
import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { PasswordStrengthInput } from '@/components/ui/passwordStrenghtInput'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { useToast } from '@/hooks/use-toast'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { registerUser, clearError, clearCredentials } from '@/store/authSlice'
import { type RegisterRequest } from '@/schemas/authSchema'

export function RegisterForm() {
  const [formData, setFormData] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    phoneNumber: '',
  })
  const [hasShownSuccessToast, setHasShownSuccessToast] = useState(false)

  const dispatch = useAppDispatch()
  const { isLoading, error, user } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()

  useEffect(() => {
    dispatch(clearError())
  }, [dispatch])

  useEffect(() => {
    if (user && !error && !isLoading && !hasShownSuccessToast) {
      setHasShownSuccessToast(true)
      toast({
        title: 'Success',
        description: 'Account created successfully! Please log in to continue.',
      })
      dispatch(clearCredentials())
      setTimeout(() => {
        router.push('/login')
      }, 1000)
    }
  }, [user, error, isLoading, hasShownSuccessToast, router, toast, dispatch])

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

    if (formData.password !== formData.confirmPassword) {
      toast({
        title: 'Error',
        description: 'Passwords do not match',
        variant: 'destructive',
      })
      return
    }

    const registerData: RegisterRequest = {
      username: formData.username,
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      password: formData.password,
      phoneNumber: formData.phoneNumber || undefined,
    }

    setHasShownSuccessToast(false)
    dispatch(registerUser(registerData))
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="firstName" className="text-sm font-medium">
            First Name
          </Label>
          <div className="relative group">
            <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
            <Input
              id="firstName"
              type="text"
              placeholder="Enter your first name"
              value={formData.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
              className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
              required
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="lastName" className="text-sm font-medium">
            Last Name
          </Label>
          <div className="relative group">
            <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
            <Input
              id="lastName"
              type="text"
              placeholder="Enter your last name"
              value={formData.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
              className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
              required
            />
          </div>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="username" className="text-sm font-medium">
          Username
        </Label>
        <div className="relative group">
          <User className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
          <Input
            id="username"
            type="text"
            placeholder="Choose a username"
            value={formData.username}
            onChange={(e) => handleInputChange('username', e.target.value)}
            className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="register-email" className="text-sm font-medium">
          Email
        </Label>
        <div className="relative group">
          <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
          <Input
            id="register-email"
            type="email"
            placeholder="Enter your email"
            value={formData.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="phoneNumber" className="text-sm font-medium">
          Phone Number (Optional)
        </Label>
        <div className="relative group">
          <Phone className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
          <Input
            id="phoneNumber"
            type="tel"
            placeholder="Enter your phone number"
            value={formData.phoneNumber}
            onChange={(e) => handleInputChange('phoneNumber', e.target.value)}
            className="pl-10 transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="register-password" className="text-sm font-medium">
          Password
        </Label>
        <PasswordStrengthInput
          id="register-password"
          placeholder="Create a password"
          value={formData.password}
          onChange={(e) => handleInputChange('password', e.target.value)}
          className="transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          showStrengthIndicator={true}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="confirm-password" className="text-sm font-medium">
          Confirm Password
        </Label>
        <PasswordStrengthInput
          id="confirm-password"
          placeholder="Confirm your password"
          value={formData.confirmPassword}
          onChange={(e) => handleInputChange('confirmPassword', e.target.value)}
          className="transition-all duration-300 focus:shadow-lg focus:shadow-primary/20"
          required
        />
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
            Creating account...
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <UserPlus className="h-4 w-4" />
            Create Account
          </div>
        )}
      </Button>

      <Separator className="my-6" />

      <div className="text-center text-sm text-muted-foreground">
        <div className="flex items-center justify-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
          By creating an account, you agree to our terms of service
        </div>
      </div>
    </form>
  )
}
