import { useState } from 'react'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { Button } from '../components/ui/Button'
import { PostCard } from '../components/social/PostCard'
import { follow, getDiscovery } from '../api/social'
import { useQuery } from '../hooks/useQuery'
import { extractErrorMessage } from '../utils/errors'

const PAGE_SIZE = 10

export function DiscoveryPage() {
  const [page, setPage] = useState(0)
  // Tracks the userId currently being followed so we can disable the right button
  // and surface a per-row error inline. Using an id (vs a boolean) keeps siblings
  // independent if the user fires multiple clicks in quick succession.
  const [followingUserId, setFollowingUserId] = useState<number | null>(null)
  const [followError, setFollowError] = useState<string | null>(null)

  const { data, isLoading, error, reload } = useQuery(
    () => getDiscovery({ page, size: PAGE_SIZE }),
    [page],
    'Failed to load discovery feed.',
  )

  async function handleFollow(userId: number) {
    setFollowError(null)
    setFollowingUserId(userId)
    try {
      await follow(userId)
      // After follow, that user's posts no longer match the "not-followed"
      // criterion, so refetching naturally removes them from the list.
      reload()
    } catch (err) {
      setFollowError(extractErrorMessage(err, 'Failed to follow user.'))
    } finally {
      setFollowingUserId(null)
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Discover</h1>
        <p className="text-sm text-gray-500">
          Workouts from people you don't follow yet. Tap Follow to add them to your feed.
        </p>
      </div>

      <ErrorBanner message={error} />
      <ErrorBanner message={followError} />
      {isLoading && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="Nothing new to discover"
          description="You already follow everyone who's shared a workout. Come back when more users post."
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <div className="space-y-3">
          {data.content.map(post => (
            <div key={post.id} className="space-y-2">
              <PostCard post={post} />
              <div className="flex justify-end">
                <Button
                  size="sm"
                  onClick={() => handleFollow(post.userId)}
                  disabled={followingUserId !== null}
                >
                  {followingUserId === post.userId ? 'Following…' : `Follow @${post.username}`}
                </Button>
              </div>
            </div>
          ))}
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
