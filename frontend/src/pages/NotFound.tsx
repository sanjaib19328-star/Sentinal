import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

export const NotFound: React.FC = () => {
  return (
    <div
      style={{
        minHeight: '70vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '2rem',
      }}
    >
      <div
        style={{
          width: '3.5rem',
          height: '3.5rem',
          borderRadius: '50%',
          backgroundColor: 'var(--danger-light)',
          color: 'var(--danger)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '1rem',
        }}
      >
        <ShieldAlert style={{ width: '2rem', height: '2rem' }} />
      </div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
        404 — Resource Not Found
      </h1>
      <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', maxWidth: '24rem', marginBottom: '1.5rem' }}>
        The page or application you requested does not exist or you do not have permission to view it.
      </p>
      <Link to="/dashboard" className="btn btn-primary btn-sm">
        <ArrowLeft style={{ width: '0.875rem', height: '0.875rem' }} />
        Return to Dashboard
      </Link>
    </div>
  );
};
