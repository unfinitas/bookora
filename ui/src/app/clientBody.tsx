'use client';

import React from 'react';
import { usePathname } from 'next/navigation';
import { Toaster } from '@/components/ui/Toaster';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';

export default function ClientBody({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const hideLayout = ['/login', '/register', '/verify-email'].includes(pathname);

  return (
    <>
      {!hideLayout && <Header />}
      <main className="flex-1">{children}</main>
      {!hideLayout && <Footer />}
      <Toaster />
    </>
  );
}
