import { Link } from 'react-router'
import type { PostDto } from '../../types/social'

export function PostCard({ post }: { post: PostDto }) {
  const date = new Date(post.createdAt)
  // templateName is plain text, NOT a link — `/logs/{id}` is owner-only on the
  // backend, so clicking through would 404 for everyone except the post author.
  return (
    <article className="bg-white rounded-2xl shadow p-4">
      <div className="flex items-baseline justify-between gap-2 flex-wrap">
        <Link
          to={`/users/${post.userId}`}
          className="font-semibold text-gray-800 hover:text-blue-700"
        >
          @{post.username}
        </Link>
        <time className="text-xs text-gray-500">{date.toLocaleString()}</time>
      </div>
      <p className="text-sm text-gray-600 mt-1">
        Workout: <span className="text-gray-800">{post.templateName}</span>
      </p>
      {post.caption && (
        <p className="text-sm text-gray-700 mt-2 whitespace-pre-wrap">{post.caption}</p>
      )}
    </article>
  )
}
