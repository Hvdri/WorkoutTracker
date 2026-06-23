export function LoadingSpinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center py-10 text-sm text-gray-500" role="status">
      {label}
    </div>
  )
}
