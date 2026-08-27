import React, { useEffect, useState, useCallback } from 'react';
import { ApiEndpoint, ApiEndpointAnalytics } from '../../types/application';
import { applicationsApi } from '../../api/applications';
import { Modal } from '../common/Modal';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorBanner } from '../common/ErrorBanner';
import { getErrorMessage } from '../../api/client';
import {
  Activity,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Zap,
  ShieldAlert,
  Calendar,
  Layers,
} from 'lucide-react';

interface ApiDetailsModalProps {
  isOpen: boolean;
  applicationId: number;
  endpoint: ApiEndpoint | null;
  onClose: () => void;
}

export const ApiDetailsModal: React.FC<ApiDetailsModalProps> = ({
  isOpen,
  applicationId,
  endpoint,
  onClose,
}) => {
  const [analytics, setAnalytics] = useState<ApiEndpointAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalytics = useCallback(async () => {
    if (!endpoint) return;
    setLoading(true);
    setError(null);
    try {
      const data = await applicationsApi.getApiAnalytics(applicationId, endpoint.id);
      setAnalytics(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [applicationId, endpoint]);

  useEffect(() => {
    if (isOpen && endpoint) {
      fetchAnalytics();
    } else {
      setAnalytics(null);
    }
  }, [isOpen, endpoint, fetchAnalytics]);

  if (!endpoint) return null;

  const getMethodBadgeClass = (method: string) => {
    switch (method.toUpperCase()) {
      case 'GET':
        return 'badge-healthy';
      case 'POST':
        return 'badge-primary';
      case 'PUT':
      case 'PATCH':
        return 'badge-degraded';
      case 'DELETE':
        return 'badge-unavailable';
      default:
        return 'badge-unknown';
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Endpoint Analytics: ${endpoint.method} ${endpoint.normalizedPath}`}
      maxWidth="780px"
    >
      {/* Header Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          padding: '0.75rem 1rem',
          backgroundColor: 'var(--bg-surface)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-md)',
          marginBottom: '1.25rem',
        }}
      >
        <span
          className={`badge ${getMethodBadgeClass(endpoint.method)}`}
          style={{ fontSize: '0.875rem', fontWeight: 700, padding: '0.25rem 0.625rem' }}
        >
          {endpoint.method}
        </span>
        <code style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
          {endpoint.normalizedPath}
        </code>
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchAnalytics} />}

      {loading ? (
        <LoadingSpinner message="Aggregating endpoint metrics and percentiles..." />
      ) : analytics ? (
        <div>
          {/* Key Metrics Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
              gap: '0.75rem',
              marginBottom: '1.25rem',
            }}
          >
            <div className="stat-card" style={{ padding: '0.875rem' }}>
              <div className="stat-label" style={{ fontSize: '0.75rem' }}>
                <Activity style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
                <span>Total Requests</span>
              </div>
              <div className="stat-value" style={{ fontSize: '1.375rem' }}>
                {analytics.totalRequests.toLocaleString()}
              </div>
            </div>

            <div className="stat-card" style={{ padding: '0.875rem' }}>
              <div className="stat-label" style={{ fontSize: '0.75rem' }}>
                <CheckCircle2 style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
                <span>Success Rate</span>
              </div>
              <div className="stat-value" style={{ fontSize: '1.375rem', color: 'var(--success-text)' }}>
                {analytics.successRate.toFixed(1)}%
              </div>
              <small style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                {analytics.successCount.toLocaleString()} successful
              </small>
            </div>

            <div className="stat-card" style={{ padding: '0.875rem' }}>
              <div className="stat-label" style={{ fontSize: '0.75rem' }}>
                <AlertTriangle style={{ width: '0.875rem', height: '0.875rem', color: 'var(--danger)' }} />
                <span>Errors</span>
              </div>
              <div className="stat-value" style={{ fontSize: '1.375rem', color: analytics.errorCount > 0 ? 'var(--danger-text)' : 'var(--text-primary)' }}>
                {analytics.errorCount.toLocaleString()}
              </div>
              <small style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                {analytics.errorRate.toFixed(1)}% error rate
              </small>
            </div>

            <div className="stat-card" style={{ padding: '0.875rem' }}>
              <div className="stat-label" style={{ fontSize: '0.75rem' }}>
                <Clock style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
                <span>Avg Latency</span>
              </div>
              <div className="stat-value" style={{ fontSize: '1.375rem' }}>
                {analytics.avgLatencyMs} <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>ms</span>
              </div>
            </div>
          </div>

          {/* Latency Percentiles & Status Breakdown */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              gap: '1rem',
              marginBottom: '1.5rem',
            }}
          >
            {/* Latency Percentiles */}
            <div className="card" style={{ padding: '1rem' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                <Zap style={{ width: '1rem', height: '1rem', color: 'var(--warning)' }} />
                Latency Percentiles
              </h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', borderBottom: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Median (P50)</span>
                <span style={{ fontWeight: 600 }}>{analytics.p50LatencyMs} ms</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', borderBottom: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>95th Percentile (P95)</span>
                <span style={{ fontWeight: 600 }}>{analytics.p95LatencyMs} ms</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>99th Percentile (P99)</span>
                <span style={{ fontWeight: 600 }}>{analytics.p99LatencyMs} ms</span>
              </div>
            </div>

            {/* Error & Throttling Breakdown */}
            <div className="card" style={{ padding: '1rem' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                <ShieldAlert style={{ width: '1rem', height: '1rem', color: 'var(--danger)' }} />
                Status & Rate Limit Codes
              </h4>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', borderBottom: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>4xx Client Errors</span>
                <span style={{ fontWeight: 600, color: analytics.status4xxCount > 0 ? 'var(--warning-text)' : 'var(--text-primary)' }}>
                  {analytics.status4xxCount}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', borderBottom: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>5xx Server / Gateway Errors</span>
                <span style={{ fontWeight: 600, color: analytics.status5xxCount > 0 ? 'var(--danger-text)' : 'var(--text-primary)' }}>
                  {analytics.status5xxCount}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.375rem 0', fontSize: '0.8125rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>429 Rate-Limited (Throttled)</span>
                <span style={{ fontWeight: 600, color: analytics.rateLimitedCount > 0 ? 'var(--warning-text)' : 'var(--text-primary)' }}>
                  {analytics.rateLimitedCount}
                </span>
              </div>
            </div>
          </div>

          {/* Timestamps */}
          <div
            style={{
              display: 'flex',
              gap: '1.5rem',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              marginBottom: '1.25rem',
              padding: '0.5rem 0.75rem',
              backgroundColor: 'var(--bg-surface)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Calendar style={{ width: '0.75rem', height: '0.75rem' }} />
              First Discovered: {new Date(analytics.firstSeenAt).toLocaleString()}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <Clock style={{ width: '0.75rem', height: '0.75rem' }} />
              Last Traffic: {new Date(analytics.lastSeenAt).toLocaleString()}
            </span>
          </div>

          {/* Recent Requests Table */}
          <div>
            <h4 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <Layers style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
              Recent Gateway Requests
            </h4>

            {analytics.recentRequests.length === 0 ? (
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>No request logs recorded yet.</p>
            ) : (
              <div className="table-container" style={{ maxHeight: '220px', overflowY: 'auto' }}>
                <table className="table" style={{ fontSize: '0.8125rem' }}>
                  <thead>
                    <tr>
                      <th>Status</th>
                      <th>Actual Path</th>
                      <th>Latency</th>
                      <th>Client IP</th>
                      <th>Timestamp</th>
                    </tr>
                  </thead>
                  <tbody>
                    {analytics.recentRequests.map((req) => (
                      <tr key={req.requestId}>
                        <td>
                          <span
                            className={`badge ${
                              req.statusCode >= 200 && req.statusCode < 400
                                ? 'badge-healthy'
                                : req.statusCode === 429
                                ? 'badge-degraded'
                                : 'badge-unavailable'
                            }`}
                            style={{ fontSize: '0.6875rem' }}
                          >
                            {req.statusCode}
                          </span>
                        </td>
                        <td style={{ fontFamily: 'var(--font-mono)' }}>{req.path}</td>
                        <td>{req.latencyMs} ms</td>
                        <td>{req.clientIp || '127.0.0.1'}</td>
                        <td style={{ color: 'var(--text-muted)' }}>
                          {new Date(req.timestamp).toLocaleTimeString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      ) : null}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
        <button onClick={onClose} className="btn btn-secondary btn-sm">
          Close
        </button>
      </div>
    </Modal>
  );
};
