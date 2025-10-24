import { Eye, EyeOff, Lock } from 'lucide-react'
import type React from 'react'
import { forwardRef, useState } from 'react'
import { CustomButton } from '@/components/ui/CustomButton'
import { Input } from '@/components/ui/Input'
import { cn } from '@/utils/lib'

interface PasswordInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  className?: string
}

const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(({ className, ...props }, ref) => {
  const [showPassword, setShowPassword] = useState(false)

  return (
    <div className="relative group">
      <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors z-10" />
      <Input type={showPassword ? 'text' : 'password'} className={cn('pl-10 pr-12', className)} ref={ref} {...props} />
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
  )
})
PasswordInput.displayName = 'PasswordInput'

export { PasswordInput }
