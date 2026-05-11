import apiClient from './client'
import type { PageResponse } from '../types/api'
import type {
  CreateExerciseRequest,
  ExerciseDto,
  MuscleGroup,
} from '../types/workout'

export interface ListExercisesParams {
  muscleGroup?: MuscleGroup
  name?: string
  page?: number
  size?: number
  sort?: string
}

export async function listExercises(params: ListExercisesParams = {}): Promise<PageResponse<ExerciseDto>> {
  const response = await apiClient.get<PageResponse<ExerciseDto>>('/exercises', { params })
  return response.data
}

export async function getExerciseById(id: number): Promise<ExerciseDto> {
  const response = await apiClient.get<ExerciseDto>(`/exercises/${id}`)
  return response.data
}

export async function createExercise(data: CreateExerciseRequest): Promise<ExerciseDto> {
  const response = await apiClient.post<ExerciseDto>('/exercises', data)
  return response.data
}

export async function updateExercise(id: number, data: CreateExerciseRequest): Promise<ExerciseDto> {
  const response = await apiClient.put<ExerciseDto>(`/exercises/${id}`, data)
  return response.data
}

export async function deleteExercise(id: number): Promise<void> {
  await apiClient.delete(`/exercises/${id}`)
}
