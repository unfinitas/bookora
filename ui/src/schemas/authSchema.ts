import { z } from 'zod'
import { userSchema } from './userSchema'

export const registerSchema = userSchema
  .pick({
    username: true,
    firstName: true,
    lastName: true,
    email: true,
    password: true,
  })
  .extend({
    confirmPassword: z
      .string()
      .min(1, 'Confirm password is required')
      .max(100),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

export const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

export type RegisterFormData = z.infer<typeof registerSchema>
export type RegisterRequest = Omit<RegisterFormData, 'confirmPassword'>
export type LoginFormData = z.infer<typeof loginSchema>
