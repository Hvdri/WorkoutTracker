export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface AuthResponse {
  token: string
  username: string
  roles: string[]
}

export interface AuthUser {
  userId: number
  username: string
  roles: string[]
}
