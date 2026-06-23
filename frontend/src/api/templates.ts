import apiClient from './client'
import type {
  AddExerciseToTemplateRequest,
  CreateTemplateRequest,
  ExerciseTemplateDto,
  WorkoutTemplateDto,
} from '../types/workout'

export async function listTemplates(splitId: number): Promise<WorkoutTemplateDto[]> {
  const response = await apiClient.get<WorkoutTemplateDto[]>(`/splits/${splitId}/templates`)
  return response.data
}

export async function createTemplate(splitId: number, data: CreateTemplateRequest): Promise<WorkoutTemplateDto> {
  const response = await apiClient.post<WorkoutTemplateDto>(`/splits/${splitId}/templates`, data)
  return response.data
}

export async function deleteTemplate(splitId: number, templateId: number): Promise<void> {
  await apiClient.delete(`/splits/${splitId}/templates/${templateId}`)
}

export async function addExerciseToTemplate(
  splitId: number,
  templateId: number,
  data: AddExerciseToTemplateRequest,
): Promise<ExerciseTemplateDto> {
  const response = await apiClient.post<ExerciseTemplateDto>(
    `/splits/${splitId}/templates/${templateId}/exercises`,
    data,
  )
  return response.data
}

export async function removeExerciseFromTemplate(
  splitId: number,
  templateId: number,
  exerciseTemplateId: number,
): Promise<void> {
  await apiClient.delete(
    `/splits/${splitId}/templates/${templateId}/exercises/${exerciseTemplateId}`,
  )
}
