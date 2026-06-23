export type MuscleGroup =
  | 'CHEST'
  | 'BACK'
  | 'SHOULDERS'
  | 'BICEPS'
  | 'TRICEPS'
  | 'LEGS'
  | 'GLUTES'
  | 'CORE'
  | 'CARDIO'

export const MUSCLE_GROUPS: MuscleGroup[] = [
  'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS',
  'LEGS', 'GLUTES', 'CORE', 'CARDIO',
]

export type WorkoutStatus = 'IN_PROGRESS' | 'COMPLETED'

export interface ExerciseDto {
  id: number
  name: string
  description: string | null
  muscleGroup: MuscleGroup
  imageUrl: string | null
}

export interface CreateExerciseRequest {
  name: string
  description?: string | null
  muscleGroup: MuscleGroup
  imageUrl?: string | null
}

export interface ExerciseTemplateDto {
  id: number
  exerciseId: number
  exerciseName: string
  muscleGroup: MuscleGroup
  targetSets: number | null
  targetReps: number | null
  orderIndex: number
}

export interface AddExerciseToTemplateRequest {
  exerciseId: number
  targetSets?: number | null
  targetReps?: number | null
  orderIndex: number
}

export interface WorkoutTemplateDto {
  id: number
  name: string
  orderIndex: number
  exercises: ExerciseTemplateDto[]
}

export interface CreateTemplateRequest {
  name: string
  orderIndex: number
}

export interface WorkoutSplitDto {
  id: number
  name: string
  isActive: boolean
  createdAt: string
  templates: WorkoutTemplateDto[]
}

export interface CreateSplitRequest {
  name: string
}

export interface SetLogDto {
  id: number
  setNumber: number
  weightKg: number
  reps: number
  rpe: number | null
}

export interface CreateSetLogRequest {
  setNumber: number
  weightKg: number
  reps: number
  rpe?: number | null
}

export interface UpdateSetLogRequest {
  weightKg?: number
  reps?: number
  rpe?: number | null
}

export interface ExerciseLogDto {
  id: number
  exerciseId: number
  exerciseName: string
  muscleGroup: MuscleGroup
  notes: string | null
  sets: SetLogDto[]
}

export interface CreateExerciseLogRequest {
  exerciseId: number
  notes?: string | null
}

export interface WorkoutLogDto {
  id: number
  date: string
  photoUrl: string | null
  status: WorkoutStatus
  notes: string | null
  templateId: number
  templateName: string
  exerciseLogs: ExerciseLogDto[]
  createdAt: string
}

export interface CreateWorkoutLogRequest {
  date: string
  templateId: number
  photoUrl?: string | null
  notes?: string | null
}

export interface UpdateWorkoutLogRequest {
  photoUrl?: string | null
  notes?: string | null
  status?: WorkoutStatus
}
