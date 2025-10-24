'use client'

import React from 'react'
import { LogOut } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { CustomButton } from '@/components/ui/CustomButton'
import { useToast } from '@/hooks/useToast'
import { useAppDispatch, useAppSelector } from '@/libs/redux/hook'
import { persistor } from '@/libs/redux/store'
import { AuthAction } from '@/libs/redux/auth/authReducer'
import AuthThunk from '@/libs/redux/auth/authThunk'

export function LogoutButton() {
  const dispatch = useAppDispatch()
  const { username, token } = useAppSelector((state) => state.auth)
  const { toast } = useToast()
  const router = useRouter()
  const [loading, setLoading] = React.useState(false)

  const handleLogout = async () => {
    setLoading(true)
    try {
      dispatch(AuthAction.logout())

      if (token) await dispatch(AuthThunk.LogoutThunk())

      persistor.purge().catch(console.error)

      router.push('/')

      toast({ title: 'Success', description: 'Logged out successfully!' })
    } finally {
      setLoading(false)
    }
  }

  if (!username) return null

  return (
    <CustomButton
      onClick={handleLogout}
      variant="outline"
      size="sm"
      disabled={loading}
      className="flex items-center gap-2"
    >
      <LogOut className="h-4 w-4" />
      {loading ? 'Logging out...' : `Logout (${username})`}
    </CustomButton>
  )
}
