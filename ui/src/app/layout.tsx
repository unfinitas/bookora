import React, { Suspense } from 'react'
import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import StoreProvider from './storeProvider'
import '@/styles/globals.css'
import ClientBody from '@/app/clientBody'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Bookora - Book Management Platform',
  description: 'Your comprehensive book management and booking platform',
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className="h-full light" suppressHydrationWarning>
      <body className={`${inter.className} antialiased h-full flex flex-col`}>
        <StoreProvider>
          <Suspense fallback={<div className="p-8 text-muted-foreground">Loading...</div>}>
            <ClientBody>{children}</ClientBody>
          </Suspense>
        </StoreProvider>
      </body>
    </html>
  )
}
