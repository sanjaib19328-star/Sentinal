import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  message?: string;
  className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ message = 'Loading...', className = '' }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem 1rem',
        color: 'var(--text-muted)',
      }}
      className={className}
    >
      <Loader2
        style={{
          width: '2rem',
          height: '2rem',
          animation: 'spin 1s linear infinite',
          color: 'var(--primary)',
          marginBottom: '0.75rem',
        }}
      />
      <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>{message}</span>
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};
