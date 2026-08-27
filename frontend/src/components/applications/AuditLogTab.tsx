import React, { useEffect, useState } from 'react';
import { AuditLog, AuditAction } from '../../types/audit';
import { PagedResponse } from '../../types/application';
import { getAuditLogs } from '../../api/audit';
import { getErrorMessage } from '../../api/client';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorBanner } from '../common/ErrorBanner';
import { EmptyState } from '../common/EmptyState';
import {
  FileText,
  RotateCcw,
  KeyRound,
  Sliders,
  Bell,
  Layers,
} from 'lucide-react';

interface AuditLogTabProps {
  applicationId: number;
}

export const AuditLogTab: React.FC<AuditLogTabProps> = ({ applicationId }) => {
  const [data, setData] = useState<PagedResponse<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionFilter, setActionFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  const fetchLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getAuditLogs({
        applicationId,
        action: actionFilter ? (actionFilter as AuditAction) : undefined,
        page,
        size: 20,
      });
      setData(response);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [applicationId, actionFilter, page]);

  const getActionBadgeColor = (action: AuditAction) => {
    if (action.includes('CREATED')) return { bg: 'rgba(34, 197, 94, 0.12)', text: 'var(--success)' };
    if (action.includes('DELETED') || action.includes('REVOKED')) return { bg: 'rgba(239, 68, 68, 0.12)', text: 'var(--danger)' };
    if (action.includes('UPDATED')) return { bg: 'rgba(59, 130, 246, 0.12)', text: 'var(--primary)' };
    return { bg: 'rgba(107, 114, 128, 0.12)', text: '#4b5563' };
  };

  const getActionIcon = (action: AuditAction) => {
    if (action.startsWith('API_KEY')) return <KeyRound style={{ width: '0.875rem', height: '0.875rem' }} />;
    if (action.startsWith('POLICY')) return <Sliders style={{ width: '0.875rem', height: '0.875rem' }} />;
    if (action.startsWith('ALERT')) return <Bell style={{ width: '0.875rem', height: '0.875rem' }} />;
    return <Layers style={{ width: '0.875rem', height: '0.875rem' }} />;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {error && <ErrorBanner message={error} onRetry={fetchLogs} />}

      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h3 className="card-title">Management Plane Audit Log</h3>
            <p className="card-subtitle">
              Immutable trail of configuration modifications, API key lifecycle events, and security policy changes
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <select
              value={actionFilter}
              onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
              className="form-input"
              style={{ width: 'auto', padding: '0.375rem 0.75rem', fontSize: '0.8125rem' }}
            >
              <option value="">All Action Types</option>
              <option value="APPLICATION_UPDATED">Application Updates</option>
              <option value="API_KEY_CREATED">API Key Created</option>
              <option value="API_KEY_UPDATED">API Key Updated</option>
              <option value="API_KEY_REVOKED">API Key Revoked</option>
              <option value="POLICY_CREATED">Policy Created</option>
              <option value="POLICY_UPDATED">Policy Updated</option>
              <option value="ALERT_RULE_CREATED">Alert Rule Created</option>
              <option value="ALERT_RULE_DELETED">Alert Rule Deleted</option>
            </select>

            <button onClick={fetchLogs} className="btn btn-secondary btn-sm">
              <RotateCcw style={{ width: '0.875rem', height: '0.875rem' }} />
              Refresh
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '2rem' }}>
            <LoadingSpinner message="Fetching audit logs..." />
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={FileText}
            title="No Audit Records Found"
            description="Management plane events such as key generation, rate policy updates, and rule creations will appear here."
          />
        ) : (
          <>
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Target</th>
                    <th>Description</th>
                    <th>Actor</th>
                    <th>IP Address</th>
                    <th style={{ textAlign: 'right' }}>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((log) => {
                    const badge = getActionBadgeColor(log.action);
                    return (
                      <tr key={log.id}>
                        <td>
                          <div
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.375rem',
                              padding: '0.25rem 0.5rem',
                              borderRadius: '0.25rem',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              background: badge.bg,
                              color: badge.text,
                            }}
                          >
                            {getActionIcon(log.action)}
                            <span>{log.action}</span>
                          </div>
                        </td>
                        <td style={{ fontSize: '0.8125rem', fontWeight: 600 }}>
                          {log.targetType} {log.targetId ? `#${log.targetId}` : ''}
                        </td>
                        <td style={{ fontSize: '0.8125rem' }}>{log.description}</td>
                        <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>{log.actorEmail}</td>
                        <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>{log.ipAddress || '127.0.0.1'}</td>
                        <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {new Date(log.createdAt).toLocaleString()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {data.totalPages > 1 && (
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
                  Page {data.page + 1} of {data.totalPages} ({data.totalElements} records)
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
                    onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                    disabled={page >= data.totalPages - 1}
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
