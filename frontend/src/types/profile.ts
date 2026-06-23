export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY'

export const GENDERS: Gender[] = ['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY']

export interface UserProfileDto {
  userId: number
  username: string
  displayName: string | null
  bio: string | null
  fitnessGoal: string | null
  heightCm: number | null
  weightKg: number | null
  gender: Gender | null
}

export interface UserProfileUpdateRequest {
  bio?: string | null
  fitnessGoal?: string | null
  heightCm?: number | null
  weightKg?: number | null
  gender?: Gender | null
}
