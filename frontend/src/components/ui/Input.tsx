import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string | null
}

export function Input({ label, error, id, className = '', ...rest }: InputProps) {
  const borderClass = error ? 'border-red-400 focus:ring-red-400' : 'border-gray-300 focus:ring-blue-500'
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <input
        id={id}
        {...rest}
        className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 ${borderClass} ${className}`}
      />
      {error && <p role="alert" className="text-xs text-red-600">{error}</p>}
    </div>
  )
}

interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string | null
}

export function TextArea({ label, error, id, className = '', ...rest }: TextAreaProps) {
  const borderClass = error ? 'border-red-400 focus:ring-red-400' : 'border-gray-300 focus:ring-blue-500'
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <textarea
        id={id}
        {...rest}
        className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 ${borderClass} ${className}`}
      />
      {error && <p role="alert" className="text-xs text-red-600">{error}</p>}
    </div>
  )
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string | null
  children: ReactNode
}

export function Select({ label, error, id, children, className = '', ...rest }: SelectProps) {
  const borderClass = error ? 'border-red-400 focus:ring-red-400' : 'border-gray-300 focus:ring-blue-500'
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <select
        id={id}
        {...rest}
        className={`w-full rounded-lg border px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 ${borderClass} ${className}`}
      >
        {children}
      </select>
      {error && <p role="alert" className="text-xs text-red-600">{error}</p>}
    </div>
  )
}
