import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="bg-white rounded-2xl shadow p-8 text-center max-w-sm">
        <p className="text-xs uppercase tracking-wide text-gray-400">404</p>
        <h1 className="text-2xl font-bold text-gray-800 mt-1">Page not found</h1>
        <p className="text-sm text-gray-500 mt-2">The page you tried to open doesn't exist.</p>
        <Link to="/" className="inline-block mt-4 text-blue-600 hover:underline text-sm">
          ← Back to dashboard
        </Link>
      </div>
    </div>
  )
}
