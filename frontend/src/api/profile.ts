import apiClient from './client'
import type { PageResponse } from '../types/api'
import type { PostDto, UserSummaryDto } from '../types/social'
import type { UserProfileDto, UserProfileUpdateRequest } from '../types/profile'

export async function getMyProfile(): Promise<UserProfileDto> {
  const response = await apiClient.get<UserProfileDto>('/users/me/profile')
  return response.data
}

export async function updateMyProfile(data: UserProfileUpdateRequest): Promise<UserProfileDto> {
  const response = await apiClient.put<UserProfileDto>('/users/me/profile', data)
  return response.data
}

export async function getUserProfile(id: number): Promise<UserProfileDto> {
  const response = await apiClient.get<UserProfileDto>(`/users/${id}/profile`)
  return response.data
}

export interface UserPostsParams {
  page?: number
  size?: number
}

export async function getUserPosts(id: number, params: UserPostsParams = {}): Promise<PageResponse<PostDto>> {
  const response = await apiClient.get<PageResponse<PostDto>>(`/users/${id}/posts`, { params })
  return response.data
}

export async function getUserFollowers(id: number): Promise<UserSummaryDto[]> {
  const response = await apiClient.get<UserSummaryDto[]>(`/users/${id}/followers`)
  return response.data
}

export async function getUserFollowing(id: number): Promise<UserSummaryDto[]> {
  const response = await apiClient.get<UserSummaryDto[]>(`/users/${id}/following`)
  return response.data
}
