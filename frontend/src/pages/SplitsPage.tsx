import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { LoadingSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { Pill } from '../components/ui/Badge'
import { activateSplit, createSplit, deleteSplit, listSplits } from '../api/splits'
import { useQuery } from '../hooks/useQuery'
import { extractErrorMessage, extractFieldErrors } from '../utils/errors'
import { required, maxLength } from '../utils/validation'

export function SplitsPage() {
  const { data, isLoading, error: loadError, reload } = useQuery(
    () => listSplits(),
    [],
    'Failed to load splits.',
  )
  const splits = data ?? []
  const [actionError, setActionError] = useState<string | null>(null)

  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [nameError, setNameError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const err = required(name, 'Name') ?? maxLength(name, 50, 'Name')
    setNameError(err)
    if (err) return
    setSubmitError(null)
    setIsSubmitting(true)
    try {
      await createSplit({ name: name.trim() })
      setName('')
      setCreateOpen(false)
      reload()
    } catch (err2) {
      const fields = extractFieldErrors(err2)
      if (fields?.name != null) setNameError(fields.name)
      setSubmitError(extractErrorMessage(err2, 'Failed to create split.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleActivate(id: number, splitName: string) {
    const currentlyActive = splits.find(s => s.isActive)
    const message = currentlyActive
      ? `Switch active split from "${currentlyActive.name}" to "${splitName}"?`
      : `Activate split "${splitName}"?`
    if (!window.confirm(message)) return
    setActionError(null)
    try {
      await activateSplit(id)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to activate split.'))
    }
  }

  async function handleDelete(id: number, splitName: string) {
    if (!window.confirm(`Delete split "${splitName}"? Its templates will be removed.`)) return
    setActionError(null)
    try {
      await deleteSplit(id)
      reload()
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to delete split.'))
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">My splits</h1>
          <p className="text-sm text-gray-500">One split is active at a time. Templates live inside splits.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>+ New split</Button>
      </div>

      <ErrorBanner message={loadError ?? actionError} />

      {isLoading && <LoadingSpinner />}

      {!isLoading && splits.length === 0 && (
        <EmptyState
          title="No splits yet"
          description="Create your first split to start logging workouts."
          action={<Button onClick={() => setCreateOpen(true)}>+ Create split</Button>}
        />
      )}

      {!isLoading && splits.length > 0 && (
        <ul className="grid sm:grid-cols-2 gap-4">
          {splits.map(split => (
            <li key={split.id} className="bg-white rounded-2xl shadow p-4">
              <div className="flex items-start justify-between gap-2">
                <Link to={`/splits/${split.id}`} className="text-lg font-semibold text-gray-800 hover:text-blue-700">
                  {split.name}
                </Link>
                {split.isActive && <Pill className="bg-green-100 text-green-700">Active</Pill>}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                {split.templates.length} template{split.templates.length === 1 ? '' : 's'}
              </p>
              <div className="flex gap-2 mt-4">
                {!split.isActive && (
                  <Button size="sm" onClick={() => handleActivate(split.id, split.name)}>
                    Activate
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => handleDelete(split.id, split.name)}
                >
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Create split">
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            id="split-name"
            label="Name"
            placeholder="e.g. PPL"
            value={name}
            onChange={e => setName(e.target.value)}
            error={nameError}
            autoFocus
          />
          <ErrorBanner message={submitError} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
