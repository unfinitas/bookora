'use client'

import { LogOut } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { clearCredentials } from '@/store/authSlice'
import { useRouter } from 'next/navigation'
import { useToast } from '@/hooks/use-toast'

export function LogoutButton() {
  const dispatch = useAppDispatch()
  const { user } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()

  const handleLogout = () => {
    dispatch(clearCredentials())

    toast({
      title: 'Success',
      description: 'Logged out successfully!',
    })

    router.push('/')
  }

  if (!user) {
    return null
  }

  return (
    <Button onClick={handleLogout} variant="outline" size="sm" className="flex items-center gap-2">
      <LogOut className="h-4 w-4" />
      Logout ({user.username})
    </Button>
  )
}
