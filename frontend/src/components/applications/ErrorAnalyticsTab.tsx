import React, { useEffect, useState } from 'react';
import { ErrorAnalyticsResponse } from '../../types/analytics';
import { getErrorAnalytics } from '../../api/analytics';
import { getErrorMessage } from '../../api/client';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorBanner } from '../common/ErrorBanner';
import { EmptyState } from '../common/EmptyState';
import {
  AlertOctagon,
  AlertTriangle,
  RotateCcw,
  Clock,
  CheckCircle,
} from 'lucide-react';

interface ErrorAnalyticsTabProps {
  applicationId: number;
}

export const ErrorAnalyticsTab: React.FC<ErrorAnalyticsTabProps> = ({ applicationId }) => {
  const [data, setData] = useState<ErrorAnalyticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [methodFilter, setMethodFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  const fetchErrors = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getErrorAnalytics(applicationId, {
        status: statusFilter ? parseInt(statusFilter, 10) : undefined,
        method: methodFilter || undefined,
        page,
        size: 15,
      });
      setData(response);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchErrors();
  }, [applicationId, statusFilter, methodFilter, page]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {error && <ErrorBanner message={error} onRetry={fetchErrors} />}

      {/* Summary KPI Cards */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-label">
            <AlertOctagon style={{ width: '1rem', height: '1rem', color: 'var(--danger)' }} />
            <span>Total Errors</span>
          </div>
          <div className="stat-value" style={{ color: 'var(--danger-text)' }}>
            {data ? data.totalErrors : 0}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            All 4xx & 5xx HTTP responses
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <AlertTriangle style={{ width: '1rem', height: '1rem', color: 'var(--warning)' }} />
            <span>Error Rate</span>
          </div>
          <div className="stat-value" style={{ color: data && data.errorRate > 5 ? 'var(--danger-text)' : 'inherit' }}>
            {data ? `${data.errorRate}%` : '0%'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Percentage of total traffic
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <Clock style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
            <span>Top Failing Status</span>
          </div>
          <div className="stat-value">
            {data && data.errorByStatusCode.length > 0 ? data.errorByStatusCode[0].key : 'None'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            {data && data.errorByStatusCode.length > 0 ? `${data.errorByStatusCode[0].count} occurrences` : 'No errors recorded'}
          </div>
        </div>
      </div>

      {/* Error Breakdown Charts / Distributions */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem' }}>
        {/* By Status Code */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Errors by Status Code</h3>
          </div>
          <div style={{ padding: '1.25rem' }}>
            {loading ? (
              <LoadingSpinner message="Calculating status distributions..." />
            ) : !data || data.errorByStatusCode.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>No status code errors recorded</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {data.errorByStatusCode.map((item) => (
                  <div key={item.key}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 600 }}>HTTP {item.key}</span>
                      <span style={{ color: 'var(--text-muted)' }}>{item.count} errors ({item.percentage}%)</span>
                    </div>
                    <div style={{ background: 'var(--surface-subtle, #f1f5f9)', borderRadius: '0.25rem', height: '0.5rem', overflow: 'hidden' }}>
                      <div
                        style={{
                          background: parseInt(item.key, 10) >= 500 ? 'var(--danger)' : 'var(--warning)',
                          width: `${item.percentage}%`,
                          height: '100%',
                          borderRadius: '0.25rem',
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* By Endpoint */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Errors by Endpoint</h3>
          </div>
          <div style={{ padding: '1.25rem' }}>
            {loading ? (
              <LoadingSpinner message="Calculating endpoint error distributions..." />
            ) : !data || data.errorByEndpoint.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>No endpoint errors recorded</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {data.errorByEndpoint.slice(0, 6).map((item) => (
                  <div key={item.key}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{item.key}</span>
                      <span style={{ color: 'var(--text-muted)' }}>{item.count} errors ({item.percentage}%)</span>
                    </div>
                    <div style={{ background: 'var(--surface-subtle, #f1f5f9)', borderRadius: '0.25rem', height: '0.5rem', overflow: 'hidden' }}>
                      <div
                        style={{
                          background: 'var(--danger)',
                          width: `${item.percentage}%`,
                          height: '100%',
                          borderRadius: '0.25rem',
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Filterable Error Logs Table */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h3 className="card-title">Error Request Log Stream</h3>
            <p className="card-subtitle">Filtered historical logs for 4xx and 5xx client and gateway responses</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', flexWrap: 'wrap' }}>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="form-input"
              style={{ width: 'auto', padding: '0.375rem 0.625rem', fontSize: '0.8125rem' }}
            >
              <option value="">All Error Statuses</option>
              <option value="400">400 Bad Request</option>
              <option value="401">401 Unauthorized</option>
              <option value="403">403 Forbidden</option>
              <option value="404">404 Not Found</option>
              <option value="405">405 Method Not Allowed</option>
              <option value="413">413 Payload Too Large</option>
              <option value="429">429 Rate Limit Exceeded</option>
              <option value="500">500 Internal Error</option>
              <option value="502">502 Bad Gateway</option>
            </select>

            <select
              value={methodFilter}
              onChange={(e) => { setMethodFilter(e.target.value); setPage(0); }}
              className="form-input"
              style={{ width: 'auto', padding: '0.375rem 0.625rem', fontSize: '0.8125rem' }}
            >
              <option value="">All Methods</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>

            <button onClick={fetchErrors} className="btn btn-secondary btn-sm">
              <RotateCcw style={{ width: '0.875rem', height: '0.875rem' }} />
              Refresh
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '2rem' }}>
            <LoadingSpinner message="Fetching error stream..." />
          </div>
        ) : !data || data.errorLogs.content.length === 0 ? (
          <EmptyState
            icon={CheckCircle}
            title="No Errors Found"
            description="No request logs matched your filter criteria. All gateway traffic in this view returned successful 2xx/3xx responses."
          />
        ) : (
          <>
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Method</th>
                    <th>Path</th>
                    <th>Request ID</th>
                    <th>Latency</th>
                    <th>Client IP</th>
                    <th style={{ textAlign: 'right' }}>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {data.errorLogs.content.map((log, idx) => (
                    <tr key={idx}>
                      <td>
                        <span
                          style={{
                            padding: '0.2rem 0.4rem',
                            borderRadius: '0.25rem',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            background: log.statusCode >= 500 ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                            color: log.statusCode >= 500 ? 'var(--danger)' : 'var(--warning)',
                          }}
                        >
                          {log.statusCode}
                        </span>
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, fontSize: '0.75rem' }}>{log.method}</span>
                      </td>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{log.path}</td>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {log.requestId}
                      </td>
                      <td style={{ fontSize: '0.8125rem' }}>{log.latencyMs}ms</td>
                      <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>{log.clientIp || '127.0.0.1'}</td>
                      <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {new Date(log.timestamp).toLocaleTimeString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {data.errorLogs.totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1rem 1.5rem',
                  borderTop: '1px solid var(--border-color)',
                }}
              >
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  Page {data.errorLogs.page + 1} of {data.errorLogs.totalPages} ({data.errorLogs.totalElements} errors)
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn btn-secondary btn-sm"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(data.errorLogs.totalPages - 1, p + 1))}
                    disabled={page >= data.errorLogs.totalPages - 1}
                    className="btn btn-secondary btn-sm"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
