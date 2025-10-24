import Link from 'next/link'
import { ArrowLeft, Lock } from 'lucide-react'
import { ResetPasswordForm } from '@/components/forms/ResetPassword'

export default function ResetPasswordPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex items-center justify-center p-4">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-6">
            <div className="relative">
              <div className="absolute inset-0 bg-primary/20 rounded-full blur-xl"></div>
              <Lock className="h-16 w-16 text-primary relative z-10" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-foreground mb-3 tracking-tight">Reset Password</h1>
          <p className="text-muted-foreground text-lg font-light">Enter your new password below</p>
        </div>
        <div className="bg-card/80 backdrop-blur-sm border border-border/50 shadow-2xl p-8 md:p-10 rounded-2xl">
          <ResetPasswordForm />
        </div>
        <div className="mt-8 space-y-6">
          <div className="flex items-center justify-center">
            <Link
              href="/login"
              className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-all duration-300 group"
            >
              <ArrowLeft className="h-4 w-4 group-hover:-translate-x-1 transition-transform" />
              Back to Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
