import { useState, type FormEvent } from 'react'
import { Button } from '../components/ui/Button'
import { Input, Select, TextArea } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { MuscleBadge } from '../components/ui/Badge'
import { createExercise, deleteExercise, listExercises, updateExercise } from '../api/exercises'
import { useDebounce } from '../hooks/useDebounce'
import { useQuery } from '../hooks/useQuery'
import type { CreateExerciseRequest, ExerciseDto, MuscleGroup } from '../types/workout'
import { MUSCLE_GROUPS } from '../types/workout'
import { extractErrorMessage, extractFieldErrors } from '../utils/errors'
import { maxLength, required } from '../utils/validation'

const PAGE_SIZE = 12

export function AdminExercisesPage() {
  const [search, setSearch] = useState('')
  const [muscleFilter, setMuscleFilter] = useState<MuscleGroup | ''>('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebounce(search, 300)
  const { data, isLoading, error: loadError, reload } = useQuery(
    () => listExercises({
      page,
      size: PAGE_SIZE,
      sort: 'name,asc',
      name: debouncedSearch || undefined,
      muscleGroup: muscleFilter || undefined,
    }),
    [page, debouncedSearch, muscleFilter],
    'Failed to load exercises.',
  )
  const [actionError, setActionError] = useState<string | null>(null)

  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [initial, setInitial] = useState<CreateExerciseRequest | null>(null)

  function startCreate() {
    setEditingId(null)
    setInitial({ name: '', description: '', muscleGroup: 'CHEST', imageUrl: '' })
    setFormOpen(true)
  }

  function startEdit(ex: ExerciseDto) {
    setEditingId(ex.id)
    setInitial({
      name: ex.name,
      description: ex.description ?? '',
      muscleGroup: ex.muscleGroup,
      imageUrl: ex.imageUrl ?? '',
    })
    setFormOpen(true)
  }

  async function handleDelete(ex: ExerciseDto) {
    if (!window.confirm(`Delete "${ex.name}"? This affects all templates that use it.`)) return
    setActionError(null)
    try {
      await deleteExercise(ex.id)
      // If we just deleted the only item on a non-first page, step back so the
      // user lands on a page with content instead of an empty page with a Prev
      // button they need to click manually. Step-back triggers useQuery refetch
      // via the deps change; otherwise reload() fetches the current page.
      if (data && data.content.length === 1 && page > 0) {
        setPage(p => p - 1)
      } else {
        reload()
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to delete exercise.'))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Admin · Exercises</h1>
          <p className="text-sm text-gray-500">Manage the global exercise catalog.</p>
        </div>
        <Button onClick={startCreate}>+ New exercise</Button>
      </div>

      <div className="bg-white rounded-2xl shadow p-4 grid sm:grid-cols-2 gap-3">
        <Input
          id="admin-search"
          label="Search"
          placeholder="e.g. bench press"
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(0) }}
        />
        <Select
          id="admin-muscle"
          label="Muscle group"
          value={muscleFilter}
          onChange={e => { setMuscleFilter(e.target.value as MuscleGroup | ''); setPage(0) }}
        >
          <option value="">All</option>
          {MUSCLE_GROUPS.map(g => (
            <option key={g} value={g}>{g}</option>
          ))}
        </Select>
      </div>

      <ErrorBanner message={loadError ?? actionError} />
      {isLoading && !data && <LoadingSpinner />}

      {!isLoading && data && data.content.length === 0 && (
        <EmptyState
          title="No exercises found"
          description={debouncedSearch || muscleFilter
            ? 'Try a different search or filter.'
            : 'Add the first exercise to the catalog.'}
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <div className="bg-white rounded-2xl shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
              <tr>
                <th className="text-left px-4 py-2">Name</th>
                <th className="text-left px-4 py-2">Muscle group</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {data.content.map(ex => (
                <tr key={ex.id}>
                  <td className="px-4 py-2 font-medium text-gray-800">{ex.name}</td>
                  <td className="px-4 py-2"><MuscleBadge group={ex.muscleGroup} /></td>
                  <td className="px-4 py-2 text-right space-x-1">
                    <Button size="sm" variant="ghost" onClick={() => startEdit(ex)}>Edit</Button>
                    <Button size="sm" variant="danger" onClick={() => handleDelete(ex)}>Delete</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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

      {formOpen && initial && (
        <ExerciseFormModal
          open={formOpen}
          onClose={() => setFormOpen(false)}
          initial={initial}
          editingId={editingId}
          onSaved={() => { setFormOpen(false); reload() }}
        />
      )}
    </div>
  )
}

function ExerciseFormModal({
  open,
  onClose,
  initial,
  editingId,
  onSaved,
}: {
  open: boolean
  onClose: () => void
  initial: CreateExerciseRequest
  editingId: number | null
  onSaved: () => void
}) {
  const [name, setName] = useState(initial.name)
  const [description, setDescription] = useState(initial.description ?? '')
  const [muscleGroup, setMuscleGroup] = useState<MuscleGroup>(initial.muscleGroup)
  const [imageUrl, setImageUrl] = useState(initial.imageUrl ?? '')
  const [errors, setErrors] = useState<Partial<Record<'name' | 'description' | 'muscleGroup' | 'imageUrl', string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const next: typeof errors = {}
    const nErr = required(name, 'Name') ?? maxLength(name, 100, 'Name')
    if (nErr) next.name = nErr
    const dErr = maxLength(description, 1000, 'Description')
    if (dErr) next.description = dErr
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const payload: CreateExerciseRequest = {
        name: name.trim(),
        description: description.trim() || null,
        muscleGroup,
        imageUrl: imageUrl.trim() || null,
      }
      if (editingId == null) {
        await createExercise(payload)
      } else {
        await updateExercise(editingId, payload)
      }
      onSaved()
    } catch (err) {
      const fields = extractFieldErrors(err)
      if (fields) {
        const mapped: typeof errors = {}
        for (const [k, v] of Object.entries(fields)) {
          if (k === 'name' || k === 'description' || k === 'muscleGroup' || k === 'imageUrl') {
            mapped[k] = v
          }
        }
        setErrors(mapped)
      }
      setSubmitError(extractErrorMessage(err, 'Failed to save exercise.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={editingId == null ? 'New exercise' : 'Edit exercise'}>
      <form onSubmit={handleSubmit} className="space-y-3">
        <Input
          id="ex-name"
          label="Name"
          value={name}
          onChange={e => setName(e.target.value)}
          error={errors.name}
          autoFocus
        />
        <Select
          id="ex-muscle"
          label="Muscle group"
          value={muscleGroup}
          onChange={e => setMuscleGroup(e.target.value as MuscleGroup)}
          error={errors.muscleGroup}
        >
          {MUSCLE_GROUPS.map(g => (
            <option key={g} value={g}>{g}</option>
          ))}
        </Select>
        <TextArea
          id="ex-desc"
          label="Description"
          rows={3}
          maxLength={1000}
          value={description}
          onChange={e => setDescription(e.target.value)}
          error={errors.description}
        />
        <Input
          id="ex-img"
          label="Image URL"
          value={imageUrl}
          onChange={e => setImageUrl(e.target.value)}
          error={errors.imageUrl}
        />

        <ErrorBanner message={submitError} />

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
