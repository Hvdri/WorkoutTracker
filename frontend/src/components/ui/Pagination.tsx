import { Button } from './Button'

interface Props {
  page: number          // zero-based, matches backend
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: Props) {
  // Always render the strip so users see "1 of 1 · N total" instead of wondering
  // whether more pages are hidden. Empty results read as "No results" — saying
  // "Page 1 of 1 · 0 total" is confusing.
  const summary =
    totalElements === 0
      ? 'No results'
      : `Page ${page + 1} of ${Math.max(totalPages, 1)} · ${totalElements} total`
  return (
    <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
      <span>{summary}</span>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          size="sm"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          ← Prev
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Next →
        </Button>
      </div>
    </div>
  )
}
