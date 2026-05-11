import { useEffect, useState } from 'react'

// Returns a value that lags `delayMs` behind the input.
// Used by search fields so we don't fire a request on every keystroke.
export function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const handle = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(handle)
  }, [value, delayMs])
  return debounced
}
