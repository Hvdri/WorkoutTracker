import { Link, isRouteErrorResponse, useRouteError } from 'react-router'

export function ErrorPage() {
  const err = useRouteError()
  const isRouteResponse = isRouteErrorResponse(err)
  const status = isRouteResponse ? err.status : 500
  const heading =
    isRouteResponse && status === 404 ? 'Page not found' : 'Something went wrong'
  const detail = isRouteResponse
    ? err.statusText || err.data
    : err instanceof Error
      ? err.message
      : 'Unexpected error'

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="bg-white rounded-2xl shadow p-8 text-center max-w-md">
        <p className="text-xs uppercase tracking-wide text-gray-400">{status}</p>
        <h1 className="text-2xl font-bold text-gray-800 mt-1">{heading}</h1>
        {detail && <p className="text-sm text-gray-500 mt-2 break-words">{detail}</p>}
        <Link to="/" className="inline-block mt-4 text-blue-600 hover:underline text-sm">
          ← Back to dashboard
        </Link>
      </div>
    </div>
  )
}
