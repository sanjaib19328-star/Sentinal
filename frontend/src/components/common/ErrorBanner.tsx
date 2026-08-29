import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
  title?: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
  message,
  onRetry,
  title = 'Error',
}) => {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        padding: '1rem 1.25rem',
        backgroundColor: 'var(--danger-light)',
        border: '1px solid var(--danger-border)',
        borderRadius: 'var(--radius-md)',
        marginBottom: '1.25rem',
        color: 'var(--danger-text)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
        <AlertTriangle style={{ width: '1.25rem', height: '1.25rem', color: 'var(--danger)', flexShrink: 0, marginTop: '0.125rem' }} />
        <div>
          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--danger-text)', marginBottom: '0.125rem' }}>
            {title}
          </h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--danger-text)' }}>
            {message}
          </p>
        </div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="btn btn-secondary btn-sm"
          style={{
            borderColor: 'var(--danger-border)',
            color: 'var(--danger-text)',
            backgroundColor: '#ffffff',
            marginLeft: '1rem',
          }}
        >
          <RefreshCw style={{ width: '0.875rem', height: '0.875rem' }} />
          Retry
        </button>
      )}
    </div>
  );
};
