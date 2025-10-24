'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ArrowRight, Rocket } from 'lucide-react'
import { CustomButton } from '@/components/ui/CustomButton'
import { useAppSelector } from '@/libs/redux/hook'

export default function Home() {
  const router = useRouter()
  const { isAuthenticated } = useAppSelector((state) => state.auth)

  useEffect(() => {
    if (isAuthenticated) {
      router.replace('/workplace')
    }
  }, [isAuthenticated, router])

  return (
    <div className="bg-background flex items-center justify-center">
      <section className="container mx-auto px-4 text-center py-20">
        <div className="max-w-4xl mx-auto space-y-8">
          <div
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 animate-fade-in">
            <Rocket className="h-4 w-4 text-primary animate-bounce" />
            <span className="text-sm font-semibold text-primary">
          New: Enhanced Booking Experience
        </span>
          </div>

          <h1 className="text-5xl md:text-7xl font-bold leading-tight">
            Ultimate Booking
            <span className="block text-primary mt-2">Management System</span>
          </h1>

          <p className="text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto font-light">
            Connect customers with providers seamlessly. Manage services, bookings, and availability with our
            comprehensive platform built for modern businesses.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link href="/login">
              <CustomButton size="lg" className="group">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
              </CustomButton>
            </Link>
            <Link href="/register">
              <CustomButton variant="outline" size="lg">
                Create Account
              </CustomButton>
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
