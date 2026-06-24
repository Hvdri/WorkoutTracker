import { useEffect, useState } from 'react'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { TextArea } from '../ui/Input'
import { ErrorBanner } from '../ui/ErrorBanner'
import { LoadingSpinner } from '../ui/LoadingSpinner'
import { EmptyState } from '../ui/EmptyState'
import { listLogs } from '../../api/workoutLogs'
import { createPost } from '../../api/posts'
import { extractErrorMessage } from '../../utils/errors'
import type { WorkoutLogDto } from '../../types/workout'

interface Props {
  open: boolean
  onClose: () => void
  onShared: () => void
}

// Pulls the user's own logs and filters client-side to those that are COMPLETED.
// We don't filter out already-shared logs here — the backend returns 409 with a
// clear message ("WorkoutLog has already been shared as a post") and we surface
// that inline. The trade-off is one extra round-trip per duplicate attempt, which
// is fine: it keeps the modal stateless and avoids a second list-endpoint just
// for "shareable" logs.
const MAX_LOGS = 50

export function ShareWorkoutModal({ open, onClose, onShared }: Props) {
  const [logs, setLogs] = useState<WorkoutLogDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [selectedLogId, setSelectedLogId] = useState<number | null>(null)
  const [caption, setCaption] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Reset modal state on open so reopening after a successful share starts clean.
  useEffect(() => {
    if (!open) return
    setSelectedLogId(null)
    setCaption('')
    setSubmitError(null)
    setLoadError(null)
    setIsLoading(true)

    let cancelled = false
    listLogs({ page: 0, size: MAX_LOGS, sort: 'date,desc' })
      .then(page => {
        if (cancelled) return
        setLogs(page.content.filter(l => l.status === 'COMPLETED'))
      })
      .catch(err => {
        if (!cancelled) setLoadError(extractErrorMessage(err, 'Failed to load your workouts.'))
      })
      .finally(() => { if (!cancelled) setIsLoading(false) })

    return () => { cancelled = true }
  }, [open])

  async function handleSubmit() {
    if (selectedLogId == null) return
    setIsSubmitting(true)
    setSubmitError(null)
    try {
      await createPost({
        workoutLogId: selectedLogId,
        caption: caption.trim() || null,
      })
      onShared()
      onClose()
    } catch (err) {
      setSubmitError(extractErrorMessage(err, 'Failed to share workout.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Share a workout" widthClass="max-w-lg">
      <ErrorBanner message={loadError} />
      <ErrorBanner message={submitError} />

      {isLoading && <LoadingSpinner />}

      {!isLoading && !loadError && logs.length === 0 && (
        <EmptyState
          title="No completed workouts to share"
          description="Mark a workout as complete first, then come back to share it."
        />
      )}

      {!isLoading && logs.length > 0 && (
        <div className="space-y-3">
          <p className="text-sm text-gray-600">Pick a completed workout:</p>
          <div className="max-h-64 overflow-y-auto -mx-2 px-2 divide-y divide-gray-100 border border-gray-100 rounded-lg">
            {logs.map(log => (
              <label
                key={log.id}
                className={`flex items-center gap-3 py-2 px-2 cursor-pointer rounded ${
                  selectedLogId === log.id ? 'bg-blue-50' : 'hover:bg-gray-50'
                }`}
              >
                <input
                  type="radio"
                  name="share-log"
                  value={log.id}
                  checked={selectedLogId === log.id}
                  onChange={() => setSelectedLogId(log.id)}
                  className="text-blue-600"
                />
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-gray-800 truncate">
                    {log.templateName}
                  </div>
                  <div className="text-xs text-gray-500">{log.date}</div>
                </div>
              </label>
            ))}
          </div>

          <TextArea
            id="share-caption"
            label="Caption (optional)"
            value={caption}
            onChange={e => setCaption(e.target.value)}
            maxLength={300}
            rows={3}
            placeholder="How did it go?"
          />
          <p className="text-xs text-gray-400 text-right">{caption.length}/300</p>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={selectedLogId == null || isSubmitting}
            >
              {isSubmitting ? 'Sharing…' : 'Share'}
            </Button>
          </div>
        </div>
      )}

      {!isLoading && logs.length === 0 && (
        <div className="flex justify-end pt-2">
          <Button variant="secondary" onClick={onClose}>Close</Button>
        </div>
      )}
    </Modal>
  )
}
