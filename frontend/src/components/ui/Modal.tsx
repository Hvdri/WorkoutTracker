import { useEffect, useId, useRef, type ReactNode } from 'react'

interface Props {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  widthClass?: string
}

const FOCUSABLE =
  'button:not([disabled]),[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])'

export function Modal({ open, onClose, title, children, widthClass = 'max-w-md' }: Props) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const titleId = useId()
  // Hold onClose in a ref so the setup effect can depend on [open] only. Callers
  // pass inline arrows, so onClose identity changes every parent render — if it
  // were in the deps, the effect cleanup would run on each render and steal focus
  // back to the trigger element while the dialog is open. Bouncing focus while
  // the user is typing is a real bug, not theoretical.
  const onCloseRef = useRef(onClose)
  useEffect(() => { onCloseRef.current = onClose }, [onClose])

  useEffect(() => {
    if (!open) return

    // Remember where focus was before opening so we can restore it on close.
    const previouslyFocused = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current

    function focusables(): HTMLElement[] {
      if (!dialog) return []
      return Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE))
    }

    focusables()[0]?.focus()

    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onCloseRef.current()
        return
      }
      if (e.key !== 'Tab') return
      // Trap Tab / Shift+Tab inside the dialog so keyboard users can't drift to
      // the background page.
      const f = focusables()
      if (f.length === 0) return
      const first = f[0]
      const last = f[f.length - 1]
      const active = document.activeElement
      if (e.shiftKey && active === first) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && active === last) {
        e.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKey)

    // Lock background scroll while the modal is open so the page underneath
    // doesn't scroll when the user spins the wheel inside (or outside) the dialog.
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = previousOverflow
      previouslyFocused?.focus()
    }
  }, [open])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
      onClick={() => onCloseRef.current()}
      role="dialog"
      aria-modal="true"
      aria-labelledby={title ? titleId : undefined}
    >
      <div
        ref={dialogRef}
        className={`bg-white rounded-2xl shadow-lg p-6 w-full ${widthClass}`}
        onClick={e => e.stopPropagation()}
      >
        {title && (
          <h2 id={titleId} className="text-lg font-semibold text-gray-800 mb-4">
            {title}
          </h2>
        )}
        {children}
      </div>
    </div>
  )
}
