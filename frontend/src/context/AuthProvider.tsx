import { useEffect, useState, type ReactNode } from 'react'
import { login as apiLogin, register as apiRegister } from '../api/auth'
import type { AuthUser, AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'
import { STORAGE_KEYS } from '../constants/storage'
import { AuthContext } from './AuthContext'

// Decode the JWT payload and check the exp claim without verifying the signature.
// We only use this to avoid showing the dashboard when the token is already expired.
// The backend always validates the signature — this is just a UX shortcut.
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

// Validate that a parsed object matches the AuthUser shape before trusting it.
function isValidAuthUser(obj: unknown): obj is AuthUser {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    typeof (obj as AuthUser).username === 'string' &&
    Array.isArray((obj as AuthUser).roles) &&
    (obj as AuthUser).roles.every(r => typeof r === 'string')
  )
}

function clearStorage() {
  localStorage.removeItem(STORAGE_KEYS.TOKEN)
  localStorage.removeItem(STORAGE_KEYS.USER)
}

// Pure read of localStorage. Returns the recovered user plus a `corrupt` flag for
// the cleanup effect — never mutates. Two callers: the lazy useState initializer
// (just the user) and the mount effect (just `corrupt`). Keeping reads pure means
// render stays side-effect-free even in React's concurrent / strict modes.
function readStoredUser(): { user: AuthUser | null; corrupt: boolean } {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
  const stored = localStorage.getItem(STORAGE_KEYS.USER)
  if (!token && !stored) return { user: null, corrupt: false }
  if (!token || !stored || isTokenExpired(token)) return { user: null, corrupt: true }
  try {
    const parsed: unknown = JSON.parse(stored)
    if (isValidAuthUser(parsed)) return { user: parsed, corrupt: false }
  } catch {
    // fall through to corrupt
  }
  return { user: null, corrupt: true }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // useState's lazy initializer runs once at mount; pairs with the cleanup effect
  // below to handle the corrupt-storage case without flickering between commits.
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser().user)

  // Kept as a context value for downstream callers that already expect it
  // (ProtectedRoute uses it). Always false now that hydration is synchronous;
  // left in the API for forward compatibility (e.g. async token refresh later).
  const isLoading = false

  // If localStorage was in a corrupt state at mount, wipe it. Effect runs after
  // first commit so render stays pure.
  useEffect(() => {
    if (readStoredUser().corrupt) clearStorage()
  }, [])

  function persistAuth(response: AuthResponse) {
    const authUser: AuthUser = { username: response.username, roles: response.roles }
    localStorage.setItem(STORAGE_KEYS.TOKEN, response.token)
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(authUser))
    setUser(authUser)
  }

  async function login(data: LoginRequest) {
    persistAuth(await apiLogin(data))
  }

  async function register(data: RegisterRequest) {
    persistAuth(await apiRegister(data))
  }

  function logout() {
    clearStorage()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
