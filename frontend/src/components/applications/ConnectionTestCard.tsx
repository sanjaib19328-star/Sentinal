import React, { useState } from 'react';
import { Application, ConnectionTestResponse } from '../../types/application';
import { applicationsApi } from '../../api/applications';
import { StatusBadge } from '../common/StatusBadge';
import { getErrorMessage } from '../../api/client';
import { Activity, Globe, Clock, CheckCircle2, AlertTriangle, XCircle, HelpCircle, Loader2 } from 'lucide-react';

interface ConnectionTestCardProps {
  application: Application;
  onStatusUpdated?: (newStatus: ConnectionTestResponse) => void;
}

export const ConnectionTestCard: React.FC<ConnectionTestCardProps> = ({
  application,
  onStatusUpdated,
}) => {
  const [testing, setTesting] = useState(false);
  const [lastResult, setLastResult] = useState<ConnectionTestResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleTestConnection = async () => {
    setTesting(true);
    setError(null);
    try {
      const result = await applicationsApi.testConnection(application.id);
      setLastResult(result);
      if (onStatusUpdated) {
        onStatusUpdated(result);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setTesting(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return <CheckCircle2 style={{ width: '1.25rem', height: '1.25rem', color: 'var(--success)' }} />;
      case 'DEGRADED':
        return <AlertTriangle style={{ width: '1.25rem', height: '1.25rem', color: 'var(--warning)' }} />;
      case 'UNAVAILABLE':
        return <XCircle style={{ width: '1.25rem', height: '1.25rem', color: 'var(--danger)' }} />;
      case 'UNKNOWN':
      default:
        return <HelpCircle style={{ width: '1.25rem', height: '1.25rem', color: 'var(--neutral)' }} />;
    }
  };

  const currentStatus = lastResult ? lastResult.status : application.healthStatus;
  const currentLastSeen = lastResult ? lastResult.checkedAt : application.lastSeenAt;

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h3 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Activity style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
            Application Health & Observation
          </h3>
          <p className="card-subtitle">
            Non-blocking health probe and latency telemetry
          </p>
        </div>
        <StatusBadge status={currentStatus} />
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1rem',
          padding: '1rem',
          backgroundColor: 'var(--bg-subtle)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          marginBottom: '1.25rem',
        }}
      >
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
            Target URL
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.875rem', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
            <Globe style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)', flexShrink: 0 }} />
            <span>{application.baseUrl}</span>
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
            Last Observed
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            <Clock style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)', flexShrink: 0 }} />
            <span>
              {currentLastSeen
                ? new Date(currentLastSeen).toLocaleString()
                : 'No observation recorded yet (UNKNOWN)'}
            </span>
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
            Connection Mode
          </div>
          <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
            {application.connectionMode} (Fail-Safe)
          </div>
        </div>
      </div>

      {/* Result Display */}
      {lastResult && (
        <div
          style={{
            padding: '1rem',
            borderRadius: 'var(--radius-md)',
            backgroundColor: lastResult.reachable ? 'var(--success-light)' : 'var(--danger-light)',
            border: `1px solid ${lastResult.reachable ? 'var(--success-border)' : 'var(--danger-border)'}`,
            marginBottom: '1.25rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, fontSize: '0.875rem', color: lastResult.reachable ? 'var(--success-text)' : 'var(--danger-text)' }}>
              {getStatusIcon(lastResult.status)}
              <span>{lastResult.message}</span>
            </div>
            {lastResult.latencyMs !== null && (
              <span className="badge badge-healthy" style={{ fontSize: '0.75rem' }}>
                {lastResult.latencyMs} ms
              </span>
            )}
          </div>
          <div style={{ fontSize: '0.75rem', color: lastResult.reachable ? 'var(--success-text)' : 'var(--danger-text)' }}>
            Observed at: {new Date(lastResult.checkedAt).toLocaleTimeString()}
          </div>
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '0.75rem 1rem',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--danger-light)',
            border: '1px solid var(--danger-border)',
            color: 'var(--danger-text)',
            fontSize: '0.8125rem',
            marginBottom: '1.25rem',
          }}
        >
          {error}
        </div>
      )}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          Probes are executed without blocking runtime traffic on the target application.
        </p>
        <button
          onClick={handleTestConnection}
          disabled={testing}
          className="btn btn-primary btn-sm"
        >
          {testing ? (
            <>
              <Loader2 style={{ width: '0.875rem', height: '0.875rem', animation: 'spin 1s linear infinite' }} />
              Executing Probe...
            </>
          ) : (
            <>
              <Activity style={{ width: '0.875rem', height: '0.875rem' }} />
              Test Connection Now
            </>
          )}
        </button>
      </div>
    </div>
  );
};
