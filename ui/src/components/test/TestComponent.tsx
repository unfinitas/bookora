'use client'

import { useState, useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { increment, decrement } from '@/store/counterSlice'
import { Button } from '@/components/ui/button'
import { userSchema, type User } from '@/schemas/testSchema'

type RootState = {
  counter: {
    value: number
  }
}

export default function TestComponent() {
  const [mounted, setMounted] = useState(false)
  const count = useSelector((state: RootState) => state.counter.value)
  const dispatch = useDispatch()
  const [user, setUser] = useState<User | null>(null)
  const [error, setError] = useState<string>('')

  useEffect(() => {
    setMounted(true)
  }, [])

  const testZod = () => {
    try {
      const testData = {
        name: 'John Doe',
        email: 'john@example.com',
        age: 25,
      }
      const validatedUser = userSchema.parse(testData)
      setUser(validatedUser)
      setError('')
    } catch {
      setError('Validation failed')
    }
  }

  if (!mounted) {
    return (
      <div className="p-8 space-y-4">
        <h1 className="text-2xl font-bold">Test Component</h1>
        <p>Loading...</p>
      </div>
    )
  }

  return (
    <div className="p-8 space-y-4">
      <h1 className="text-2xl">Test Component</h1>

      <div className="space-y-2">
        <h2 className="text-lg">Redux Test</h2>
        <p>Count: {count}</p>
        <div className="space-x-2">
          <Button onClick={() => dispatch(increment())}>+</Button>
          <Button onClick={() => dispatch(decrement())}>-</Button>
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-lg">Zod Test</h2>
        <Button onClick={testZod}>Test Zod Validation</Button>
        {user && <p className="text-green-600">Valid user: {user.name}</p>}
        {error && <p className="text-red-600">{error}</p>}
      </div>

      <div className="space-y-2">
        <h2 className="text-lg">ShadCN UI Test</h2>
        <Button variant="outline">Outline Button</Button>
        <Button variant="destructive">Destructive Button</Button>
      </div>
    </div>
  )
}
