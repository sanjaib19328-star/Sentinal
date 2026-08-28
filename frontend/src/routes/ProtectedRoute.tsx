import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <LoadingSpinner message="Authenticating session..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    const isSourcePath = location.pathname.startsWith('/src') || location.pathname.includes('.');
    const rawPath = `${location.pathname}${location.search}${location.hash}`;
    const targetPath = (isSourcePath || location.pathname === '/') ? '/dashboard' : rawPath;
    return <Navigate to={`/login?redirect=${encodeURIComponent(targetPath)}`} state={{ from: location }} replace />;
  }

  return <>{children}</>;
};
