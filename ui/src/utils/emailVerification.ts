interface EmailVerificationData {
  email: string
  timestamp: number
  expires: number
  hash: string // Simple hash for integrity check
}

const VERIFICATION_EXPIRY_MINUTES = 10
const STORAGE_KEY = 'pendingVerificationEmail'

/**
 * Simple hash function for data integrity
 */
function simpleHash(data: string): string {
  let hash = 0
  for (let i = 0; i < data.length; i++) {
    const char = data.charCodeAt(i)
    hash = (hash << 5) - hash + char
    hash = hash & hash // Convert to 32-bit integer
  }
  return hash.toString(36)
}

/**
 * Store email securely for verification
 * @param email - User's email address
 */
export function storeVerificationEmail(email: string): void {
  try {
    const timestamp = Date.now()
    const expires = timestamp + VERIFICATION_EXPIRY_MINUTES * 60 * 1000
    const hash = simpleHash(email + timestamp.toString())

    const emailData: EmailVerificationData = {
      email,
      timestamp,
      expires,
      hash,
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(emailData))
  } catch (error) {
    console.error('Failed to store verification email:', error)
    throw new Error('Failed to store verification email')
  }
}

/**
 * Retrieve and validate verification email
 * @returns email if valid, null if expired/invalid
 */
export function getVerificationEmail(): string | null {
  try {
    const storedData = sessionStorage.getItem(STORAGE_KEY)

    if (!storedData) {
      return null
    }

    const emailData: EmailVerificationData = JSON.parse(storedData)

    // Check if data has expired
    if (Date.now() > emailData.expires) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }

    // Verify data integrity
    const expectedHash = simpleHash(emailData.email + emailData.timestamp.toString())
    if (emailData.hash !== expectedHash) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }

    return emailData.email
  } catch (error) {
    console.error('Failed to retrieve verification email:', error)
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

/**
 * Clear verification email data
 */
export function clearVerificationEmail(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY)
  } catch (error) {
    console.error('Failed to clear verification email:', error)
  }
}

/**
 * Check if verification email exists and is valid
 */
export function hasValidVerificationEmail(): boolean {
  return getVerificationEmail() !== null
}
