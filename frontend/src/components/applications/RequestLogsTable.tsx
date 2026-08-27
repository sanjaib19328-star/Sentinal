import React from 'react';
import { PagedResponse, RequestLog } from '../../types/application';
import { EmptyState } from '../common/EmptyState';
import { FileText, ChevronLeft, ChevronRight } from 'lucide-react';

interface RequestLogsTableProps {
  logsData: PagedResponse<RequestLog> | null;
  loading: boolean;
  page: number;
  onPageChange: (newPage: number) => void;
}

export const RequestLogsTable: React.FC<RequestLogsTableProps> = ({
  logsData,
  loading,
  page,
  onPageChange,
}) => {
  const getStatusBadgeColor = (code: number) => {
    if (code >= 200 && code < 300) return 'badge-healthy';
    if (code >= 300 && code < 400) return 'badge-healthy';
    if (code >= 400 && code < 500) return 'badge-degraded';
    return 'badge-unavailable';
  };

  const getMethodBadge = (method: string) => {
    return (
      <span
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: '0.75rem',
          fontWeight: 600,
          padding: '0.2rem 0.5rem',
          borderRadius: 'var(--radius-sm)',
          backgroundColor: '#f1f5f9',
          color: '#334155',
          border: '1px solid #cbd5e1',
        }}
      >
        {method}
      </span>
    );
  };

  if (!logsData || logsData.totalElements === 0) {
    return (
      <EmptyState
        icon={FileText}
        title="No Request Logs Recorded"
        description="No gateway traffic has been routed for this application's API keys yet. Send requests with X-Sentinel-Api-Key to record real telemetry."
      />
    );
  }

  return (
    <div>
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Status</th>
              <th>Method</th>
              <th>Path</th>
              <th>Latency</th>
              <th>Client IP</th>
              <th>Timestamp</th>
              <th>Request ID</th>
            </tr>
          </thead>
          <tbody>
            {logsData.content.map((log) => (
              <tr key={log.requestId}>
                <td>
                  <span className={`badge ${getStatusBadgeColor(log.statusCode)}`}>
                    {log.statusCode}
                  </span>
                </td>
                <td>{getMethodBadge(log.method)}</td>
                <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                  {log.path}
                </td>
                <td style={{ fontSize: '0.8125rem' }}>
                  {log.latencyMs} ms
                </td>
                <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  {log.clientIp || '—'}
                </td>
                <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  {new Date(log.timestamp).toLocaleString()}
                </td>
                <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {log.requestId.substring(0, 8)}...
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginTop: '1rem',
          padding: '0.5rem 0',
        }}
      >
        <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
          Showing page {logsData.page + 1} of {Math.max(1, logsData.totalPages)} ({logsData.totalElements} total logs)
        </span>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0 || loading}
            className="btn btn-secondary btn-sm"
          >
            <ChevronLeft style={{ width: '0.875rem', height: '0.875rem' }} />
            Previous
          </button>
          <button
            onClick={() => onPageChange(page + 1)}
            disabled={page + 1 >= logsData.totalPages || loading}
            className="btn btn-secondary btn-sm"
          >
            Next
            <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
          </button>
        </div>
      </div>
    </div>
  );
};
