import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Button } from '../components/ui/Button'
import { Input, Select, TextArea } from '../components/ui/Input'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { MuscleBadge } from '../components/ui/Badge'
import { getActiveSplit } from '../api/splits'
import { createLog, deleteLog } from '../api/workoutLogs'
import { addExerciseLog } from '../api/exerciseLogs'
import type { WorkoutSplitDto, WorkoutTemplateDto } from '../types/workout'
import { extractErrorMessage } from '../utils/errors'
import { maxLength, notFutureDate, todayIso } from '../utils/validation'

export function NewWorkoutPage() {
  const navigate = useNavigate()

  const [split, setSplit] = useState<WorkoutSplitDto | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [templateId, setTemplateId] = useState<number | ''>('')
  const [date, setDate] = useState(todayIso())
  const [photoUrl, setPhotoUrl] = useState('')
  const [notes, setNotes] = useState('')

  const [errors, setErrors] = useState<{ templateId?: string; date?: string; notes?: string }>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    getActiveSplit()
      .then(s => {
        if (cancelled) return
        setSplit(s)
        if (s && s.templates.length > 0) {
          const sorted = [...s.templates].sort((a, b) => a.orderIndex - b.orderIndex)
          setTemplateId(sorted[0].id)
        }
        setLoadError(null)
      })
      .catch(err => {
        if (!cancelled) setLoadError(extractErrorMessage(err, 'Failed to load active split.'))
      })
      .finally(() => { if (!cancelled) setIsLoading(false) })
    return () => { cancelled = true }
  }, [])

  const selectedTemplate: WorkoutTemplateDto | undefined =
    split?.templates.find(t => t.id === templateId)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const nextErrors: typeof errors = {}
    if (!templateId) nextErrors.templateId = 'Pick a template'
    const dateErr = notFutureDate(date)
    if (dateErr) nextErrors.date = dateErr
    const notesErr = maxLength(notes, 500, 'Notes')
    if (notesErr) nextErrors.notes = notesErr
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setSubmitError(null)
    setIsSubmitting(true)

    let createdLogId: number | null = null
    try {
      const log = await createLog({
        date,
        templateId: Number(templateId),
        photoUrl: photoUrl.trim() || null,
        notes: notes.trim() || null,
      })
      createdLogId = log.id

      // Pre-populate exercise logs from the template, in order.
      // Sequential to keep ordering stable; the lists are short (typically <10).
      if (selectedTemplate) {
        const exercises = [...selectedTemplate.exercises].sort(
          (a, b) => a.orderIndex - b.orderIndex,
        )
        for (const ex of exercises) {
          await addExerciseLog(log.id, { exerciseId: ex.exerciseId, notes: null })
        }
      }

      navigate(`/logs/${log.id}`)
    } catch (err) {
      // Roll back the partially-created log so the user doesn't end up with an empty
      // entry in their history. If rollback fails (network gone, etc.), navigate to
      // the detail page anyway so they can finish or delete it manually instead of
      // leaving an orphan that can't be reached.
      if (createdLogId != null) {
        try {
          await deleteLog(createdLogId)
        } catch {
          navigate(`/logs/${createdLogId}`)
          return
        }
      }
      setSubmitError(extractErrorMessage(err, 'Failed to start workout.'))
      setIsSubmitting(false)
    }
  }

  if (isLoading) return <LoadingSpinner />

  if (loadError) return <ErrorBanner message={loadError} />

  if (!split) {
    return (
      <EmptyState
        title="No active split"
        description="Activate a split before logging a workout."
        action={<Link to="/splits"><Button>Go to splits</Button></Link>}
      />
    )
  }

  if (split.templates.length === 0) {
    return (
      <EmptyState
        title="No templates in your active split"
        description={`Add at least one template to "${split.name}" before logging a workout.`}
        action={<Link to={`/splits/${split.id}`}><Button>Open split</Button></Link>}
      />
    )
  }

  const sortedTemplates = [...split.templates].sort((a, b) => a.orderIndex - b.orderIndex)

  return (
    <div className="max-w-2xl space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Log a workout</h1>
        <p className="text-sm text-gray-500">
          Active split: <span className="font-medium text-gray-700">{split.name}</span>
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow p-4 space-y-4">
        <Select
          id="templateId"
          label="Template"
          value={templateId}
          onChange={e => setTemplateId(e.target.value ? Number(e.target.value) : '')}
          error={errors.templateId}
        >
          {sortedTemplates.map(t => (
            <option key={t.id} value={t.id}>{t.name}</option>
          ))}
        </Select>

        {selectedTemplate && selectedTemplate.exercises.length > 0 && (
          <div className="bg-gray-50 rounded-lg p-3 space-y-2">
            <p className="text-xs uppercase tracking-wide text-gray-500">Pre-filled exercises</p>
            <ul className="space-y-1">
              {[...selectedTemplate.exercises]
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map(ex => (
                  <li key={ex.id} className="flex items-center gap-2 text-sm">
                    <MuscleBadge group={ex.muscleGroup} />
                    <span className="text-gray-800">{ex.exerciseName}</span>
                    {ex.targetSets != null && ex.targetReps != null && (
                      <span className="text-xs text-gray-500">
                        target {ex.targetSets} × {ex.targetReps}
                      </span>
                    )}
                  </li>
                ))}
            </ul>
          </div>
        )}

        <Input
          id="date"
          label="Date"
          type="date"
          value={date}
          max={todayIso()}
          onChange={e => setDate(e.target.value)}
          error={errors.date}
        />

        <Input
          id="photoUrl"
          label="Photo URL (optional)"
          placeholder="https://…"
          value={photoUrl}
          onChange={e => setPhotoUrl(e.target.value)}
        />

        <TextArea
          id="notes"
          label="Notes (optional)"
          rows={3}
          maxLength={500}
          value={notes}
          onChange={e => setNotes(e.target.value)}
          error={errors.notes}
        />

        <ErrorBanner message={submitError} />

        <div className="flex justify-end gap-2 pt-2">
          <Link to="/"><Button type="button" variant="secondary">Cancel</Button></Link>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Starting…' : 'Start workout'}
          </Button>
        </div>
      </form>
    </div>
  )
}
