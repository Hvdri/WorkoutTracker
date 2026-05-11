import { Navigate } from 'react-router'
import type { ReactNode } from 'react'
import { useAuth } from '../hooks/useAuth'

export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return <div className="p-6 text-sm text-gray-500">Loading…</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!user.roles.includes('ROLE_ADMIN')) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}
