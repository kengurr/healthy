import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginScreen } from './features/auth/LoginScreen';
import { DashboardScreen } from './features/dashboard/DashboardScreen';
import { UsersListScreen } from './features/users/UsersListScreen';
import { ProvidersListScreen } from './features/providers/ProvidersListScreen';
import { BookingsListScreen } from './features/bookings/BookingsListScreen';
import { BookingDetailScreen } from './features/bookings/BookingDetailScreen';
import { ServicesScreen } from './features/services/ServicesScreen';
import { EscalationsListScreen } from './features/escalations/EscalationsListScreen';
import { AppLayout } from './components/layout/AppLayout';
import { getAccessToken } from '@zdravdom/api-client';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  if (!getAccessToken()) {
    return <Navigate to="/login" replace />;
  }
  return <AppLayout>{children}</AppLayout>;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/users"
        element={
          <ProtectedRoute>
            <UsersListScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/providers"
        element={
          <ProtectedRoute>
            <ProvidersListScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings/:id"
        element={
          <ProtectedRoute>
            <BookingDetailScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings"
        element={
          <ProtectedRoute>
            <BookingsListScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/services"
        element={
          <ProtectedRoute>
            <ServicesScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/escalations"
        element={
          <ProtectedRoute>
            <EscalationsListScreen />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;