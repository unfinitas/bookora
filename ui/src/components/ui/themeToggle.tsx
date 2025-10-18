'use client'

import { Moon, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'
import { useEffect, useState } from 'react'

export function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <div className="flex items-center gap-2 p-2 bg-card/50 backdrop-blur-sm border border-border/50">
        <Sun className="h-4 w-4 text-muted-foreground" />
        <div className="w-9 h-5 bg-muted rounded-full" />
        <Moon className="h-4 w-4 text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2 p-2 bg-card/50 backdrop-blur-sm border border-border/50 transition-all duration-300 hover:border-primary/50">
      <Sun
        className={`h-4 w-4 transition-all duration-300 ${
          theme === 'light' ? 'text-primary scale-110' : 'text-muted-foreground'
        }`}
      />
      <button
        onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
        className="relative w-9 h-5 bg-muted rounded-full transition-all duration-300 hover:bg-muted/80"
        aria-label="Toggle theme"
      >
        <div
          className={`absolute top-0.5 left-0.5 w-4 h-4 bg-primary rounded-full transition-transform duration-300 ${
            theme === 'dark' ? 'translate-x-4' : 'translate-x-0'
          }`}
        />
      </button>
      <Moon
        className={`h-4 w-4 transition-all duration-300 ${
          theme === 'dark' ? 'text-primary scale-110' : 'text-muted-foreground'
        }`}
      />
    </div>
  )
}
