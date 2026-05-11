import axios from 'axios'
import type { ErrorResponse } from '../types/api'

export function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    if (!err.response) {
      return 'Could not reach the server. Check your connection.'
    }
    const data = err.response.data as ErrorResponse | undefined
    return data?.message ?? fallback
  }
  return fallback
}

// Returns the per-field error map from a Spring `MethodArgumentNotValidException` response,
// e.g. {"username": "may only contain ..."}. Returns null when the server didn't include one.
export function extractFieldErrors(err: unknown): Record<string, string> | null {
  if (!axios.isAxiosError(err) || !err.response) return null
  const data = err.response.data as ErrorResponse | undefined
  return data?.errors ?? null
}

export function extractStatus(err: unknown): number | null {
  if (axios.isAxiosError(err) && err.response) return err.response.status
  return null
}
