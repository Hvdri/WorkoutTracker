import { useState } from 'react'
import { Link } from 'react-router'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { Button } from '../components/ui/Button'
import { PostCard } from '../components/social/PostCard'
import { ShareWorkoutModal } from '../components/social/ShareWorkoutModal'
import { getFeed } from '../api/social'
import { useQuery } from '../hooks/useQuery'

const PAGE_SIZE = 10

export function FeedPage() {
  const [page, setPage] = useState(0)
  const [shareOpen, setShareOpen] = useState(false)
  const { data, isLoading, error, reload } = useQuery(
    () => getFeed({ page, size: PAGE_SIZE }),
    [page],
    'Failed to load feed.',
  )

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Feed</h1>
          <p className="text-sm text-gray-500">Workouts shared by people you follow.</p>
        </div>
        <Button onClick={() => setShareOpen(true)}>Share a workout</Button>
      </div>

      <ErrorBanner message={error} />
      {isLoading && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="Your feed is empty"
          description="Follow other users to see their workouts here."
          action={
            <Link
              to="/discover"
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              Find people to follow →
            </Link>
          }
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <div className="space-y-3">
          {data.content.map(p => <PostCard key={p.id} post={p} />)}
        </div>
      )}

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}

      <ShareWorkoutModal
        open={shareOpen}
        onClose={() => setShareOpen(false)}
        onShared={reload}
      />
    </div>
  )
}
