import { RegisterForm } from '@/features/auth/forms/registerForms'
import Link from 'next/link'
import { ArrowLeft, Calendar, Shield } from 'lucide-react'
import { ThemeToggle } from '@/components/ui/themeToggle'

export default function RegisterPage() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute inset-0 -z-10">
        <div className="absolute top-20 left-10 w-72 h-72 bg-primary/5 rounded-full animate-pulse" />
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-accent/5 rounded-full animate-pulse delay-1000" />
      </div>

      <div className="absolute top-6 right-6 z-50">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-2xl animate-slide-up">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-6">
            <div className="relative">
              <Calendar className="h-16 w-16 text-primary animate-bounce-slow" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-foreground mb-3 tracking-tight">Join Bookora</h1>
          <p className="text-muted-foreground text-lg font-light">Create your account to start managing your books</p>
        </div>

        <div className="bg-card border border-border shadow-2xl p-8 md:p-10 animate-scale-in">
          <RegisterForm />
        </div>

        <div className="mt-8 space-y-6">
          <div className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link
              href="/login"
              className="text-primary hover:text-primary/80 font-medium transition-all duration-300 hover:underline underline-offset-4"
            >
              Sign in here
            </Link>
          </div>

          <div className="flex items-center justify-center">
            <Link
              href="/"
              className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-all duration-300 group"
            >
              <ArrowLeft className="h-4 w-4 group-hover:-translate-x-1 transition-transform" />
              Back to Home
            </Link>
          </div>

          <div className="flex items-center justify-center">
            <div className="inline-flex items-center gap-3 text-xs text-muted-foreground bg-muted/50 px-4 py-3 border border-border/50 backdrop-blur-sm">
              <Shield className="h-4 w-4 text-green-500" />
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                <span>Secure registration with enterprise-grade encryption</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
