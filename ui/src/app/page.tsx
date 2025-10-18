import Link from 'next/link'
import { ArrowRight, Rocket, Zap, Shield, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { ThemeToggle } from '@/components/ui/themeToggle'

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b border-border/40 backdrop-blur-sm sticky top-0 z-50 bg-background/80">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
              <Rocket className="h-5 w-5 text-primary-foreground animate-pulse" />
            </div>
            <span className="font-bold text-xl tracking-tight">Bookora</span>
          </div>
          <div className="flex items-center gap-4">
            <ThemeToggle />
            <Link href="/login">
              <Button variant="ghost">Login</Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="container mx-auto px-4 py-20 md:py-32">
        <div className="max-w-4xl mx-auto text-center space-y-8">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 animate-fade-in">
            <Rocket className="h-4 w-4 text-primary animate-bounce" />
            <span className="text-sm font-semibold text-primary">New: Enhanced Booking Experience</span>
          </div>

          <h1 className="text-5xl md:text-7xl font-bold text-balance leading-tight tracking-tight">
            Ultimate Booking
            <span className="block text-primary mt-2">Management System</span>
          </h1>

          <p className="text-xl text-muted-foreground text-pretty leading-relaxed max-w-2xl mx-auto font-light">
            Connect customers with providers seamlessly. Manage services, bookings, and availability with our
            comprehensive platform built for modern businesses.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link href="/login">
              <Button size="lg" className="group">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Button>
            </Link>
            <Link href="/register">
              <Button variant="outline" size="lg">
                Create Account
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/40 mt-20">
        <div className="container mx-auto px-4 py-8">
          <p className="text-center text-sm text-muted-foreground">© Bookora {new Date().getFullYear()}</p>
        </div>
      </footer>
    </div>
  )
}
