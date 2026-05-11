import { useState, type FormEvent } from 'react'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import { addSet } from '../../api/setLogs'
import { extractErrorMessage } from '../../utils/errors'
import { rangeNumber } from '../../utils/validation'

interface Props {
  logId: number
  exerciseLogId: number
  nextSetNumber: number
  onAdded: () => void
}

export function AddSetForm({ logId, exerciseLogId, nextSetNumber, onAdded }: Props) {
  const [weight, setWeight] = useState('')
  const [reps, setReps] = useState('')
  const [rpe, setRpe] = useState('')
  const [errors, setErrors] = useState<{ weight?: string; reps?: string; rpe?: string }>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const w = Number(weight)
    const r = Number(reps)
    const rp = rpe === '' ? null : Number(rpe)
    const next: typeof errors = {}
    if (weight === '' || Number.isNaN(w) || w < 0) next.weight = 'Weight must be ≥ 0'
    if (reps === '') next.reps = 'Reps required'
    else {
      const e2 = rangeNumber(r, 1, 100, 'Reps')
      if (e2) next.reps = e2
    }
    if (rp != null) {
      const e3 = rangeNumber(rp, 1, 10, 'RPE')
      if (e3) next.rpe = e3
    }
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSubmitError(null)
    setIsSubmitting(true)
    try {
      await addSet(logId, exerciseLogId, {
        setNumber: nextSetNumber,
        weightKg: w,
        reps: r,
        rpe: rp,
      })
      setWeight('')
      setReps('')
      setRpe('')
      onAdded()
    } catch (err) {
      setSubmitError(extractErrorMessage(err, 'Failed to add set.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-2 pt-2 border-t border-gray-100">
      <span className="text-xs text-gray-400 mb-2">#{nextSetNumber}</span>
      <div className="w-24">
        <label className="block text-xs text-gray-500">Weight (kg)</label>
        <input
          type="number"
          step="0.5"
          min={0}
          value={weight}
          onChange={e => setWeight(e.target.value)}
          placeholder="80"
          className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        />
        {errors.weight && <p className="text-xs text-red-600">{errors.weight}</p>}
      </div>
      <div className="w-20">
        <label className="block text-xs text-gray-500">Reps</label>
        <input
          type="number"
          min={1}
          max={100}
          value={reps}
          onChange={e => setReps(e.target.value)}
          placeholder="8"
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
          placeholder="—"
          className="w-full rounded border border-gray-300 px-2 py-1 text-sm"
        />
        {errors.rpe && <p className="text-xs text-red-600">{errors.rpe}</p>}
      </div>
      <Button size="sm" type="submit" disabled={isSubmitting} className="mb-0.5">
        {isSubmitting ? 'Adding…' : '+ Add set'}
      </Button>
      <div className="basis-full">
        <ErrorBanner message={submitError} />
      </div>
    </form>
  )
}
