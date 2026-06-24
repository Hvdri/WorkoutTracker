import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Button } from '../components/ui/Button'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { Pill, StatusBadge } from '../components/ui/Badge'
import { useAuth } from '../hooks/useAuth'
import { getActiveSplit } from '../api/splits'
import { listLogs } from '../api/workoutLogs'
import { listFollowers, listFollowing } from '../api/social'
import { getUserPosts } from '../api/profile'
import type { WorkoutLogDto, WorkoutSplitDto } from '../types/workout'

export function DashboardPage() {
  const { user } = useAuth()
  const [split, setSplit] = useState<WorkoutSplitDto | null>(null)
  const [recentLogs, setRecentLogs] = useState<WorkoutLogDto[]>([])
  const [followingCount, setFollowingCount] = useState<number | null>(null)
  const [followerCount, setFollowerCount] = useState<number | null>(null)
  const [postCount, setPostCount] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    // user can't be null inside ProtectedRoute, but TS doesn't know that — guard for it.
    if (!user) return
    Promise.all([
      getActiveSplit(),
      listLogs({ page: 0, size: 3, sort: 'date,desc' }),
      listFollowing(),
      listFollowers(),
      // size=1 is the cheapest way to get a count: we only read `totalElements`.
      // Goes through the Vite proxy to social-service.
      getUserPosts(user.userId, { page: 0, size: 1 }),
    ])
      .then(([s, logs, following, followers, posts]) => {
        if (cancelled) return
        setSplit(s)
        setRecentLogs(logs.content)
        setFollowingCount(following.length)
        setFollowerCount(followers.length)
        setPostCount(posts.totalElements)
        setError(null)
      })
      .catch(() => {
        if (!cancelled) setError('Failed to load dashboard.')
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  if (isLoading) return <LoadingSpinner />

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Welcome, {user?.username}</h1>
        <p className="text-sm text-gray-500">{user?.roles.join(' · ')}</p>
      </div>

      <ErrorBanner message={error} />

      <div className="grid md:grid-cols-2 gap-4">
        <ActiveSplitCard split={split} />

        <section className="bg-white rounded-2xl shadow p-4">
          <h2 className="font-semibold text-gray-800 mb-2">Quick action</h2>
          <p className="text-sm text-gray-600 mb-3">Start logging today's workout.</p>
          <Link to="/logs/new"><Button>Start today's workout</Button></Link>
        </section>
      </div>

      <section className="bg-white rounded-2xl shadow p-4">
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-semibold text-gray-800">Recent workouts</h2>
          <Link to="/history" className="text-sm text-blue-600 hover:underline">See all →</Link>
        </div>
        {recentLogs.length === 0 ? (
          <p className="text-sm text-gray-500">No workouts logged yet.</p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {recentLogs.map(log => (
              <li key={log.id}>
                <Link
                  to={`/logs/${log.id}`}
                  className="flex items-center justify-between py-2 hover:bg-gray-50 rounded px-1"
                >
                  <div>
                    <div className="text-sm font-medium text-gray-800">{log.templateName}</div>
                    <div className="text-xs text-gray-500">{log.date}</div>
                  </div>
                  <StatusBadge status={log.status} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="bg-white rounded-2xl shadow p-4">
        <h2 className="font-semibold text-gray-800 mb-2">Social</h2>
        <div className="flex gap-6 text-sm flex-wrap">
          <span>
            <span className="text-2xl font-bold text-gray-800">{postCount ?? '—'}</span>{' '}
            <span className="text-gray-500">{postCount === 1 ? 'post' : 'posts'}</span>
          </span>
          <span>
            <span className="text-2xl font-bold text-gray-800">{followingCount ?? '—'}</span>{' '}
            <span className="text-gray-500">following</span>
          </span>
          <span>
            <span className="text-2xl font-bold text-gray-800">{followerCount ?? '—'}</span>{' '}
            <span className="text-gray-500">followers</span>
          </span>
          <Link to="/feed" className="ml-auto text-blue-600 hover:underline self-end">
            Open feed →
          </Link>
        </div>
      </section>
    </div>
  )
}

function ActiveSplitCard({ split }: { split: WorkoutSplitDto | null }) {
  if (!split) {
    return (
      <section className="bg-white rounded-2xl shadow p-4">
        <h2 className="font-semibold text-gray-800 mb-2">Active split</h2>
        <p className="text-sm text-gray-600 mb-3">No active split. Create one to start logging workouts.</p>
        <Link to="/splits"><Button variant="secondary">Manage splits</Button></Link>
      </section>
    )
  }
  const sortedTemplates = [...split.templates].sort((a, b) => a.orderIndex - b.orderIndex)
  return (
    <section className="bg-white rounded-2xl shadow p-4">
      <div className="flex items-center justify-between mb-2">
        <h2 className="font-semibold text-gray-800 flex items-center gap-2">
          {split.name}
          <Pill className="bg-green-100 text-green-700">Active</Pill>
        </h2>
        <Link to={`/splits/${split.id}`} className="text-sm text-blue-600 hover:underline">Open →</Link>
      </div>
      {sortedTemplates.length === 0 ? (
        <p className="text-sm text-gray-500">No templates yet.</p>
      ) : (
        <ul className="text-sm text-gray-700 space-y-0.5">
          {sortedTemplates.map(t => (
            <li key={t.id}>
              <span className="font-medium">{t.name}</span>{' '}
              <span className="text-xs text-gray-500">
                ({t.exercises.length} exercise{t.exercises.length === 1 ? '' : 's'})
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
