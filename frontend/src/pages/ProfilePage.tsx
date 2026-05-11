import { useEffect, useState, type FormEvent } from 'react'
import { Button } from '../components/ui/Button'
import { Input, Select, TextArea } from '../components/ui/Input'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { getMyProfile, updateMyProfile } from '../api/profile'
import { GENDERS, type Gender, type UserProfileDto } from '../types/profile'
import { extractErrorMessage, extractFieldErrors } from '../utils/errors'
import { maxLength, rangeNumber } from '../utils/validation'

interface FormState {
  bio: string
  fitnessGoal: string
  heightCm: string
  weightKg: string
  gender: Gender | ''
}

const EMPTY_FORM: FormState = {
  bio: '',
  fitnessGoal: '',
  heightCm: '',
  weightKg: '',
  gender: '',
}

function toForm(profile: UserProfileDto): FormState {
  return {
    bio: profile.bio ?? '',
    fitnessGoal: profile.fitnessGoal ?? '',
    heightCm: profile.heightCm == null ? '' : String(profile.heightCm),
    weightKg: profile.weightKg == null ? '' : String(profile.weightKg),
    gender: profile.gender ?? '',
  }
}

export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfileDto | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    setIsLoading(true)
    setLoadError(null)
    getMyProfile()
      .then(p => {
        setProfile(p)
        setForm(toForm(p))
      })
      .catch(err => setLoadError(extractErrorMessage(err, 'Failed to load profile.')))
      .finally(() => setIsLoading(false))
  }, [])

  function startEdit() {
    if (profile) setForm(toForm(profile))
    setErrors({})
    setSubmitError(null)
    setEditing(true)
  }

  function cancelEdit() {
    if (profile) setForm(toForm(profile))
    setErrors({})
    setSubmitError(null)
    setEditing(false)
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const nextErrors: Partial<Record<keyof FormState, string>> = {}
    const bioErr = maxLength(form.bio, 500, 'Bio')
    if (bioErr) nextErrors.bio = bioErr
    const goalErr = maxLength(form.fitnessGoal, 100, 'Fitness goal')
    if (goalErr) nextErrors.fitnessGoal = goalErr

    const heightNum = form.heightCm === '' ? null : Number(form.heightCm)
    const weightNum = form.weightKg === '' ? null : Number(form.weightKg)
    if (heightNum !== null) {
      const e = rangeNumber(heightNum, 50, 300, 'Height')
      if (e) nextErrors.heightCm = e
    }
    if (weightNum !== null) {
      const e = rangeNumber(weightNum, 20, 500, 'Weight')
      if (e) nextErrors.weightKg = e
    }

    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    // PUT semantics on the backend: every field is written through, null clears it.
    // Empty inputs are sent as null so the user can actually clear bio/goal/etc.
    const payload = {
      bio: form.bio.trim() === '' ? null : form.bio.trim(),
      fitnessGoal: form.fitnessGoal.trim() === '' ? null : form.fitnessGoal.trim(),
      heightCm: heightNum,
      weightKg: weightNum,
      gender: form.gender === '' ? null : form.gender,
    }

    setSubmitError(null)
    setIsSubmitting(true)
    try {
      const updated = await updateMyProfile(payload)
      setProfile(updated)
      setForm(toForm(updated))
      setEditing(false)
    } catch (err) {
      const fields = extractFieldErrors(err)
      if (fields) {
        const mapped: Partial<Record<keyof FormState, string>> = {}
        for (const [k, v] of Object.entries(fields)) {
          if (k in EMPTY_FORM) mapped[k as keyof FormState] = v
        }
        setErrors(mapped)
      }
      setSubmitError(extractErrorMessage(err, 'Failed to save profile.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) return <LoadingSpinner />
  if (loadError && !profile) {
    return <ErrorBanner message={loadError} />
  }
  if (!profile) return null

  return (
    <div className="max-w-2xl space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{profile.username}</h1>
          {profile.displayName && <p className="text-sm text-gray-500">{profile.displayName}</p>}
        </div>
        {!editing && <Button onClick={startEdit}>Edit</Button>}
      </div>

      {editing ? (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow p-4 space-y-4">
          <TextArea
            id="bio"
            label="Bio"
            rows={3}
            maxLength={500}
            value={form.bio}
            onChange={e => setForm(f => ({ ...f, bio: e.target.value }))}
            error={errors.bio}
          />
          <Input
            id="fitnessGoal"
            label="Fitness goal"
            value={form.fitnessGoal}
            onChange={e => setForm(f => ({ ...f, fitnessGoal: e.target.value }))}
            error={errors.fitnessGoal}
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              id="heightCm"
              label="Height (cm)"
              type="number"
              step="0.1"
              min={50}
              max={300}
              value={form.heightCm}
              onChange={e => setForm(f => ({ ...f, heightCm: e.target.value }))}
              error={errors.heightCm}
            />
            <Input
              id="weightKg"
              label="Weight (kg)"
              type="number"
              step="0.1"
              min={20}
              max={500}
              value={form.weightKg}
              onChange={e => setForm(f => ({ ...f, weightKg: e.target.value }))}
              error={errors.weightKg}
            />
          </div>
          <Select
            id="gender"
            label="Gender"
            value={form.gender}
            onChange={e => setForm(f => ({ ...f, gender: e.target.value as Gender | '' }))}
            error={errors.gender}
          >
            <option value="">— Not specified —</option>
            {GENDERS.map(g => (
              <option key={g} value={g}>{g.replace(/_/g, ' ')}</option>
            ))}
          </Select>

          <ErrorBanner message={submitError} />

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={cancelEdit}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Save'}
            </Button>
          </div>
        </form>
      ) : (
        <dl className="bg-white rounded-2xl shadow p-4 grid sm:grid-cols-2 gap-x-6 gap-y-3 text-sm">
          <Field label="Bio" value={profile.bio} multiline />
          <Field label="Fitness goal" value={profile.fitnessGoal} />
          <Field label="Height" value={profile.heightCm == null ? null : `${profile.heightCm} cm`} />
          <Field label="Weight" value={profile.weightKg == null ? null : `${profile.weightKg} kg`} />
          <Field label="Gender" value={profile.gender ? profile.gender.replace(/_/g, ' ') : null} />
        </dl>
      )}
    </div>
  )
}

function Field({ label, value, multiline = false }: { label: string; value: string | null; multiline?: boolean }) {
  return (
    <div className={multiline ? 'sm:col-span-2' : ''}>
      <dt className="text-xs uppercase tracking-wide text-gray-400">{label}</dt>
      <dd className="mt-0.5 text-gray-800 whitespace-pre-wrap">{value ?? <span className="text-gray-400">—</span>}</dd>
    </div>
  )
}
