import { useState } from 'react'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { PostCard } from '../components/social/PostCard'
import { getFeed } from '../api/social'
import { useQuery } from '../hooks/useQuery'

const PAGE_SIZE = 10

export function FeedPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useQuery(
    () => getFeed({ page, size: PAGE_SIZE }),
    [page],
    'Failed to load feed.',
  )

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Feed</h1>
        <p className="text-sm text-gray-500">Workouts shared by people you follow.</p>
      </div>

      <ErrorBanner message={error} />
      {isLoading && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="Your feed is empty"
          description="Follow other users to see their workouts here."
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
    </div>
  )
}
