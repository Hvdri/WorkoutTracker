import { createBrowserRouter, RouterProvider } from 'react-router'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExercisesPage } from './pages/ExercisesPage'
import { SplitsPage } from './pages/SplitsPage'
import { SplitDetailPage } from './pages/SplitDetailPage'
import { ProfilePage } from './pages/ProfilePage'
import { NewWorkoutPage } from './pages/NewWorkoutPage'
import { LogDetailPage } from './pages/LogDetailPage'
import { HistoryPage } from './pages/HistoryPage'
import { FeedPage } from './pages/FeedPage'
import { UserProfilePage } from './pages/UserProfilePage'
import { AdminExercisesPage } from './pages/AdminExercisesPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ErrorPage } from './pages/ErrorPage'

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage />, errorElement: <ErrorPage /> },
  { path: '/register', element: <RegisterPage />, errorElement: <ErrorPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'exercises', element: <ExercisesPage /> },
      { path: 'splits', element: <SplitsPage /> },
      { path: 'splits/:id', element: <SplitDetailPage /> },
      { path: 'profile/me', element: <ProfilePage /> },
      { path: 'log/new', element: <NewWorkoutPage /> },
      { path: 'logs/:id', element: <LogDetailPage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'feed', element: <FeedPage /> },
      { path: 'users/:id', element: <UserProfilePage /> },
      {
        path: 'admin/exercises',
        element: (
          <AdminRoute>
            <AdminExercisesPage />
          </AdminRoute>
        ),
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
}

export default App
