import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Button } from '../components/ui/Button'
import { Input, TextArea } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { MuscleBadge, StatusBadge } from '../components/ui/Badge'
import { ExercisePickerModal } from '../components/workout/ExercisePickerModal'
import { SetLogRow } from '../components/workout/SetLogRow'
import { AddSetForm } from '../components/workout/AddSetForm'
import { completeLog, deleteLog, getLog, updateLog } from '../api/workoutLogs'
import { addExerciseLog, deleteExerciseLog } from '../api/exerciseLogs'
import { createPost } from '../api/posts'
import { useQuery } from '../hooks/useQuery'
import type { ExerciseDto, WorkoutLogDto } from '../types/workout'
import { extractErrorMessage, extractFieldErrors } from '../utils/errors'
import { maxLength } from '../utils/validation'

export function LogDetailPage() {
  const { id } = useParams<{ id: string }>()
  const logId = Number(id)
  const validId = Number.isFinite(logId)
  const navigate = useNavigate()

  const { data: log, isLoading, error: loadError, reload } = useQuery(
    () => getLog(logId),
    [logId],
    'Failed to load workout.',
  )
  const [actionError, setActionError] = useState<string | null>(null)

  const [pickerOpen, setPickerOpen] = useState(false)
  const [editingMeta, setEditingMeta] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  // Local "already shared in this session" flag. Backend rule forbids duplicate posts
  // per log, so once the modal POSTs successfully we hide the Share button to prevent
  // a follow-up click that would 409. Resets on full page reload (limitation: the
  // WorkoutLogDto doesn't carry a `postId`, so we can't make this durable).
  const [postCreated, setPostCreated] = useState(false)

  async function handleAddExercise(exercise: ExerciseDto) {
    setActionError(null)
    try {
      await addExerciseLog(logId, { exerciseId: exercise.id, notes: null })
      setPickerOpen(false)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to add exercise.'))
    }
  }

  async function handleRemoveExercise(exerciseLogId: number, name: string) {
    if (!window.confirm(`Remove "${name}" and all its sets?`)) return
    try {
      await deleteExerciseLog(logId, exerciseLogId)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to remove exercise.'))
    }
  }

  async function handleComplete() {
    if (!window.confirm('Mark this workout as completed?')) return
    setActionError(null)
    try {
      await completeLog(logId)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to complete workout.'))
    }
  }

  async function handleDelete() {
    if (!window.confirm('Delete this workout log permanently?')) return
    try {
      await deleteLog(logId)
      navigate('/history')
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to delete workout.'))
    }
  }

  if (!validId) {
    return (
      <div className="space-y-3">
        <ErrorBanner message="Invalid log id." />
        <Link to="/history" className="text-blue-600 hover:underline text-sm">← Back to history</Link>
      </div>
    )
  }
  if (isLoading && !log) return <LoadingSpinner />
  if (loadError && !log) {
    return (
      <div className="space-y-3">
        <ErrorBanner message={loadError} />
        <Link to="/history" className="text-blue-600 hover:underline text-sm">← Back to history</Link>
      </div>
    )
  }
  if (!log) return null

  const isCompleted = log.status === 'COMPLETED'
  // ExerciseLog has no orderIndex; sort by id (creation order, monotonic) to keep
  // the list stable across reloads.
  const exerciseLogs = [...log.exerciseLogs].sort((a, b) => a.id - b.id)

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-2 flex-wrap">
        <div>
          <Link to="/history" className="text-xs text-gray-500 hover:underline">← History</Link>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2 flex-wrap">
            {log.templateName}
            <StatusBadge status={log.status} />
          </h1>
          <p className="text-sm text-gray-500 mt-1">{log.date}</p>
        </div>
        <div className="flex gap-2 flex-wrap items-center">
          {/* Top-level actions are disabled while the meta editor is open: clicking
              Mark complete or Delete would unmount MetaSection and silently throw
              away whatever the user just typed. Force them to Save or Cancel first. */}
          {!isCompleted && (
            <Button
              variant="primary"
              onClick={handleComplete}
              disabled={editingMeta}
              title={editingMeta ? 'Save or cancel your notes first' : undefined}
            >
              Mark complete
            </Button>
          )}
          {isCompleted && !postCreated && (
            <Button variant="primary" onClick={() => setShareOpen(true)}>
              Share as post
            </Button>
          )}
          {isCompleted && postCreated && (
            <span className="inline-flex items-center rounded-full bg-green-100 text-green-700 px-3 py-1 text-xs font-medium">
              Posted ✓
            </span>
          )}
          <Button
            variant="danger"
            onClick={handleDelete}
            disabled={editingMeta}
            title={editingMeta ? 'Save or cancel your notes first' : undefined}
          >
            Delete
          </Button>
        </div>
      </div>

      <ErrorBanner message={actionError} />

      <MetaSection
        log={log}
        editing={editingMeta && !isCompleted}
        onEdit={() => setEditingMeta(true)}
        onClose={() => setEditingMeta(false)}
        onSaved={reload}
      />

      <div className="space-y-4">
        {exerciseLogs.length === 0 && (
          <div className="bg-white rounded-2xl shadow p-4 text-sm text-gray-500">
            No exercises in this workout yet.
          </div>
        )}

        {exerciseLogs.map(exLog => (
          <section key={exLog.id} className="bg-white rounded-2xl shadow p-4 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <h3 className="font-semibold text-gray-800">{exLog.exerciseName}</h3>
                <MuscleBadge group={exLog.muscleGroup} />
              </div>
              {!isCompleted && (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleRemoveExercise(exLog.id, exLog.exerciseName)}
                >
                  Remove
                </Button>
              )}
            </div>

            {exLog.sets.length === 0 && !isCompleted && (
              <p className="text-xs text-gray-500">No sets yet — add your first below.</p>
            )}

            <div className="space-y-1">
              {[...exLog.sets]
                .sort((a, b) => a.setNumber - b.setNumber)
                .map(s => (
                  <SetLogRow
                    key={s.id}
                    logId={logId}
                    exerciseLogId={exLog.id}
                    set={s}
                    readOnly={isCompleted}
                    onChanged={reload}
                  />
                ))}
            </div>

            {!isCompleted && (
              <AddSetForm
                logId={logId}
                exerciseLogId={exLog.id}
                nextSetNumber={
                  exLog.sets.reduce((max, s) => Math.max(max, s.setNumber), 0) + 1
                }
                onAdded={reload}
              />
            )}
          </section>
        ))}
      </div>

      {!isCompleted && (
        <div>
          <Button variant="secondary" onClick={() => setPickerOpen(true)}>
            + Add exercise
          </Button>
        </div>
      )}

      <ExercisePickerModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        onPick={handleAddExercise}
        excludeIds={log.exerciseLogs.map(el => el.exerciseId)}
      />

      <ShareAsPostModal
        open={shareOpen}
        onClose={() => setShareOpen(false)}
        onPosted={() => setPostCreated(true)}
        logId={logId}
        templateName={log.templateName}
      />
    </div>
  )
}

