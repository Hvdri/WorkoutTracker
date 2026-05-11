import apiClient from './client'
import type { PageResponse } from '../types/api'
import type {
  CreateWorkoutLogRequest,
  UpdateWorkoutLogRequest,
  WorkoutLogDto,
} from '../types/workout'

export interface ListLogsParams {
  page?: number
  size?: number
  sort?: string
}

export async function listLogs(params: ListLogsParams = {}): Promise<PageResponse<WorkoutLogDto>> {
  const response = await apiClient.get<PageResponse<WorkoutLogDto>>('/logs', { params })
  return response.data
}

export async function getLog(id: number): Promise<WorkoutLogDto> {
  const response = await apiClient.get<WorkoutLogDto>(`/logs/${id}`)
  return response.data
}

export async function createLog(data: CreateWorkoutLogRequest): Promise<WorkoutLogDto> {
  const response = await apiClient.post<WorkoutLogDto>('/logs', data)
  return response.data
}

export async function updateLog(id: number, data: UpdateWorkoutLogRequest): Promise<WorkoutLogDto> {
  const response = await apiClient.put<WorkoutLogDto>(`/logs/${id}`, data)
  return response.data
}

export async function deleteLog(id: number): Promise<void> {
  await apiClient.delete(`/logs/${id}`)
}

export async function completeLog(id: number): Promise<WorkoutLogDto> {
  const response = await apiClient.post<WorkoutLogDto>(`/logs/${id}/complete`)
  return response.data
}
