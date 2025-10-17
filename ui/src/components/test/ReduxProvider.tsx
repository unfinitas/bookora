'use client'

import { Provider } from 'react-redux'
import { useRef } from 'react'
import { configureStore } from '@reduxjs/toolkit'
import counterSlice from '@/store/counterSlice'

export function ReduxProvider({ children }: { children: React.ReactNode }) {
  const storeRef = useRef<ReturnType<typeof configureStore> | null>(null)

  if (!storeRef.current) {
    storeRef.current = configureStore({
      reducer: {
        counter: counterSlice,
      },
    })
  }

  return <Provider store={storeRef.current}>{children}</Provider>
}
