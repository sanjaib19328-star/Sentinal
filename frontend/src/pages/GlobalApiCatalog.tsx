import React, { useEffect, useState } from 'react';
import { globalApisApi } from '../api/globalApis';
import { applicationsApi } from '../api/applications';
import { GlobalApiEndpoint } from '../types/globalApi';
import { Application } from '../types/application';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { ApiTestConsoleModal } from '../components/applications/ApiTestConsoleModal';
import { getErrorMessage } from '../api/client';
import {
  Compass,
  Search,
  Play,
  RotateCw,
  Layers,
  ArrowUpDown,
  BookOpen,
  CheckCircle,
} from 'lucide-react';

export const GlobalApiCatalog: React.FC = () => {
  const [apis, setApis] = useState<GlobalApiEndpoint[]>([]);
  const [apps, setApps] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [selectedAppId, setSelectedAppId] = useState<string>('ALL');
  const [selectedMethod, setSelectedMethod] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [showDeprecatedOnly, setShowDeprecatedOnly] = useState(false);
  const [sortBy, setSortBy] = useState<'requests' | 'latency' | 'p95' | 'errors' | 'lastseen' | 'path'>('requests');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Test Console
  const [consoleOpen, setConsoleOpen] = useState(false);
  const [selectedEndpointForConsole, setSelectedEndpointForConsole] = useState<{
    appId: number;
    ep: any;
  } | null>(null);

  const fetchApis = async () => {
    setLoading(true);
    setError(null);
    try {
      const [apiList, appList] = await Promise.all([
        globalApisApi.listGlobalApis({
          search: search.trim() || undefined,
          applicationId: selectedAppId !== 'ALL' ? Number(selectedAppId) : undefined,
          method: selectedMethod !== 'ALL' ? selectedMethod : undefined,
          documentationStatus: selectedStatus !== 'ALL' ? selectedStatus : undefined,
          deprecated: showDeprecatedOnly ? true : undefined,
          sortBy,
          sortDir,
        }),
        applicationsApi.list(),
      ]);
      setApis(apiList);
      setApps(appList);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApis();
  }, [selectedAppId, selectedMethod, selectedStatus, showDeprecatedOnly, sortBy, sortDir]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchApis();
  };

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

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'DOCUMENTED_AND_DISCOVERED':
        return (
          <span className="badge badge-healthy" style={{ fontSize: '0.6875rem' }}>
            <CheckCircle style={{ width: '0.625rem', height: '0.625rem', marginRight: '0.25rem' }} />
            Verified & Active
          </span>
        );
      case 'DOCUMENTED':
        return (
          <span className="badge badge-primary" style={{ fontSize: '0.6875rem' }}>
            <BookOpen style={{ width: '0.625rem', height: '0.625rem', marginRight: '0.25rem' }} />
            Documented
          </span>
        );
      case 'DISCOVERED':
      default:
        return (
          <span className="badge badge-secondary" style={{ fontSize: '0.6875rem' }}>
            <Compass style={{ width: '0.625rem', height: '0.625rem', marginRight: '0.25rem' }} />
            Discovered
          </span>
        );
    }
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.75rem' }}>
        <div>
          <h1 className="page-title">Global API Catalog & Management</h1>
          <p className="page-subtitle">
            Centralized directory of all discovered and documented APIs across registered applications
          </p>
        </div>

        <button onClick={fetchApis} className="btn btn-secondary btn-sm" title="Refresh APIs">
          <RotateCw style={{ width: '1rem', height: '1rem' }} />
          Refresh
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchApis} />}

      {/* Filter & Search Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1rem 1.25rem' }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: '1 1 240px' }}>
            <Search style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', width: '1rem', height: '1rem', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Search by path, summary, or application..."
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

          {/* Documentation Status */}
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="form-input"
            style={{ width: '160px' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="DISCOVERED">Discovered</option>
            <option value="DOCUMENTED">Documented</option>
            <option value="DOCUMENTED_AND_DISCOVERED">Verified & Active</option>
          </select>

          {/* Sort By */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="form-input"
            style={{ width: '140px' }}
          >
            <option value="requests">Sort: Requests</option>
            <option value="latency">Sort: Avg Latency</option>
            <option value="p95">Sort: P95 Latency</option>
            <option value="errors">Sort: Error Rate</option>
            <option value="lastseen">Sort: Last Active</option>
            <option value="path">Sort: Path A-Z</option>
          </select>

          <button
            type="button"
            onClick={() => setSortDir(sortDir === 'asc' ? 'desc' : 'asc')}
            className="btn btn-secondary"
            title={`Toggle sort order (Current: ${sortDir.toUpperCase()})`}
            style={{ padding: '0.5rem 0.75rem' }}
          >
            <ArrowUpDown style={{ width: '1rem', height: '1rem' }} />
          </button>

          <label style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={showDeprecatedOnly}
              onChange={(e) => setShowDeprecatedOnly(e.target.checked)}
            />
            Deprecated only
          </label>
        </form>
      </div>

      {/* Main Table */}
      {loading ? (
        <LoadingSpinner message="Scanning unified API registry across all applications..." />
      ) : apis.length === 0 ? (
        <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Compass style={{ width: '3rem', height: '3rem', margin: '0 auto 1rem', opacity: 0.5 }} />
          <h3 style={{ fontSize: '1.125rem', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>No APIs Found</h3>
          <p style={{ fontSize: '0.875rem', maxWidth: '400px', margin: '0 auto' }}>
            No registered APIs match your search criteria. Route traffic through Sentinel or import OpenAPI specifications.
          </p>
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th style={{ width: '80px' }}>Method</th>
                  <th>Path & Summary</th>
                  <th>Application</th>
                  <th>Origin / Status</th>
                  <th style={{ textAlign: 'right' }}>Total Reqs</th>
                  <th style={{ textAlign: 'right' }}>Error Rate</th>
                  <th style={{ textAlign: 'right' }}>Avg / P95 Latency</th>
                  <th style={{ textAlign: 'right' }}>Last Seen</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {apis.map((api) => (
                  <tr key={api.id}>
                    <td>
                      <span className={`badge ${getMethodBadgeClass(api.method)}`} style={{ fontWeight: 700, minWidth: '55px', textAlign: 'center', display: 'inline-block' }}>
                        {api.method}
                      </span>
                    </td>
                    <td>
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <code style={{ fontWeight: 600, color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>
                            {api.normalizedPath}
                          </code>
                          {api.deprecated && (
                            <span style={{ fontSize: '0.625rem', padding: '0.1rem 0.35rem', borderRadius: '0.25rem', background: 'rgba(239, 68, 68, 0.15)', color: 'var(--danger)', fontWeight: 700 }}>
                              DEPRECATED
                            </span>
                          )}
                        </div>
                        {api.summary && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                            {api.summary}
                          </div>
                        )}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem', fontWeight: 500 }}>
                        <Layers style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
                        <span>{api.applicationName}</span>
                      </div>
                    </td>
                    <td>{getStatusBadge(api.documentationStatus)}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>{api.totalRequests.toLocaleString()}</td>
                    <td style={{ textAlign: 'right' }}>
                      <span style={{ color: api.errorRate > 5 ? 'var(--danger-text)' : 'inherit', fontWeight: api.errorRate > 0 ? 600 : 400 }}>
                        {api.errorRate}%
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', fontFamily: 'monospace', fontSize: '0.8125rem' }}>
                      <span>{api.avgLatencyMs}ms</span>
                      <span style={{ color: 'var(--text-muted)', marginLeft: '0.375rem', fontSize: '0.75rem' }}>
                        ({api.p95LatencyMs}ms)
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {api.lastSeenAt ? new Date(api.lastSeenAt).toLocaleTimeString() : 'Never'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        onClick={() => {
                          setSelectedEndpointForConsole({
                            appId: api.applicationId,
                            ep: {
                              id: api.id,
                              applicationId: api.applicationId,
                              method: api.method,
                              normalizedPath: api.normalizedPath,
                              documentationStatus: api.documentationStatus,
                              summary: api.summary,
                              description: api.description,
                              parametersJson: api.parametersJson,
                              requestBodySchemaJson: api.requestBodySchemaJson,
                              responsesJson: api.responsesJson,
                              deprecated: api.deprecated,
                              firstSeenAt: api.firstSeenAt,
                              lastSeenAt: api.lastSeenAt,
                              totalRequests: api.totalRequests,
                              errorCount: api.errorCount,
                              avgLatencyMs: api.avgLatencyMs,
                              successRate: api.successRate,
                            },
                          });
                          setConsoleOpen(true);
                        }}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                      >
                        <Play style={{ width: '0.75rem', height: '0.75rem', color: '#10b981' }} />
                        Try
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* API Test Console Modal */}
      {selectedEndpointForConsole && (
        <ApiTestConsoleModal
          isOpen={consoleOpen}
          applicationId={selectedEndpointForConsole.appId}
          initialEndpoint={selectedEndpointForConsole.ep}
          onClose={() => {
            setConsoleOpen(false);
            setSelectedEndpointForConsole(null);
          }}
        />
      )}
    </div>
  );
};
