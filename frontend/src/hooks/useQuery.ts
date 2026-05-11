import { useEffect, useState } from 'react'
import { extractErrorMessage } from '../utils/errors'

interface QueryResult<T> {
  data: T | null
  isLoading: boolean
  error: string | null
  reload: () => void
}

// Tiny data-fetching hook. Refetches whenever any dep changes; `reload()` forces
// a refetch for mutations. Stale data stays visible until the new response lands;
// `isLoading` stays true until the first successful response so callers can show
// a spinner on initial load AND on every refetch (including dep-change and reload).
//
// Why not React Query? This is a university project — one tiny hook is enough.
// `setIsLoading(true)` runs synchronously inside the effect, which the new
// `react-hooks/set-state-in-effect` rule flags. We accept the extra render here
// in exchange for a working "loading on refetch" UX; the alternatives
// (useTransition, useDeferredValue, isFetching/isLoading split) all add complexity
// that isn't worth it at this scale.
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
    // `fn` is intentionally excluded — callers pass inline arrows so its identity
    // changes every render; deps array is the explicit refetch trigger.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadKey])

  return {
    data,
    isLoading,
    error,
    reload: () => setReloadKey(k => k + 1),
  }
}
