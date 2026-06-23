import { useCallback, useEffect, useState } from 'react'
import { extractErrorMessage } from '../utils/errors'

interface QueryResult<T> {
  data: T | null
  isLoading: boolean
  error: string | null
  reload: () => void
}

// Tiny data-fetching hook. Refetches whenever any dep changes; `reload()` forces
// a refetch for mutations. Stale data stays visible until the new response lands;
// `isLoading` starts true and transitions to false after the first response
// (success or error), so callers can show a spinner on initial load AND on every
// refetch.
//
// The `react-hooks/exhaustive-deps` disable below is intentional: `fn` and
// `fallbackError` are excluded because callers pass inline arrows whose identity
// changes every render. The explicit `deps` array is the contract — enumerate
// every value `fn` closes over so refetches actually fire when those values
// change. Same trade-off React Query / SWR make.
export function useQuery<T>(
  fn: () => Promise<T>,
  deps: unknown[],
  fallbackError = 'Failed to load.',
): QueryResult<T> {
  const [data, setData] = useState<T | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    fn()
      .then(d => {
        if (cancelled) return
        setData(d)
        setError(null)
      })
      .catch(err => {
        if (!cancelled) setError(extractErrorMessage(err, fallbackError))
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadKey])

  // Stable reload identity so callers can include it in their own deps without
  // triggering needless re-runs.
  const reload = useCallback(() => setReloadKey(k => k + 1), [])

  return { data, isLoading, error, reload }
}
