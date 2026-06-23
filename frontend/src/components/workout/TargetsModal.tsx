import { useEffect, useState, type FormEvent } from 'react'
import { Modal } from '../ui/Modal'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import { MuscleBadge } from '../ui/Badge'
import type { ExerciseDto } from '../../types/workout'
import { rangeNumber } from '../../utils/validation'

interface Props {
  open: boolean
  exercise: ExerciseDto | null
  onCancel: () => void
  // null = user left the field blank
  onConfirm: (targets: { targetSets: number | null; targetReps: number | null }) => Promise<void>
  submitError?: string | null
}

// Two-step add: ExercisePickerModal selects the exercise, then this modal collects
// optional sets/reps targets before POSTing. Keeps the picker simple and prevents
// the "targets only settable on add, never editable" UI surprise from being entirely
// out of reach.
export function TargetsModal({ open, exercise, onCancel, onConfirm, submitError }: Props) {
  const [sets, setSets] = useState('')
  const [reps, setReps] = useState('')
  const [errors, setErrors] = useState<{ sets?: string; reps?: string }>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (open) {
      setSets('')
      setReps('')
      setErrors({})
    }
  }, [open])

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const next: typeof errors = {}
    const setsNum = sets === '' ? null : Number(sets)
    const repsNum = reps === '' ? null : Number(reps)
    // Backend fields are Integer; "2.5" would 400 at deserialization time with no
    // bean-validation field map, so client must reject fractional values up front.
    if (setsNum != null) {
      if (!Number.isInteger(setsNum)) next.sets = 'Target sets must be a whole number'
      else {
        const err = rangeNumber(setsNum, 1, 20, 'Target sets')
        if (err) next.sets = err
      }
    }
    if (repsNum != null) {
      if (!Number.isInteger(repsNum)) next.reps = 'Target reps must be a whole number'
      else {
        const err = rangeNumber(repsNum, 1, 100, 'Target reps')
        if (err) next.reps = err
      }
    }
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setIsSubmitting(true)
    try {
      await onConfirm({ targetSets: setsNum, targetReps: repsNum })
    } finally {
      setIsSubmitting(false)
    }
  }

  // Suppress Escape/overlay-click while the POST is in flight — otherwise the user
  // can "cancel" mid-submit, the request still completes, and the exercise appears
  // in the template a moment later. Cancel button is disabled by the prop below.
  function handleClose() {
    if (!isSubmitting) onCancel()
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add to template">
      <form onSubmit={handleSubmit} className="space-y-4">
        {exercise && (
          <div className="bg-gray-50 rounded-lg p-3 flex items-center gap-2 text-sm">
            <span className="font-medium text-gray-800">{exercise.name}</span>
            <MuscleBadge group={exercise.muscleGroup} />
          </div>
        )}

        <p className="text-xs text-gray-500">
          Targets are optional — leave blank to add without them.
        </p>

        <div className="grid grid-cols-2 gap-3">
          <Input
            id="target-sets"
            label="Target sets"
            type="number"
            step={1}
            min={1}
            max={20}
            placeholder="3"
            value={sets}
            onChange={e => setSets(e.target.value)}
            error={errors.sets}
          />
          <Input
            id="target-reps"
            label="Target reps"
            type="number"
            step={1}
            min={1}
            max={100}
            placeholder="8"
            value={reps}
            onChange={e => setReps(e.target.value)}
            error={errors.reps}
          />
        </div>

        <ErrorBanner message={submitError ?? null} />

        <div className="flex justify-end gap-2 pt-2">
          {/* Cancel is disabled mid-submit; otherwise the user could close the
              modal while the POST is in flight and end up with a "cancelled"
              exercise that appears in the template a moment later. */}
          <Button
            type="button"
            variant="secondary"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Adding…' : 'Add exercise'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
