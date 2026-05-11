import { useEffect, useState } from 'react'
import { Input, Select } from '../components/ui/Input'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { MuscleBadge } from '../components/ui/Badge'
import { useDebounce } from '../hooks/useDebounce'
import { useQuery } from '../hooks/useQuery'
import { listExercises } from '../api/exercises'
import { type MuscleGroup, MUSCLE_GROUPS } from '../types/workout'

const PAGE_SIZE = 12

export function ExercisesPage() {
  const [search, setSearch] = useState('')
  const [muscleGroup, setMuscleGroup] = useState<MuscleGroup | ''>('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebounce(search, 300)

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { setPage(0) }, [debouncedSearch, muscleGroup])

  const { data, isLoading, error } = useQuery(
    () => listExercises({
      page,
      size: PAGE_SIZE,
      sort: 'name,asc',
      name: debouncedSearch || undefined,
      muscleGroup: muscleGroup || undefined,
    }),
    [page, debouncedSearch, muscleGroup],
    'Failed to load exercises.',
  )

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Exercise catalog</h1>
        <p className="text-sm text-gray-500">Browse the full set of exercises.</p>
      </div>

      <div className="bg-white rounded-2xl shadow p-4 grid sm:grid-cols-2 gap-3">
        <Input
          id="search"
          label="Search"
          placeholder="e.g. bench press"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <Select
          id="muscleGroup"
          label="Muscle group"
          value={muscleGroup}
          onChange={e => setMuscleGroup(e.target.value as MuscleGroup | '')}
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
        <EmptyState title="No exercises found" description="Try a different search or filter." />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {data.content.map(ex => (
            <div key={ex.id} className="bg-white rounded-2xl shadow p-4">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold text-gray-800">{ex.name}</h3>
                <MuscleBadge group={ex.muscleGroup} />
              </div>
              {ex.description && (
                <p className="text-sm text-gray-600 mt-2 line-clamp-3">{ex.description}</p>
              )}
            </div>
          ))}
        </div>
      )}

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
