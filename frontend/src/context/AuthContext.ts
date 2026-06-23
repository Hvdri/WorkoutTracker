import { createContext } from 'react'
import type { AuthUser, LoginRequest, RegisterRequest } from '../types/auth'

// Context object is exported from a non-component file so the AuthProvider file
// can stay component-only, satisfying `react-refresh/only-export-components`.
export interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
