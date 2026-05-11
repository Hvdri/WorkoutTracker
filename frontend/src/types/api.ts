export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  timestamp: string
  errors?: Record<string, string>
}
