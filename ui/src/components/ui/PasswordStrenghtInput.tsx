import { Check, Eye, EyeOff, Lock } from 'lucide-react'
import type React from 'react'
import { forwardRef, useEffect, useState } from 'react'
import { CustomButton } from '@/components/ui/CustomButton'
import { Input } from '@/components/ui/Input'
import { cn } from '@/utils/lib'

interface PasswordStrengthInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  className?: string
  showStrengthIndicator?: boolean
}

interface PasswordRequirement {
  label: string
  test: (password: string) => boolean
}

const passwordRequirements: PasswordRequirement[] = [
  { label: 'At least 8 characters', test: (pwd) => pwd.length >= 8 },
  { label: 'At least 1 number', test: (pwd) => /\d/.test(pwd) },
  { label: 'At least 1 lowercase letter', test: (pwd) => /[a-z]/.test(pwd) },
  { label: 'At least 1 uppercase letter', test: (pwd) => /[A-Z]/.test(pwd) },
  { label: 'At least 1 special character', test: (pwd) => /[!@#$%^&*]/.test(pwd) },
]

const PasswordStrengthInput = forwardRef<HTMLInputElement, PasswordStrengthInputProps>(
  ({ className, showStrengthIndicator = false, value, onChange, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false)
    const [password, setPassword] = useState(value?.toString() || '')
    const [strength, setStrength] = useState(0)

    useEffect(() => {
      if (typeof value === 'string') {
        setPassword(value)
      }
    }, [value])

    useEffect(() => {
      const passedRequirements = passwordRequirements.filter((req) => req.test(password)).length
      setStrength(passedRequirements)
    }, [password])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value
      setPassword(newValue)
      onChange?.(e)
    }

    const getStrengthColor = () => {
      if (strength === 0) return 'bg-border'
      if (strength <= 2) return 'bg-destructive'
      if (strength === 3) return 'bg-yellow-500'
      return 'bg-green-500'
    }

    const getStrengthWidth = () => {
      return `${(strength / passwordRequirements.length) * 100}%`
    }

    const isStrong = strength === passwordRequirements.length

    return (
      <div className="space-y-3">
        <div className="relative group">
          <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
          <Input
            type={showPassword ? 'text' : 'password'}
            className={cn('pl-10 pr-12 bg-card border-border', className)}
            ref={ref}
            value={password}
            onChange={handleChange}
            {...props}
          />
          <CustomButton
            type="button"
            variant="ghost"
            size="sm"
            className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent focus:ring-0 focus:ring-offset-0"
            onClick={() => setShowPassword(!showPassword)}
            tabIndex={-1}
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4 text-muted-foreground transition-colors hover:text-foreground" />
            ) : (
              <Eye className="h-4 w-4 text-muted-foreground transition-colors hover:text-foreground" />
            )}
            <span className="sr-only">{showPassword ? 'Hide password' : 'Show password'}</span>
          </CustomButton>
        </div>

        {showStrengthIndicator && password && (
          <div className="space-y-3">
            {/* Strength Bar */}
            <div className="w-full bg-border rounded-full h-2 overflow-hidden">
              <div
                className={cn('h-full transition-all duration-300 rounded-full', getStrengthColor())}
                style={{ width: getStrengthWidth() }}
              />
            </div>

            {/* Requirements */}
            <div className="space-y-2">
              <p className={cn('text-sm font-medium', isStrong ? 'text-green-400' : 'text-foreground')}>
                {isStrong ? 'Strong password. Must contain:' : 'Strong password. Must contain:'}
              </p>
              <div className="space-y-1">
                {passwordRequirements.map((requirement, index) => {
                  const isPassed = requirement.test(password)
                  return (
                    <div key={index} className="flex items-center gap-2">
                      <Check
                        className={cn(
                          'h-4 w-4 transition-colors',
                          isPassed ? 'text-green-400' : 'text-muted-foreground',
                        )}
                      />
                      <span
                        className={cn(
                          'text-sm transition-colors',
                          isPassed ? 'text-green-400' : 'text-muted-foreground',
                        )}
                      >
                        {requirement.label}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        )}
      </div>
    )
  },
)

PasswordStrengthInput.displayName = 'PasswordStrengthInput'

export { PasswordStrengthInput }
