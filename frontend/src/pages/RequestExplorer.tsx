import React, { useEffect, useState } from 'react';
import { globalRequestsApi } from '../api/globalRequests';
import { applicationsApi } from '../api/applications';
import { GlobalRequestLog } from '../types/globalRequest';
import { Application, PagedResponse } from '../types/application';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { getErrorMessage } from '../api/client';
import {
  FileText,
  Search,
  RotateCw,
  Layers,
  ChevronLeft,
  ChevronRight,
  Copy,
  Check,
} from 'lucide-react';

export const RequestExplorer: React.FC = () => {
  const [logsData, setLogsData] = useState<PagedResponse<GlobalRequestLog> | null>(null);
  const [apps, setApps] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedAppId, setSelectedAppId] = useState<string>('ALL');
  const [selectedMethod, setSelectedMethod] = useState<string>('ALL');
  const [selectedStatusClass, setSelectedStatusClass] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  // Detail Modal
  const [selectedLog, setSelectedLog] = useState<GlobalRequestLog | null>(null);
  const [copiedId, setCopiedId] = useState(false);

  const fetchLogs = async (currentPage = page) => {
    setLoading(true);
    setError(null);
    try {
      const [resLogs, resApps] = await Promise.all([
        globalRequestsApi.getGlobalRequests({
          applicationId: selectedAppId !== 'ALL' ? Number(selectedAppId) : undefined,
          method: selectedMethod !== 'ALL' ? selectedMethod : undefined,
          statusClass: selectedStatusClass !== 'ALL' ? selectedStatusClass : undefined,
          search: search.trim() || undefined,
          page: currentPage,
          size: pageSize,
        }),
        applicationsApi.list(),
      ]);
      setLogsData(resLogs);
      setApps(resApps);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(0);
    setPage(0);
  }, [selectedAppId, selectedMethod, selectedStatusClass, pageSize]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchLogs(0);
    setPage(0);
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 2000);
  };

  const getStatusBadgeClass = (code: number) => {
    if (code >= 200 && code < 300) return 'badge-healthy';
    if (code === 429) return 'badge-unavailable';
    if (code >= 400 && code < 500) return 'badge-degraded';
    return 'badge-unavailable';
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.75rem' }}>
        <div>
          <h1 className="page-title">Global Request Observability & Explorer</h1>
          <p className="page-subtitle">
            Live stream and deep-dive query engine for all gateway HTTP transactions across connected services
          </p>
        </div>

        <button onClick={() => fetchLogs(page)} className="btn btn-secondary btn-sm" title="Refresh Logs">
          <RotateCw style={{ width: '1rem', height: '1rem' }} />
          Refresh Stream
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => fetchLogs(page)} />}

      {/* Filter Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem 1.25rem' }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: '1 1 240px' }}>
            <Search style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', width: '1rem', height: '1rem', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Filter by Request ID, path, or client IP..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="form-input"
              style={{ paddingLeft: '2.25rem', width: '100%' }}
            />
          </div>

          {/* Application Selector */}
          <select
            value={selectedAppId}
            onChange={(e) => setSelectedAppId(e.target.value)}
            className="form-input"
            style={{ width: '180px' }}
          >
            <option value="ALL">All Applications</option>
            {apps.map((app) => (
              <option key={app.id} value={app.id}>
                {app.name}
              </option>
            ))}
          </select>

          {/* HTTP Method */}
          <select
            value={selectedMethod}
            onChange={(e) => setSelectedMethod(e.target.value)}
            className="form-input"
            style={{ width: '120px' }}
          >
            <option value="ALL">All Methods</option>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="PATCH">PATCH</option>
            <option value="DELETE">DELETE</option>
          </select>

          {/* Status Class */}
          <select
            value={selectedStatusClass}
            onChange={(e) => setSelectedStatusClass(e.target.value)}
            className="form-input"
            style={{ width: '140px' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="2xx">2xx Success</option>
            <option value="4xx">4xx Client Error</option>
            <option value="429">429 Throttled</option>
            <option value="5xx">5xx Server Error</option>
          </select>

          <select
            value={pageSize}
            onChange={(e) => setPageSize(Number(e.target.value))}
            className="form-input"
            style={{ width: '110px' }}
          >
            <option value="20">20 / page</option>
            <option value="50">50 / page</option>
            <option value="100">100 / page</option>
          </select>
        </form>
      </div>

      {/* Main Table */}
      {loading ? (
        <LoadingSpinner message="Querying distributed request logs..." />
      ) : !logsData || logsData.content.length === 0 ? (
        <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <FileText style={{ width: '3rem', height: '3rem', margin: '0 auto 1rem', opacity: 0.5 }} />
          <h3 style={{ fontSize: '1.125rem', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>No Requests Found</h3>
          <p style={{ fontSize: '0.875rem', maxWidth: '400px', margin: '0 auto' }}>
            No request telemetry matches the active filters. Route requests through the gateway to generate logs.
          </p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Method & Path</th>
                  <th>Application</th>
                  <th>Consumer Key</th>
                  <th>Latency</th>
                  <th>Client IP</th>
                  <th>Request ID</th>
                  <th style={{ textAlign: 'right' }}>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {logsData.content.map((log) => (
                  <tr
                    key={log.id}
                    onClick={() => setSelectedLog(log)}
                    style={{ cursor: 'pointer' }}
                    className="hover:bg-slate-800/40 transition-colors"
                  >
                    <td>
                      <span className={`badge ${getStatusBadgeClass(log.statusCode)}`} style={{ fontWeight: 700 }}>
                        {log.statusCode}
                      </span>
                    </td>
                    <td>
                      <div>
                        <span style={{ fontWeight: 700, marginRight: '0.375rem', color: 'var(--primary)' }}>
                          {log.method}
                        </span>
                        <code style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                          {log.path}
                        </code>
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem' }}>
                        <Layers style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)' }} />
                        <span>{log.applicationName}</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                        {log.keyName || 'Anonymous / Test'}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>
                      {log.latencyMs}ms
                    </td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {log.clientIp || '127.0.0.1'}
                    </td>
                    <td>
                      <span style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {log.requestId ? `${log.requestId.slice(0, 8)}...` : '-'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.875rem 1.25rem',
              borderTop: '1px solid var(--border-color)',
              fontSize: '0.8125rem',
              color: 'var(--text-muted)',
            }}
          >
            <div>
              Showing {logsData.page * logsData.size + 1} to {Math.min((logsData.page + 1) * logsData.size, logsData.totalElements)} of {logsData.totalElements.toLocaleString()} requests
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <button
                onClick={() => {
                  const prev = Math.max(0, page - 1);
                  setPage(prev);
                  fetchLogs(prev);
                }}
                disabled={page === 0}
                className="btn btn-secondary btn-sm"
              >
                <ChevronLeft style={{ width: '0.875rem', height: '0.875rem' }} />
                Previous
              </button>

              <span>Page {page + 1} of {Math.max(1, logsData.totalPages)}</span>

              <button
                onClick={() => {
                  const next = page + 1;
                  if (next < logsData.totalPages) {
                    setPage(next);
                    fetchLogs(next);
                  }
                }}
                disabled={page + 1 >= logsData.totalPages}
                className="btn btn-secondary btn-sm"
              >
                Next
                <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Log Detail Drawer / Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center space-x-3">
                <span className={`badge ${getStatusBadgeClass(selectedLog.statusCode)}`}>
                  {selectedLog.statusCode}
                </span>
                <span className="text-base font-bold text-white">
                  {selectedLog.method} {selectedLog.path}
                </span>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 block mb-1">Application</span>
                <strong className="text-slate-200 text-sm">{selectedLog.applicationName}</strong>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 block mb-1">API Key</span>
                <strong className="text-slate-200 text-sm">{selectedLog.keyName || 'Direct / Test Console'}</strong>
                {selectedLog.keyMasked && (
                  <div className="font-mono text-slate-500 text-[11px] mt-0.5">{selectedLog.keyMasked}</div>
                )}
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 block mb-1">Latency</span>
                <strong className="text-emerald-400 font-mono text-sm">{selectedLog.latencyMs} ms</strong>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 block mb-1">Client IP</span>
                <strong className="text-slate-200 font-mono text-sm">{selectedLog.clientIp || '127.0.0.1'}</strong>
              </div>
            </div>

            {/* Request ID & Traceability */}
            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500">X-Request-Id (Distributed Trace)</span>
                <button
                  onClick={() => handleCopy(selectedLog.requestId)}
                  className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
                >
                  {copiedId ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  {copiedId ? 'Copied' : 'Copy ID'}
                </button>
              </div>
              <code className="font-mono text-xs text-slate-300 block mt-1 select-all break-all">
                {selectedLog.requestId}
              </code>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedLog(null)}
                className="btn btn-secondary btn-sm"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
