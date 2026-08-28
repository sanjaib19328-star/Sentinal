import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';

import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Applications } from './pages/Applications';
import { ApplicationDetails } from './pages/ApplicationDetails';
import { ApplicationKeys } from './pages/ApplicationKeys';
import { GlobalApiCatalog } from './pages/GlobalApiCatalog';
import { RequestExplorer } from './pages/RequestExplorer';
import { AiAssistant } from './pages/AiAssistant';
import { Profile } from './pages/Profile';
import { NotFound } from './pages/NotFound';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Authentication Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Observability Platform Routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="applications" element={<Applications />} />
            <Route path="applications/:id" element={<ApplicationDetails />} />
            <Route path="applications/:id/keys" element={<ApplicationKeys />} />
            <Route path="apis" element={<GlobalApiCatalog />} />
            <Route path="assistant" element={<AiAssistant />} />
            <Route path="requests" element={<RequestExplorer />} />
            <Route path="profile" element={<Profile />} />
            <Route path="analytics" element={<Navigate to="/dashboard" replace />} />
            <Route path="settings" element={<Profile />} />
            <Route path="alerts" element={<Navigate to="/dashboard" replace />} />
          </Route>

          {/* Source files defense: Never navigate to source files */}
          <Route path="src/*" element={<Navigate to="/dashboard" replace />} />

          {/* 404 Catch-All */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
