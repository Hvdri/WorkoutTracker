export interface NotificationDto {
  id: string
  userId: number
  type: string
  message: string
  workoutLogId: number | null
  createdAt: string
  read: boolean
}
