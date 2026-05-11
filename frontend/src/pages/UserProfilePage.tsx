import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { Button } from '../components/ui/Button'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { PostCard } from '../components/social/PostCard'
import {
  getMyProfile,
  getUserFollowers,
  getUserFollowing,
  getUserPosts,
  getUserProfile,
} from '../api/profile'
import { follow, listFollowing, unfollow } from '../api/social'
import type { UserProfileDto } from '../types/profile'
import type { PostDto } from '../types/social'
import type { PageResponse } from '../types/api'
import { extractErrorMessage } from '../utils/errors'

const PAGE_SIZE = 10

export function UserProfilePage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)

  const [profile, setProfile] = useState<UserProfileDto | null>(null)
  const [myUserId, setMyUserId] = useState<number | null>(null)
  const [isFollowing, setIsFollowing] = useState(false)
  const [followerCount, setFollowerCount] = useState<number | null>(null)
  const [followingCount, setFollowingCount] = useState<number | null>(null)

  const [posts, setPosts] = useState<PageResponse<PostDto> | null>(null)
  const [postsError, setPostsError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isMutating, setIsMutating] = useState(false)

  // Initial load: profile + own id + isFollowing + follower/following counts.
  useEffect(() => {
    if (!Number.isFinite(userId)) {
      setError('Invalid user id.')
      setIsLoading(false)
      return
    }
    let cancelled = false
    Promise.all([
      getUserProfile(userId),
      getMyProfile(),
      listFollowing(),
      getUserFollowers(userId),
      getUserFollowing(userId),
    ])
      .then(([p, me, mine, theirFollowers, theirFollowing]) => {
        if (cancelled) return
        setProfile(p)
        setMyUserId(me.userId)
        setIsFollowing(mine.some(u => u.id === userId))
        setFollowerCount(theirFollowers.length)
        setFollowingCount(theirFollowing.length)
        setError(null)
      })
      .catch(err => {
        if (!cancelled) setError(extractErrorMessage(err, 'Failed to load user.'))
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
  }, [userId])

  // Posts pagination — separate so a posts failure doesn't block the profile header.
  useEffect(() => {
    if (!Number.isFinite(userId)) return
    let cancelled = false
    getUserPosts(userId, { page, size: PAGE_SIZE })
      .then(res => {
        if (cancelled) return
        setPosts(res)
        setPostsError(null)
      })
      .catch(err => {
        if (!cancelled) setPostsError(extractErrorMessage(err, 'Failed to load posts.'))
      })
    return () => { cancelled = true }
  }, [userId, page])

  async function handleFollowToggle() {
    setActionError(null)
    setIsMutating(true)
    try {
      if (isFollowing) {
        await unfollow(userId)
        setIsFollowing(false)
        setFollowerCount(c => (c == null ? c : Math.max(0, c - 1)))
      } else {
        await follow(userId)
        setIsFollowing(true)
        setFollowerCount(c => (c == null ? c : c + 1))
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to update follow state.'))
    } finally {
      setIsMutating(false)
    }
  }

  if (isLoading) return <LoadingSpinner />
  if (error && !profile) return <ErrorBanner message={error} />
  if (!profile) return null

  const isSelf = myUserId === profile.userId
  const postCount = posts?.totalElements ?? null

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-2xl shadow p-4 flex items-start justify-between gap-2 flex-wrap">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">@{profile.username}</h1>
          {profile.bio && <p className="text-sm text-gray-600 mt-1 max-w-prose">{profile.bio}</p>}
          {profile.fitnessGoal && (
            <p className="text-xs text-gray-500 mt-2">Goal: {profile.fitnessGoal}</p>
          )}
          <div className="flex gap-5 mt-3 text-sm">
            <Stat label="posts" value={postCount} />
            <Stat label="followers" value={followerCount} />
            <Stat label="following" value={followingCount} />
          </div>
        </div>
        {!isSelf && (
          <Button
            variant={isFollowing ? 'secondary' : 'primary'}
            disabled={isMutating}
            onClick={handleFollowToggle}
          >
            {isFollowing ? 'Unfollow' : 'Follow'}
          </Button>
        )}
      </div>

      <ErrorBanner message={actionError} />

      <div>
        <h2 className="text-lg font-semibold text-gray-800 mb-2">Posts</h2>
        <ErrorBanner message={postsError} />
        {posts && posts.content.length === 0 && !postsError && (
          <EmptyState title="No public posts yet" />
        )}
        {posts && posts.content.length > 0 && (
          <div className="space-y-3">
            {posts.content.map(p => <PostCard key={p.id} post={p} />)}
          </div>
        )}
        {posts && (
          <Pagination
            page={posts.page}
            totalPages={posts.totalPages}
            totalElements={posts.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number | null }) {
  return (
    <span>
      <span className="font-semibold text-gray-800">{value ?? '—'}</span>{' '}
      <span className="text-gray-500">{label}</span>
    </span>
  )
}
