import apiClient from './client'
import type { PageResponse } from '../types/api'
import type { NotificationDto } from '../types/notifications'

export interface ListNotificationsParams {
  page?: number
  size?: number
}

export async function listNotifications(
  params: ListNotificationsParams = {},
): Promise<PageResponse<NotificationDto>> {
  const response = await apiClient.get<PageResponse<NotificationDto>>('/notifications', { params })
  return response.data
}

export async function getUnreadCount(): Promise<number> {
  // Tiny endpoint that returns just `{ count: N }` — used by the navbar bell. We
  // don't poll it on a timer; it refreshes on route changes via the Navbar's effect.
  const response = await apiClient.get<{ count: number }>('/notifications/unread-count')
  return response.data.count
}

export async function markRead(id: string): Promise<NotificationDto> {
  const response = await apiClient.put<NotificationDto>(`/notifications/${id}/read`)
  return response.data
}
