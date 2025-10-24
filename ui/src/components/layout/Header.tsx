'use client'

import Link from 'next/link'
import { Rocket } from 'lucide-react'
import { CustomButton } from '@/components/ui/CustomButton'
import { useAppSelector } from '@/libs/redux/hook'

export function Header() {
  const { isAuthenticated } = useAppSelector((state) => state.auth)

  return (
    <header className="border-b border-border/40 backdrop-blur-sm sticky top-0 z-50 bg-background/80">
      <div className="container mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
            <Rocket className="h-5 w-5 text-primary-foreground animate-pulse" />
          </div>
          <span className="font-bold text-xl tracking-tight">Bookora</span>
        </div>

        <div className="flex items-center gap-4">
          {isAuthenticated ? (
            <Link href="/workplace">
              <CustomButton variant="outline">Dashboard</CustomButton>
            </Link>
          ) : (
            <>
              <Link href="/login">
                <CustomButton variant="ghost">Login</CustomButton>
              </Link>
              <Link href="/register">
                <CustomButton>Sign Up</CustomButton>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
