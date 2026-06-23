import { useState, type FormEvent } from 'react'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import type { SetLogDto } from '../../types/workout'
import { deleteSet, updateSet } from '../../api/setLogs'
import { extractErrorMessage, extractFieldErrors } from '../../utils/errors'
import { rangeNumber } from '../../utils/validation'

interface Props {
  logId: number
  exerciseLogId: number
  set: SetLogDto
  readOnly?: boolean
  onChanged: () => void
}

// Form-state keys match the backend DTO field names (weightKg, reps, rpe) so the
// server's per-field error map can be merged with a single spread — no name-by-name
// translation that future-you would need to revisit if a DTO field is renamed.
interface FieldErrors {
  weightKg?: string
  reps?: string
  rpe?: string
}

export function SetLogRow({ logId, exerciseLogId, set, readOnly = false, onChanged }: Props) {
  const [editing, setEditing] = useState(false)
  const [weightKg, setWeightKg] = useState(String(set.weightKg))
  const [reps, setReps] = useState(String(set.reps))
  const [rpe, setRpe] = useState(set.rpe == null ? '' : String(set.rpe))
  const [errors, setErrors] = useState<FieldErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const isBusy = isSubmitting || isDeleting

  function reset() {
    setWeightKg(String(set.weightKg))
    setReps(String(set.reps))
    setRpe(set.rpe == null ? '' : String(set.rpe))
    setErrors({})
    setSubmitError(null)
  }

  async function handleSave(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const w = Number(weightKg)
    const r = Number(reps)
    const rp = rpe === '' ? null : Number(rpe)
    const next: FieldErrors = {}
    if (Number.isNaN(w) || w < 0) next.weightKg = 'Weight must be ≥ 0'
    const repsErr = rangeNumber(r, 1, 100, 'Reps')
    if (repsErr) next.reps = repsErr
    if (rp != null) {
      const e2 = rangeNumber(rp, 1, 10, 'RPE')
      if (e2) next.rpe = e2
    }
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      await updateSet(logId, exerciseLogId, set.id, { weightKg: w, reps: r, rpe: rp })
      setEditing(false)
      onChanged()
    } catch (err) {
      const fields = extractFieldErrors(err)
      if (fields) {
        // Keys match backend → single spread, no manual mapping. Use != null so an
        // empty-string error message (rare but legal) is still applied.
        const merged: FieldErrors = { ...errors }
        if (fields.weightKg != null) merged.weightKg = fields.weightKg
        if (fields.reps != null) merged.reps = fields.reps
        if (fields.rpe != null) merged.rpe = fields.rpe
        setErrors(merged)
      }
      setSubmitError(extractErrorMessage(err, 'Failed to save set.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDelete() {
    if (isBusy) return
    if (!window.confirm(`Delete set ${set.setNumber}?`)) return
    setIsDeleting(true)
    try {
      await deleteSet(logId, exerciseLogId, set.id)
      onChanged()
    } catch (err) {
      setSubmitError(extractErrorMessage(err, 'Failed to delete set.'))
    } finally {
      setIsDeleting(false)
    }
  }

  if (!editing) {
    return (
      <div className="flex items-center gap-2 text-sm py-1">
        <span className="text-xs text-gray-400 w-6">#{set.setNumber}</span>
        <span className="font-medium text-gray-800">{set.weightKg} kg</span>
        <span className="text-gray-500">×</span>
        <span className="font-medium text-gray-800">{set.reps}</span>
        {set.rpe != null && (
          <span className="text-xs text-gray-500">RPE {set.rpe}</span>
        )}
        {!readOnly && (
          <div className="ml-auto flex gap-1">
            <Button
              size="sm"
              variant="ghost"
              disabled={isBusy}
              onClick={() => { reset(); setEditing(true) }}
            >
              Edit
            </Button>
            <Button size="sm" variant="ghost" disabled={isBusy} onClick={handleDelete}>
              {isDeleting ? 'Deleting…' : 'Delete'}
            </Button>
          </div>
        )}
      </div>
    )
  }

  return (
    <form onSubmit={handleSave} className="flex flex-wrap items-end gap-2 py-2 border-t border-gray-100">
      <span className="text-xs text-gray-400 mb-2">#{set.setNumber}</span>
      <div className="w-24">
        <label className="block text-xs text-gray-500">Weight (kg)</label>
        <input
          type="number"
          step="0.5"
          min={0}
          value={weightKg}
          onChange={e => setWeightKg(e.target.value)}
          className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        />
        {errors.weightKg && <p className="text-xs text-red-600">{errors.weightKg}</p>}
      </div>
      <div className="w-20">
        <label className="block text-xs text-gray-500">Reps</label>
        <input
          type="number"
          min={1}
          max={100}
          value={reps}
          onChange={e => setReps(e.target.value)}
          className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        />
        {errors.reps && <p className="text-xs text-red-600">{errors.reps}</p>}
      </div>
      <div className="w-20">
        <label className="block text-xs text-gray-500">RPE</label>
        <input
          type="number"
          min={1}
          max={10}
          value={rpe}
          onChange={e => setRpe(e.target.value)}
          className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        />
        {errors.rpe && <p className="text-xs text-red-600">{errors.rpe}</p>}
      </div>
      <div className="flex gap-1 ml-auto mb-0.5">
        <Button size="sm" type="submit" disabled={isSubmitting}>Save</Button>
        <Button size="sm" type="button" variant="secondary" onClick={() => { reset(); setEditing(false) }}>
          Cancel
        </Button>
      </div>
      <div className="basis-full">
        <ErrorBanner message={submitError} />
      </div>
    </form>
  )
}
