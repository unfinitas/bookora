import { LoginForm } from '@/components/forms/LoginForm'
import Link from 'next/link'
import { ArrowLeft, Calendar } from 'lucide-react'

export default function LoginPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        <div className="text-center mb-6">
          <div className="flex items-center justify-center mb-4">
            <div className="relative">
              <div className="absolute inset-0 bg-primary/20 rounded-full blur-xl"></div>
              <Calendar className="h-12 w-12 text-primary relative z-10" />
            </div>
          </div>
          <h1 className="text-3xl font-bold text-foreground mb-2 tracking-tight">Welcome back</h1>
          <p className="text-muted-foreground text-base font-light">Sign in to continue to Bookora</p>
        </div>

        <div className="bg-card/80 backdrop-blur-sm border border-border/50 shadow-2xl p-6 md:p-8 rounded-2xl">
          <LoginForm />
        </div>

        <div className="mt-8 space-y-6">
          <div className="text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{' '}
            <Link
              href="/register"
              className="text-primary hover:text-primary/80 font-medium transition-all duration-300 hover:underline underline-offset-4"
            >
              Create one here
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
        </div>
      </div>
    </div>
  )
}
