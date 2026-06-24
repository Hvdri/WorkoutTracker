import { createBrowserRouter, RouterProvider } from 'react-router'
import { AuthProvider } from './context/AuthProvider'
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
import { DiscoveryPage } from './pages/DiscoveryPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { UserProfilePage } from './pages/UserProfilePage'
import { AdminExercisesPage } from './pages/AdminExercisesPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ErrorPage } from './pages/ErrorPage'

const router = createBrowserRouter([
  // /login and /register render standalone (no AppLayout); errors there fall through
  // to the router's default. ErrorPage is compact-by-design for the AppLayout chrome,
  // so we don't want it rendered without that chrome.
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      // Pathless wrapper so the errorElement renders inside <AppLayout>'s <Outlet/>
      // (the navbar stays visible on crashes). React Router replaces the entire
      // route subtree when an error fires, so attaching errorElement to a child
      // route — not to the layout route — is what keeps the chrome on screen.
      {
        errorElement: <ErrorPage />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'exercises', element: <ExercisesPage /> },
          { path: 'splits', element: <SplitsPage /> },
          { path: 'splits/:id', element: <SplitDetailPage /> },
          { path: 'profile/me', element: <ProfilePage /> },
          { path: 'logs/new', element: <NewWorkoutPage /> },
          { path: 'logs/:id', element: <LogDetailPage /> },
          { path: 'history', element: <HistoryPage /> },
          { path: 'feed', element: <FeedPage /> },
          { path: 'discover', element: <DiscoveryPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
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
