import { useEffect, useState } from 'react'
import { Modal } from '../ui/Modal'
import { Input, Select } from '../ui/Input'
import { Button } from '../ui/Button'
import { LoadingSpinner } from '../ui/LoadingSpinner'
import { EmptyState } from '../ui/EmptyState'
import { Pagination } from '../ui/Pagination'
import { ErrorBanner } from '../ui/ErrorBanner'
import { MuscleBadge } from '../ui/Badge'
import { useDebounce } from '../../hooks/useDebounce'
import { listExercises } from '../../api/exercises'
import type { ExerciseDto, MuscleGroup } from '../../types/workout'
import { MUSCLE_GROUPS } from '../../types/workout'
import type { PageResponse } from '../../types/api'
import { extractErrorMessage } from '../../utils/errors'

interface Props {
  open: boolean
  onClose: () => void
  onPick: (exercise: ExerciseDto) => void
  excludeIds?: number[]
}

const PAGE_SIZE = 8

export function ExercisePickerModal({ open, onClose, onPick, excludeIds = [] }: Props) {
  const [search, setSearch] = useState('')
  const [muscleGroup, setMuscleGroup] = useState<MuscleGroup | ''>('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<ExerciseDto> | null>(null)
  // Start in loading state so the spinner shows immediately when the modal opens.
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const debouncedSearch = useDebounce(search, 300)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    // Show the spinner on every refetch (open / page / filter change). The
    // react-hooks/set-state-in-effect rule wants us to avoid sync setState in effect
    // bodies; the alternatives (useTransition / isFetching split) are more complex
    // than the trade-off is worth here.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true)
    listExercises({
      page,
      size: PAGE_SIZE,
      sort: 'name,asc',
      name: debouncedSearch || undefined,
      muscleGroup: muscleGroup || undefined,
    })
      .then(res => {
        if (cancelled) return
        setData(res)
        setError(null)
      })
      .catch(err => {
        if (!cancelled) setError(extractErrorMessage(err, 'Failed to load exercises.'))
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
  }, [open, page, debouncedSearch, muscleGroup])

  return (
    <Modal open={open} onClose={onClose} title="Pick an exercise" widthClass="max-w-xl">
      <div className="space-y-3">
        <div className="grid sm:grid-cols-2 gap-3">
          <Input
            id="picker-search"
            label="Search"
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            placeholder="bench press"
          />
          <Select
            id="picker-muscle"
            label="Muscle group"
            value={muscleGroup}
            onChange={e => { setMuscleGroup(e.target.value as MuscleGroup | ''); setPage(0) }}
          >
            <option value="">All</option>
            {MUSCLE_GROUPS.map(g => (
              <option key={g} value={g}>{g}</option>
            ))}
          </Select>
        </div>

        <ErrorBanner message={error} />
        {isLoading && <LoadingSpinner />}

        {!isLoading && data && data.content.length === 0 && (
          <EmptyState title="No exercises found" />
        )}

        {!isLoading && data && data.content.length > 0 && (
          <ul className="divide-y divide-gray-100 border border-gray-100 rounded-lg">
            {data.content.map(ex => {
              const disabled = excludeIds.includes(ex.id)
              return (
                <li
                  key={ex.id}
                  className={`flex items-center justify-between px-3 py-2 ${disabled ? 'opacity-50' : ''}`}
                >
                  <div>
                    <div className="text-sm font-medium text-gray-800">{ex.name}</div>
                    <div className="mt-0.5"><MuscleBadge group={ex.muscleGroup} /></div>
                  </div>
                  <Button
                    size="sm"
                    variant={disabled ? 'secondary' : 'primary'}
                    disabled={disabled}
                    onClick={() => onPick(ex)}
                  >
                    {disabled ? 'Added' : 'Add'}
                  </Button>
                </li>
              )
            })}
          </ul>
        )}

        {data && (
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={setPage}
          />
        )}

        <div className="flex justify-end pt-2">
          <Button variant="secondary" onClick={onClose}>Close</Button>
        </div>
      </div>
    </Modal>
  )
}
