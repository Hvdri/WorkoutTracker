import apiClient from './client'
import type { PageResponse } from '../types/api'
import type { PostDto, UserSummaryDto } from '../types/social'

export async function follow(userId: number): Promise<void> {
  await apiClient.post(`/social/follow/${userId}`)
}

export async function unfollow(userId: number): Promise<void> {
  await apiClient.delete(`/social/follow/${userId}`)
}

export async function listFollowing(): Promise<UserSummaryDto[]> {
  const response = await apiClient.get<UserSummaryDto[]>('/social/following')
  return response.data
}

export async function listFollowers(): Promise<UserSummaryDto[]> {
  const response = await apiClient.get<UserSummaryDto[]>('/social/followers')
  return response.data
}

export interface FeedParams {
  page?: number
  size?: number
}

export async function getFeed(params: FeedParams = {}): Promise<PageResponse<PostDto>> {
  const response = await apiClient.get<PageResponse<PostDto>>('/social/feed', { params })
  return response.data
}
