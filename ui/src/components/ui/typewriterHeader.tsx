'use client'

import { useEffect, useState } from 'react'

const typewriterTexts = [
  'Welcome to Bookora',
  'Ultimate Booking Management',
  'Connect customers with providers',
  'Manage services and bookings',
  'Seamless booking experience',
  'Provider availability tracking',
  'Guest booking without login',
  'Admin control and oversight',
]

export function TypewriterHeader() {
  const [currentText, setCurrentText] = useState('')

  useEffect(() => {
    const typeWriter = () => {
      let i = 0
      let textPos = 0
      let currentString = typewriterTexts[i]
      const speed = 100
      const deleteSpeed = 50
      const waitTime = 2000

      function type() {
        setCurrentText(currentString.substring(0, textPos) + '_')

        if (textPos++ === currentString.length) {
          setTimeout(() => deleteText(), waitTime)
        } else {
          setTimeout(type, speed)
        }
      }

      function deleteText() {
        setCurrentText(currentString.substring(0, textPos) + '_')

        if (textPos-- === 0) {
          i = (i + 1) % typewriterTexts.length
          currentString = typewriterTexts[i]
          setTimeout(type, 500)
        } else {
          setTimeout(deleteText, deleteSpeed)
        }
      }
      type()
    }

    typeWriter()
  }, [])

  return (
    <div className="text-center space-y-4">
      <div className="flex justify-center"></div>
      <div className="space-y-2">
        <h1 className="text-3xl font-bold text-balance bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent min-h-[3rem] font-mono tracking-tight">
          {currentText}
        </h1>
      </div>
    </div>
  )
}
