import { z } from 'zod'

export const UserRole = {
  USER: 'USER',
  PROVIDER: 'PROVIDER',
  ADMIN: 'ADMIN',
} as const

export type UserRoleType = keyof typeof UserRole

export const userSchema = z.object({
  id: z.string().uuid(),
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be at most 50 characters')
    .regex(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens'),
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName: z.string().min(1, 'Last name is required').max(100),
  email: z.string().email('Please enter a valid email address'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(100)
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,100}$/,
      'Password must contain at least one lowercase letter, one uppercase letter, one number, and one special character (@$!%*?&)',
    ),
  role: z.enum(UserRole).default(UserRole.USER),
  isGuest: z.boolean().default(false),
  createdAt: z.date().optional(),
  updatedAt: z.date().optional(),
})

export const userPublicInfoSchema = userSchema.pick({
  id: true,
  username: true,
  role: true,
})

export type User = z.infer<typeof userSchema>
export type UserPublicInfo = z.infer<typeof userPublicInfoSchema>
