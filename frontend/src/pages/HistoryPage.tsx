import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Select } from '../components/ui/Input'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { listLogs } from '../api/workoutLogs'
import { useQuery } from '../hooks/useQuery'

const PAGE_SIZE = 10

// `template.name` works as a sort property because Spring Data resolves nested
// JPA paths automatically (WorkoutLog.template -> WorkoutTemplate.name).
const SORT_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'date,desc', label: 'Date — newest first' },
  { value: 'date,asc', label: 'Date — oldest first' },
  { value: 'template.name,asc', label: 'Template name — A to Z' },
  { value: 'template.name,desc', label: 'Template name — Z to A' },
  { value: 'createdAt,desc', label: 'Created — newest first' },
]

export function HistoryPage() {
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState('date,desc')

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { setPage(0) }, [sort])

  const { data, isLoading, error } = useQuery(
    () => listLogs({ page, size: PAGE_SIZE, sort }),
    [page, sort],
    'Failed to load history.',
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Workout history</h1>
          <p className="text-sm text-gray-500">Every workout you've logged.</p>
        </div>
        <Link to="/log/new"><Button>+ Log workout</Button></Link>
      </div>

      <div className="bg-white rounded-2xl shadow p-4">
        <Select
          id="sort"
          label="Sort by"
          value={sort}
          onChange={e => setSort(e.target.value)}
        >
          {SORT_OPTIONS.map(o => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </Select>
      </div>

      <ErrorBanner message={error} />
      {isLoading && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="No workouts yet"
          description="Log your first workout to start building history."
          action={<Link to="/log/new"><Button>+ Log workout</Button></Link>}
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <ul className="space-y-3">
          {data.content.map(log => (
            <li key={log.id}>
              <Link
                to={`/logs/${log.id}`}
                className="block bg-white rounded-2xl shadow p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-center justify-between gap-2 flex-wrap">
                  <div>
                    <h3 className="font-semibold text-gray-800">{log.templateName}</h3>
                    <p className="text-xs text-gray-500 mt-0.5">{log.date}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge status={log.status} />
                    <span className="text-xs text-gray-500">
                      {log.exerciseLogs.length} exercise{log.exerciseLogs.length === 1 ? '' : 's'}
                    </span>
                  </div>
                </div>
              </Link>
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
