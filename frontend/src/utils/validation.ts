// Shared client-side form validation helpers.
// All return `null` for valid input or an error message string for invalid input.

export function required(value: string | null | undefined, label = 'This field'): string | null {
  if (!value || !value.trim()) return `${label} is required`
  return null
}

export function maxLength(value: string | null | undefined, max: number, label = 'This field'): string | null {
  if (value && value.length > max) return `${label} must be at most ${max} characters`
  return null
}

export function rangeNumber(
  value: number | null | undefined,
  min: number,
  max: number,
  label = 'This field',
): string | null {
  if (value == null) return null
  if (Number.isNaN(value)) return `${label} must be a number`
  if (value < min || value > max) return `${label} must be between ${min} and ${max}`
  return null
}

// Validates a YYYY-MM-DD string is not after today's local date.
// Compares strings (lexicographic equals chronological for ISO dates) instead of
// Date objects — `new Date('2026-05-10')` is parsed as UTC midnight, so using
// it would mark today's date as "future" in any timezone east of UTC.
export function notFutureDate(value: string | null | undefined, label = 'Date'): string | null {
  if (!value) return `${label} is required`
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return `${label} is invalid`
  if (value > todayIso()) return `${label} cannot be in the future`
  return null
}

export function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}
