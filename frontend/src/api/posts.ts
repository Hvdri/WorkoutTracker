import apiClient from './client'
import type { CreatePostRequest, PostDto } from '../types/social'

export async function createPost(data: CreatePostRequest): Promise<PostDto> {
  const response = await apiClient.post<PostDto>('/posts', data)
  return response.data
}

export async function deletePost(id: number): Promise<void> {
  await apiClient.delete(`/posts/${id}`)
}