function MetaSection({
  log,
  editing,
  onEdit,
  onClose,
  onSaved,
}: {
  log: WorkoutLogDto
  editing: boolean
  onEdit: () => void
  onClose: () => void
  onSaved: () => void
}) {
  const isCompleted = log.status === 'COMPLETED'
  const [photoUrl, setPhotoUrl] = useState(log.photoUrl ?? '')
  const [notes, setNotes] = useState(log.notes ?? '')
  const [error, setError] = useState<string | null>(null)
  const [notesError, setNotesError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Seed local state on Edit click only — not via a useEffect on [log] — so that
  // a mid-edit reload (e.g. user clicks "Mark complete" elsewhere on the page,
  // triggering a refetch of `log`) doesn't wipe what the user just typed.
  function startEdit() {
    setPhotoUrl(log.photoUrl ?? '')
    setNotes(log.notes ?? '')
    setError(null)
    setNotesError(null)
    onEdit()
  }

  async function handleSave(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const nErr = maxLength(notes, 500, 'Notes')
    setNotesError(nErr)
    if (nErr) return
    setError(null)
    setIsSubmitting(true)
    try {
      await updateLog(log.id, {
        photoUrl: photoUrl.trim() || null,
        notes: notes.trim() || null,
      })
      onClose()
      onSaved()
    } catch (err) {
      const fields = extractFieldErrors(err)
      if (fields?.notes != null) setNotesError(fields.notes)
      setError(extractErrorMessage(err, 'Failed to save changes.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // The parent already passes editing={editingMeta && !isCompleted}, and B2's fix
  // disables Mark complete while editingMeta is true, so isCompleted can't flip
  // mid-edit. The `editing` prop alone is sufficient here.
  if (editing) {
    return (
      <form onSubmit={handleSave} className="bg-white rounded-2xl shadow p-4 space-y-3">
        <Input
          id="photoUrl"
          label="Photo URL"
          value={photoUrl}
          onChange={e => setPhotoUrl(e.target.value)}
          placeholder="https://…"
        />
        <TextArea
          id="notes"
          label="Notes"
          rows={3}
          maxLength={500}
          value={notes}
          onChange={e => setNotes(e.target.value)}
          error={notesError}
        />
        <ErrorBanner message={error} />
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : 'Save'}
          </Button>
        </div>
      </form>
    )
  }

  if (!log.photoUrl && !log.notes && isCompleted) return null

  return (
    <div className="bg-white rounded-2xl shadow p-4 space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-xs uppercase tracking-wide text-gray-400">Notes & photo</span>
        {!isCompleted && (
          <Button size="sm" variant="ghost" onClick={startEdit}>Edit</Button>
        )}
      </div>
      {log.notes ? (
        <p className="text-sm text-gray-700 whitespace-pre-wrap">{log.notes}</p>
      ) : (
        <p className="text-sm text-gray-400">No notes</p>
      )}
      {log.photoUrl && (
        <a href={log.photoUrl} target="_blank" rel="noreferrer" className="block">
          <img src={log.photoUrl} alt="" className="mt-2 rounded-lg max-h-72 object-cover" />
        </a>
      )}
    </div>
  )
}

function ShareAsPostModal({
  open,
  onClose,
  onPosted,
  logId,
  templateName,
}: {
  open: boolean
  onClose: () => void
  onPosted: () => void
  logId: number
  templateName: string
}) {
  const [caption, setCaption] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [captionError, setCaptionError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (open) {
      setCaption('')
      setError(null)
      setCaptionError(null)
    }
  }, [open])

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const cErr = maxLength(caption, 300, 'Caption')
    setCaptionError(cErr)
    if (cErr) return
    setError(null)
    setIsSubmitting(true)
    try {
      await createPost({ workoutLogId: logId, caption: caption.trim() || null })
      onPosted()
      onClose()
    } catch (err) {
      const fields = extractFieldErrors(err)
      if (fields?.caption != null) setCaptionError(fields.caption)
      setError(extractErrorMessage(err, 'Failed to create post.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={`Share "${templateName}"`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <TextArea
          id="caption"
          label="Caption (optional)"
          rows={3}
          maxLength={300}
          value={caption}
          onChange={e => setCaption(e.target.value)}
          error={captionError}
        />
        <ErrorBanner message={error} />
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Sharing…' : 'Share'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
