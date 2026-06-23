import apiClient from './client'
import type { CreateSplitRequest, WorkoutSplitDto } from '../types/workout'

export async function listSplits(): Promise<WorkoutSplitDto[]> {
  const response = await apiClient.get<WorkoutSplitDto[]>('/splits')
  return response.data
}

// Backend returns 204 No Content when there is no active split.
export async function getActiveSplit(): Promise<WorkoutSplitDto | null> {
  const response = await apiClient.get<WorkoutSplitDto | ''>('/splits/active')
  if (response.status === 204) return null
  return response.data as WorkoutSplitDto
}

export async function getSplitById(id: number): Promise<WorkoutSplitDto> {
  const response = await apiClient.get<WorkoutSplitDto>(`/splits/${id}`)
  return response.data
}

export async function createSplit(data: CreateSplitRequest): Promise<WorkoutSplitDto> {
  const response = await apiClient.post<WorkoutSplitDto>('/splits', data)
  return response.data
}

export async function activateSplit(id: number): Promise<WorkoutSplitDto> {
  const response = await apiClient.put<WorkoutSplitDto>(`/splits/${id}/activate`)
  return response.data
}

export async function deleteSplit(id: number): Promise<void> {
  await apiClient.delete(`/splits/${id}`)
}
