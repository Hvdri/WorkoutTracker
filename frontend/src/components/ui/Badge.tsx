import type { ReactNode } from 'react'
import type { MuscleGroup, WorkoutStatus } from '../../types/workout'

const MUSCLE_COLOR: Record<MuscleGroup, string> = {
  CHEST: 'bg-rose-100 text-rose-700',
  BACK: 'bg-indigo-100 text-indigo-700',
  SHOULDERS: 'bg-amber-100 text-amber-800',
  BICEPS: 'bg-purple-100 text-purple-700',
  TRICEPS: 'bg-fuchsia-100 text-fuchsia-700',
  LEGS: 'bg-emerald-100 text-emerald-700',
  GLUTES: 'bg-pink-100 text-pink-700',
  CORE: 'bg-yellow-100 text-yellow-800',
  CARDIO: 'bg-sky-100 text-sky-700',
}

export function MuscleBadge({ group }: { group: MuscleGroup }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${MUSCLE_COLOR[group]}`}>
      {group}
    </span>
  )
}

export function StatusBadge({ status }: { status: WorkoutStatus }) {
  const cls =
    status === 'COMPLETED'
      ? 'bg-green-100 text-green-700'
      : 'bg-yellow-100 text-yellow-800'
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}>
      {status === 'COMPLETED' ? 'Completed' : 'In progress'}
    </span>
  )
}

export function Pill({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-700 ${className}`}>
      {children}
    </span>
  )
}
