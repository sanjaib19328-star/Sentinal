import React, { useState } from 'react';
import { ApiEndpoint } from '../../types/application';
import { ApiDetailsModal } from './ApiDetailsModal';
import { ConnectApplicationModal } from './ConnectApplicationModal';
import { ApiTestConsoleModal } from './ApiTestConsoleModal';
import { BulkApiCheckModal } from './BulkApiCheckModal';
import { LoadingSpinner } from '../common/LoadingSpinner';
import {
  Compass,
  Search,
  Zap,
  Clock,
  RotateCw,
  Info,
  Play,
  CheckCircle,
  BookOpen,
  Sparkles,
} from 'lucide-react';

interface ApiCatalogTabProps {
  applicationId: number;
  applicationBaseUrl?: string;
  endpoints: ApiEndpoint[];
  loading: boolean;
  onRefresh: () => void;
}

export const ApiCatalogTab: React.FC<ApiCatalogTabProps> = ({
  applicationId,
  applicationBaseUrl,
  endpoints,
  loading,
  onRefresh,
}) => {
  const [search, setSearch] = useState('');
  const [selectedMethod, setSelectedMethod] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiEndpoint | null>(null);
  const [consoleEndpoint, setConsoleEndpoint] = useState<ApiEndpoint | null>(null);
  const [isConsoleOpen, setIsConsoleOpen] = useState(false);
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false);
  const [isBulkCheckOpen, setIsBulkCheckOpen] = useState(false);

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

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'DOCUMENTED_AND_DISCOVERED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">
            <CheckCircle className="w-2.5 h-2.5 mr-1" />
            Verified & Active
          </span>
        );
      case 'DOCUMENTED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold bg-sky-500/10 text-sky-400 border border-sky-500/20">
            <BookOpen className="w-2.5 h-2.5 mr-1" />
            Documented
          </span>
        );
      case 'DISCOVERED':
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Compass className="w-2.5 h-2.5 mr-1" />
            Discovered
          </span>
        );
    }
  };

  const filteredEndpoints = endpoints.filter((ep) => {
    const matchesSearch =
      ep.normalizedPath.toLowerCase().includes(search.toLowerCase()) ||
      ep.method.toLowerCase().includes(search.toLowerCase()) ||
      (ep.summary && ep.summary.toLowerCase().includes(search.toLowerCase()));
    const matchesMethod =
      selectedMethod === 'ALL' || ep.method.toUpperCase() === selectedMethod;
    const matchesStatus =
      selectedStatus === 'ALL' || ep.documentationStatus === selectedStatus;
    return matchesSearch && matchesMethod && matchesStatus;
  });

  const methodsList = ['ALL', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

  return (
    <div>
      {/* Header & Description */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.25rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              API Catalog & Documentation
            </h3>
            <span className="badge badge-primary" style={{ fontSize: '0.75rem' }}>
              {endpoints.length} APIs
            </span>
          </div>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
            Central catalog combining OpenAPI documentation with real-time gateway auto-discovery
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            type="button"
            onClick={() => setIsConnectModalOpen(true)}
            className="btn btn-primary btn-sm flex items-center gap-1.5"
            style={{ height: '34px', padding: '0 0.875rem' }}
          >
            <Sparkles style={{ width: '0.875rem', height: '0.875rem' }} />
            Import & Discover APIs
          </button>

          <button
            type="button"
            onClick={() => {
              setConsoleEndpoint(null);
              setIsConsoleOpen(true);
            }}
            className="btn btn-secondary btn-sm flex items-center gap-1.5"
            style={{ height: '34px', padding: '0 0.875rem' }}
          >
            <Play style={{ width: '0.875rem', height: '0.875rem' }} />
            API Console
          </button>

          <button
            type="button"
            onClick={() => setIsBulkCheckOpen(true)}
            className="btn btn-secondary btn-sm flex items-center gap-1.5"
            style={{ height: '34px', padding: '0 0.875rem' }}
          >
            <Zap style={{ width: '0.875rem', height: '0.875rem' }} />
            AI Bulk Check
          </button>

          {/* Search */}
          <div style={{ position: 'relative' }}>
            <Search
              style={{
                position: 'absolute',
                left: '0.625rem',
                top: '50%',
                transform: 'translateY(-50%)',
                width: '0.875rem',
                height: '0.875rem',
                color: 'var(--text-muted)',
              }}
            />
            <input
              type="text"
              placeholder="Search APIs..."
              className="form-input"
              style={{ paddingLeft: '2rem', height: '34px', fontSize: '0.8125rem', width: '180px' }}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {/* Status Filter */}
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="form-input"
            style={{ height: '34px', fontSize: '0.8125rem', width: '130px', padding: '0 0.5rem' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="DISCOVERED">Discovered</option>
            <option value="DOCUMENTED">Documented</option>
            <option value="DOCUMENTED_AND_DISCOVERED">Verified</option>
          </select>

          {/* Method Filter */}
          <div style={{ display: 'flex', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
            {methodsList.map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => setSelectedMethod(m)}
                style={{
                  padding: '0.25rem 0.5rem',
                  fontSize: '0.6875rem',
                  fontWeight: selectedMethod === m ? 700 : 500,
                  backgroundColor: selectedMethod === m ? 'var(--primary)' : 'var(--bg-surface)',
                  color: selectedMethod === m ? '#fff' : 'var(--text-secondary)',
                  border: 'none',
                  cursor: 'pointer',
                  borderRight: '1px solid var(--border-color)',
                }}
              >
                {m}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={onRefresh}
            className="btn btn-secondary btn-sm"
            title="Refresh APIs"
            style={{ height: '34px', padding: '0 0.625rem' }}
          >
            <RotateCw style={{ width: '0.875rem', height: '0.875rem' }} />
          </button>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Scanning for endpoints..." />
      ) : endpoints.length === 0 ? (
        <div
          style={{
            padding: '3rem 1.5rem',
            textAlign: 'center',
            backgroundColor: 'var(--bg-surface)',
            border: '1px dashed var(--border-color)',
            borderRadius: 'var(--radius-lg)',
          }}
        >
          <Compass style={{ width: '3rem', height: '3rem', color: 'var(--text-muted)', margin: '0 auto 1rem' }} />
          <h4 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.375rem' }}>
            No APIs Cataloged Yet
          </h4>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', maxWidth: '32rem', margin: '0 auto 1.25rem' }}>
            Discover APIs by connecting your application backend or routing live requests through the Sentinel gateway.
          </p>

          <div className="flex justify-center space-x-3 mb-4">
            <button
              onClick={() => setIsConnectModalOpen(true)}
              className="btn btn-primary btn-sm flex items-center gap-1.5"
            >
              <Sparkles className="w-4 h-4" />
              Import & Discover APIs
            </button>
          </div>

          <div
            style={{
              maxWidth: '520px',
              margin: '0 auto',
              padding: '0.875rem 1rem',
              backgroundColor: 'var(--bg-main)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-md)',
              textAlign: 'left',
              fontSize: '0.75rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', marginBottom: '0.375rem', color: 'var(--text-primary)', fontWeight: 600 }}>
              <Info style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
              Route traffic through Sentinel:
            </div>
            <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)', display: 'block', wordBreak: 'break-all' }}>
              curl -H "X-Sentinel-API-Key: YOUR_KEY" http://localhost:8080/api/v1/gateway/your/path
            </code>
          </div>
        </div>
      ) : filteredEndpoints.length === 0 ? (
        <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          No endpoints match the filter "{search}" ({selectedMethod})
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th style={{ width: '80px' }}>Method</th>
                <th>Endpoint & Summary</th>
                <th>Origin / Status</th>
                <th>Requests</th>
                <th>Errors</th>
                <th>Avg Latency</th>
                <th>Success Rate</th>
                <th>Last Traffic</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredEndpoints.map((ep) => (
                <tr key={ep.id}>
                  <td>
                    <span
                      className={`badge ${getMethodBadgeClass(ep.method)}`}
                      style={{ fontSize: '0.75rem', fontWeight: 700, minWidth: '55px', textAlign: 'center', display: 'inline-block' }}
                    >
                      {ep.method}
                    </span>
                  </td>
                  <td>
                    <div>
                      <div className="flex items-center space-x-2">
                        <code style={{ fontWeight: 600, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)', fontSize: '0.875rem' }}>
                          {ep.normalizedPath}
                        </code>
                        {ep.deprecated && (
                          <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                            DEPRECATED
                          </span>
                        )}
                      </div>
                      {ep.summary && (
                        <p className="text-xs text-slate-400 mt-0.5 line-clamp-1">{ep.summary}</p>
                      )}
                    </div>
                  </td>
                  <td>{getStatusBadge(ep.documentationStatus)}</td>
                  <td style={{ fontWeight: 600 }}>{ep.totalRequests.toLocaleString()}</td>
                  <td style={{ color: ep.errorCount > 0 ? 'var(--danger-text)' : 'var(--text-muted)' }}>
                    {ep.errorCount.toLocaleString()}
                  </td>
                  <td>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                      <Clock style={{ width: '0.75rem', height: '0.75rem', color: 'var(--text-muted)' }} />
                      {ep.avgLatencyMs} ms
                    </span>
                  </td>
                  <td>
                    <span
                      style={{
                        fontWeight: 600,
                        color: ep.successRate >= 95 ? 'var(--success-text)' : ep.successRate >= 80 ? 'var(--warning-text)' : 'var(--danger-text)',
                      }}
                    >
                      {ep.successRate.toFixed(1)}%
                    </span>
                  </td>
                  <td style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    {ep.lastSeenAt ? new Date(ep.lastSeenAt).toLocaleTimeString() : 'Never'}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                      <button
                        onClick={() => {
                          setConsoleEndpoint(ep);
                          setIsConsoleOpen(true);
                        }}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                        title="Test endpoint in Console"
                      >
                        <Play style={{ width: '0.75rem', height: '0.75rem', color: '#10b981' }} />
                        Try
                      </button>
                      <button
                        onClick={() => setSelectedEndpoint(ep)}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                        title="View endpoint analytics"
                      >
                        <Zap style={{ width: '0.75rem', height: '0.75rem' }} />
                        Analytics
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Endpoint Analytics Drilldown Modal */}
      <ApiDetailsModal
        isOpen={!!selectedEndpoint}
        applicationId={applicationId}
        endpoint={selectedEndpoint}
        onClose={() => setSelectedEndpoint(null)}
      />

      {/* Simplified Connect & Auto-Discover Modal */}
      <ConnectApplicationModal
        isOpen={isConnectModalOpen}
        initialUrl={applicationBaseUrl}
        onClose={() => setIsConnectModalOpen(false)}
        onSuccess={() => {
          setIsConnectModalOpen(false);
          onRefresh();
        }}
      />

      {/* API Test Console Modal */}
      <ApiTestConsoleModal
        isOpen={isConsoleOpen}
        applicationId={applicationId}
        initialEndpoint={consoleEndpoint}
        onExecuted={() => onRefresh()}
        onClose={() => {
          setIsConsoleOpen(false);
          setConsoleEndpoint(null);
        }}
      />

      {/* AI Bulk API Check Modal */}
      <BulkApiCheckModal
        isOpen={isBulkCheckOpen}
        applicationId={applicationId}
        totalEndpointsCount={endpoints.length}
        onClose={() => {
          setIsBulkCheckOpen(false);
          onRefresh();
        }}
      />
    </div>
  );
};
