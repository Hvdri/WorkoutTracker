export interface PostDto {
  id: number
  userId: number
  username: string
  workoutLogId: number
  templateName: string
  caption: string | null
  createdAt: string
}

export interface CreatePostRequest {
  workoutLogId: number
  caption?: string | null
}

export interface UserSummaryDto {
  id: number
  username: string
  bio: string | null
}
