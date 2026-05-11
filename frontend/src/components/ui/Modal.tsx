import { useEffect, type ReactNode } from 'react'

interface Props {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  widthClass?: string
}

export function Modal({ open, onClose, title, children, widthClass = 'max-w-md' }: Props) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    // Lock background scroll while the modal is open so the page underneath
    // doesn't scroll when the user spins the wheel inside (or outside) the dialog.
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = previousOverflow
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className={`bg-white rounded-2xl shadow-lg p-6 w-full ${widthClass}`}
        onClick={e => e.stopPropagation()}
      >
        {title && <h2 className="text-lg font-semibold text-gray-800 mb-4">{title}</h2>}
        {children}
      </div>
    </div>
  )
}
