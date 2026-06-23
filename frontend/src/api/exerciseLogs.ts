import apiClient from './client'
import type {
  CreateExerciseLogRequest,
  ExerciseLogDto,
} from '../types/workout'

export async function listExerciseLogs(logId: number): Promise<ExerciseLogDto[]> {
  const response = await apiClient.get<ExerciseLogDto[]>(`/logs/${logId}/exercises`)
  return response.data
}

export async function addExerciseLog(
  logId: number,
  data: CreateExerciseLogRequest,
  options: { signal?: AbortSignal } = {},
): Promise<ExerciseLogDto> {
  const response = await apiClient.post<ExerciseLogDto>(
    `/logs/${logId}/exercises`,
    data,
    { signal: options.signal },
  )
  return response.data
}

export async function deleteExerciseLog(logId: number, exerciseLogId: number): Promise<void> {
  await apiClient.delete(`/logs/${logId}/exercises/${exerciseLogId}`)
}
