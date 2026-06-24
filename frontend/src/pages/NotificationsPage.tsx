import { useState } from 'react'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { listNotifications, markRead } from '../api/notifications'
import { useQuery } from '../hooks/useQuery'
import { extractErrorMessage } from '../utils/errors'

const PAGE_SIZE = 20

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const [actionError, setActionError] = useState<string | null>(null)
  const { data, isLoading, error, reload } = useQuery(
    () => listNotifications({ page, size: PAGE_SIZE }),
    [page],
    'Failed to load notifications.',
  )

  async function handleMarkRead(id: string) {
    setActionError(null)
    try {
      await markRead(id)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to mark notification as read.'))
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Notifications</h1>
        <p className="text-sm text-gray-500">Updates about your workouts.</p>
      </div>

      <ErrorBanner message={error} />
      <ErrorBanner message={actionError} />

      {isLoading && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="No notifications"
          description="Complete a workout to start getting notifications here."
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <ul className="space-y-2">
          {data.content.map(n => (
            <li
              key={n.id}
              className={`bg-white rounded-2xl shadow p-3 flex items-start gap-3 ${
                !n.read ? 'border-l-4 border-blue-500' : ''
              }`}
            >
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-800">{n.message}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {new Date(n.createdAt).toLocaleString()}
                </p>
              </div>
              {!n.read && (
                <button
                  onClick={() => handleMarkRead(n.id)}
                  className="text-xs text-blue-600 hover:text-blue-700 self-start whitespace-nowrap"
                >
                  Mark read
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
