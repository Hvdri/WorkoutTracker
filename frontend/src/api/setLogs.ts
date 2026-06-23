import apiClient from './client'
import type {
  CreateSetLogRequest,
  SetLogDto,
  UpdateSetLogRequest,
} from '../types/workout'

export async function addSet(
  logId: number,
  exerciseLogId: number,
  data: CreateSetLogRequest,
): Promise<SetLogDto> {
  const response = await apiClient.post<SetLogDto>(
    `/logs/${logId}/exercises/${exerciseLogId}/sets`,
    data,
  )
  return response.data
}

export async function updateSet(
  logId: number,
  exerciseLogId: number,
  setId: number,
  data: UpdateSetLogRequest,
): Promise<SetLogDto> {
  const response = await apiClient.put<SetLogDto>(
    `/logs/${logId}/exercises/${exerciseLogId}/sets/${setId}`,
    data,
  )
  return response.data
}

export async function deleteSet(
  logId: number,
  exerciseLogId: number,
  setId: number,
): Promise<void> {
  await apiClient.delete(`/logs/${logId}/exercises/${exerciseLogId}/sets/${setId}`)
}
