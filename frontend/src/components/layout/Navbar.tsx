import { useEffect, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { getUnreadCount } from '../../api/notifications'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard' },
  { to: '/splits', label: 'Splits' },
  { to: '/logs/new', label: 'Log workout' },
  { to: '/history', label: 'History' },
  { to: '/exercises', label: 'Exercises' },
  { to: '/feed', label: 'Feed' },
  { to: '/discover', label: 'Discover' },
  { to: '/profile/me', label: 'Profile' },
]

export function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false

  // Unread notification count for the bell badge. Refreshes whenever the route
  // changes (cheap GET to /api/notifications/unread-count, routed via Vite proxy
  // to notification-service:8082). No timer-based polling — keeps this minimal.
  const [unread, setUnread] = useState<number>(0)
  useEffect(() => {
    if (!user) return
    let cancelled = false
    getUnreadCount()
      .then(c => { if (!cancelled) setUnread(c) })
      .catch(() => { /* silent — notification-service may be down */ })
    return () => { cancelled = true }
  }, [user, location.pathname])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-white border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-6 flex-wrap">
          <span className="font-bold text-gray-800">WorkoutTracker</span>
          <nav className="flex items-center gap-3 flex-wrap">
            {NAV_ITEMS.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `text-sm ${isActive ? 'text-blue-600 font-medium' : 'text-gray-600 hover:text-gray-900'}`
                }
              >
                {item.label}
              </NavLink>
            ))}
            {isAdmin && (
              <NavLink
                to="/admin/exercises"
                className={({ isActive }) =>
                  `text-sm ${isActive ? 'text-blue-600 font-medium' : 'text-gray-600 hover:text-gray-900'}`
                }
              >
                Admin
              </NavLink>
            )}
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm text-gray-600">
          <NavLink
            to="/notifications"
            className={({ isActive }) =>
              `relative inline-flex items-center px-2 py-1 rounded ${
                isActive ? 'text-blue-600 font-medium' : 'text-gray-600 hover:text-gray-900'
              }`
            }
            aria-label={unread > 0 ? `${unread} unread notifications` : 'Notifications'}
          >
            <span aria-hidden="true">🔔</span>
            {unread > 0 && (
              <span className="ml-1 inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1 bg-red-500 text-white text-xs font-medium rounded-full">
                {unread > 99 ? '99+' : unread}
              </span>
            )}
          </NavLink>
          <span>{user?.username}</span>
          <button
            onClick={handleLogout}
            className="text-red-600 hover:text-red-700 font-medium"
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}
