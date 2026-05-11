import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { MuscleBadge, Pill } from '../components/ui/Badge'
import { ExercisePickerModal } from '../components/workout/ExercisePickerModal'
import { getSplitById } from '../api/splits'
import {
  addExerciseToTemplate,
  createTemplate,
  deleteTemplate,
  removeExerciseFromTemplate,
} from '../api/templates'
import { useQuery } from '../hooks/useQuery'
import type { ExerciseDto, WorkoutTemplateDto } from '../types/workout'
import { extractErrorMessage } from '../utils/errors'
import { maxLength, rangeNumber, required } from '../utils/validation'

export function SplitDetailPage() {
  const { id } = useParams<{ id: string }>()
  const splitId = Number(id)
  const validId = Number.isFinite(splitId)

  const { data: split, isLoading, error: loadError, reload } = useQuery(
    () => getSplitById(splitId),
    [splitId],
    'Failed to load split.',
  )

  const [addTemplateOpen, setAddTemplateOpen] = useState(false)

  if (!validId) {
    return (
      <div className="space-y-4">
        <ErrorBanner message="Invalid split id." />
        <Link to="/splits" className="text-blue-600 hover:underline text-sm">← Back to splits</Link>
      </div>
    )
  }

  if (isLoading && !split) return <LoadingSpinner />
  if (loadError && !split) {
    return (
      <div className="space-y-4">
        <ErrorBanner message={loadError} />
        <Link to="/splits" className="text-blue-600 hover:underline text-sm">← Back to splits</Link>
      </div>
    )
  }
  if (!split) return null

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/splits" className="text-xs text-gray-500 hover:underline">← Splits</Link>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            {split.name}
            {split.isActive && <Pill className="bg-green-100 text-green-700">Active</Pill>}
          </h1>
        </div>
        <Button onClick={() => setAddTemplateOpen(true)}>+ New template</Button>
      </div>

      <ErrorBanner message={loadError} />

      {split.templates.length === 0 && (
        <EmptyState
          title="No templates yet"
          description="A template is a single workout day (e.g. Push, Pull, Legs)."
          action={<Button onClick={() => setAddTemplateOpen(true)}>+ Add template</Button>}
        />
      )}

      <div className="space-y-4">
        {[...split.templates]
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map(template => (
            <TemplateCard
              key={template.id}
              splitId={splitId}
              template={template}
              onChanged={reload}
            />
          ))}
      </div>

      <AddTemplateModal
        open={addTemplateOpen}
        onClose={() => setAddTemplateOpen(false)}
        splitId={splitId}
        existingCount={split.templates.length}
        onCreated={reload}
      />
    </div>
  )
}

function TemplateCard({
  splitId,
  template,
  onChanged,
}: {
  splitId: number
  template: WorkoutTemplateDto
  onChanged: () => void
}) {
  const [pickerOpen, setPickerOpen] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isAdding, setIsAdding] = useState(false)

  async function handleDeleteTemplate() {
    if (!window.confirm(`Delete template "${template.name}"?`)) return
    try {
      await deleteTemplate(splitId, template.id)
      onChanged()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to delete template.'))
    }
  }

  async function handlePickExercise(exercise: ExerciseDto) {
    setIsAdding(true)
    try {
      await addExerciseToTemplate(splitId, template.id, {
        exerciseId: exercise.id,
        targetSets: null,
        targetReps: null,
        orderIndex: template.exercises.length,
      })
      setPickerOpen(false)
      onChanged()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to add exercise.'))
    } finally {
      setIsAdding(false)
    }
  }

  async function handleRemoveExercise(exerciseTemplateId: number, name: string) {
    if (!window.confirm(`Remove "${name}" from this template?`)) return
    try {
      await removeExerciseFromTemplate(splitId, template.id, exerciseTemplateId)
      onChanged()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to remove exercise.'))
    }
  }

  return (
    <section className="bg-white rounded-2xl shadow p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-gray-800">{template.name}</h2>
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="secondary"
            disabled={isAdding}
            onClick={() => setPickerOpen(true)}
          >
            + Add exercise
          </Button>
          <Button size="sm" variant="danger" onClick={handleDeleteTemplate}>
            Delete
          </Button>
        </div>
      </div>

      <ErrorBanner message={actionError} />

      {template.exercises.length === 0 ? (
        <p className="text-sm text-gray-500">No exercises yet.</p>
      ) : (
        <ul className="divide-y divide-gray-100">
          {[...template.exercises]
            .sort((a, b) => a.orderIndex - b.orderIndex)
            .map(ex => (
              <li key={ex.id} className="flex items-center justify-between py-2">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-gray-800">{ex.exerciseName}</span>
                  <MuscleBadge group={ex.muscleGroup} />
                  {ex.targetSets != null && ex.targetReps != null && (
                    <span className="text-xs text-gray-500">
                      {ex.targetSets} × {ex.targetReps}
                    </span>
                  )}
                </div>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleRemoveExercise(ex.id, ex.exerciseName)}
                >
                  Remove
                </Button>
              </li>
            ))}
        </ul>
      )}

      <ExercisePickerModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        onPick={handlePickExercise}
        excludeIds={template.exercises.map(e => e.exerciseId)}
      />
    </section>
  )
}

function AddTemplateModal({
  open,
  onClose,
  splitId,
  existingCount,
  onCreated,
}: {
  open: boolean
  onClose: () => void
  splitId: number
  existingCount: number
  onCreated: () => void
}) {
  const [name, setName] = useState('')
  const [orderIndex, setOrderIndex] = useState(existingCount)
  const [nameError, setNameError] = useState<string | null>(null)
  const [orderError, setOrderError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (open) {
      setName('')
      setOrderIndex(existingCount)
      setNameError(null)
      setOrderError(null)
      setSubmitError(null)
    }
  }, [open, existingCount])

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const nErr = required(name, 'Name') ?? maxLength(name, 50, 'Name')
    const oErr = rangeNumber(orderIndex, 0, 100, 'Order')
    setNameError(nErr)
    setOrderError(oErr)
    if (nErr || oErr) return
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      await createTemplate(splitId, { name: name.trim(), orderIndex })
      onCreated()
      onClose()
    } catch (err) {
      setSubmitError(extractErrorMessage(err, 'Failed to create template.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New template">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          id="template-name"
          label="Name"
          placeholder="e.g. Push"
          value={name}
          onChange={e => setName(e.target.value)}
          error={nameError}
          autoFocus
        />
        <Input
          id="template-order"
          label="Order"
          type="number"
          min={0}
          value={orderIndex}
          onChange={e => setOrderIndex(Number(e.target.value))}
          error={orderError}
        />
        <ErrorBanner message={submitError} />
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Create'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
