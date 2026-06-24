import { NavLink, useNavigate } from 'react-router'
import { useAuth } from '../../hooks/useAuth'

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
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false

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
