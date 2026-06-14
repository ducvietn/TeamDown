import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AppProvider, useApp } from './context/AppContext.jsx'
import Layout from './components/layout/Layout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import TaskWorkspace from './pages/TaskWorkspace.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import LeaderReviewPage from './pages/LeaderReviewPage.jsx'
import PeerReviewPage from './pages/PeerReviewPage.jsx'
import NotificationPage from './pages/NotificationPage.jsx'
import ReportPage from './pages/ReportPage.jsx'

function ProtectedRoute({ children, requireLeader = false, requireTeacher = false }) {
  const { currentUser, isLeader, isTeacher } = useApp()
  if (!currentUser) return <Navigate to="/login" replace />
  if (requireLeader && !isLeader()) return <Navigate to="/" replace />
  if (requireTeacher && !isTeacher()) return <Navigate to="/" replace />
  return children
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<Layout />}>
        <Route path="/" element={<TaskWorkspace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/my-reviews" element={<PeerReviewPage />} />
        <Route path="/notifications" element={<NotificationPage />} />

        <Route
          path="/leader-review"
          element={
            <ProtectedRoute requireLeader>
              <LeaderReviewPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports"
          element={
            <ProtectedRoute requireTeacher>
              <ReportPage />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <AppRoutes />
      </AppProvider>
    </BrowserRouter>
  )
}
