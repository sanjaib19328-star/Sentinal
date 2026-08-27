import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { apiKeysApi } from '../api/apiKeys';
import { Application, PagedResponse, RequestLog, ApiEndpoint } from '../types/application';
import { MetricItem } from '../types/metrics';
import { ApiKey } from '../types/apiKey';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { ConnectionTestCard } from '../components/applications/ConnectionTestCard';
import { ConnectionAccessCard } from '../components/applications/ConnectionAccessCard';
import { ConnectApplicationModal } from '../components/applications/ConnectApplicationModal';
import { ApiTestConsoleModal } from '../components/applications/ApiTestConsoleModal';
import { EditApplicationModal } from '../components/applications/EditApplicationModal';
import { RequestLogsTable } from '../components/applications/RequestLogsTable';
import { MetricsOverview } from '../components/metrics/MetricsOverview';
import { CreateKeyModal } from '../components/apiKeys/CreateKeyModal';
import { EditKeyModal } from '../components/apiKeys/EditKeyModal';
import { ApiKeyModal } from '../components/apiKeys/ApiKeyModal';
import { ApiCatalogTab } from '../components/applications/ApiCatalogTab';
import { PolicyTab } from '../components/applications/PolicyTab';
import { AlertsTab } from '../components/applications/AlertsTab';
import { ErrorAnalyticsTab } from '../components/applications/ErrorAnalyticsTab';
import { AuditLogTab } from '../components/applications/AuditLogTab';
import { CircuitBreakerWidget } from '../components/applications/CircuitBreakerWidget';
import { ConsumerAnalyticsDrawer } from '../components/applications/ConsumerAnalyticsDrawer';
import { UpstreamAuthModal } from '../components/applications/UpstreamAuthModal';
import { getErrorMessage } from '../api/client';
import {
  ArrowLeft,
  Edit2,
  Trash2,
  BarChart3,
  FileText,
  KeyRound,
  Plus,
  Globe,
  Clock,
  Compass,
  LayoutDashboard,
  ShieldAlert,
  Sliders,
  AlertOctagon,
  History,
  RotateCw,
  Activity,
  ShieldCheck,
} from 'lucide-react';

export const ApplicationDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const appId = parseInt(id || '', 10);

  const [application, setApplication] = useState<Application | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<
    'overview' | 'apis' | 'requests' | 'metrics' | 'errors' | 'policies' | 'alerts' | 'keys' | 'audit'
  >('overview');

  // Discovered APIs State
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [endpointsLoading, setEndpointsLoading] = useState(false);

  // Metrics State
  const [metrics, setMetrics] = useState<MetricItem[]>([]);

  // Request Logs State
  const [logs, setLogs] = useState<PagedResponse<RequestLog> | null>(null);
  const [logsLoading, setLogsLoading] = useState(false);
  const [logsPage, setLogsPage] = useState(0);

  // API Keys State
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [keysLoading, setKeysLoading] = useState(false);
  const [isCreateKeyModalOpen, setIsCreateKeyModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null);
  const [newlyCreatedKey, setNewlyCreatedKey] = useState<ApiKey | null>(null);
  const [revokingKeyId, setRevokingKeyId] = useState<number | null>(null);
  const [deletingKeyId, setDeletingKeyId] = useState<number | null>(null);
  const [regeneratingKeyId, setRegeneratingKeyId] = useState<number | null>(null);

  // Phase 3 Consumer Analytics Drawer
  const [selectedKeyForAnalytics, setSelectedKeyForAnalytics] = useState<ApiKey | null>(null);

  // Modals & Dialogs
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isUpstreamAuthModalOpen, setIsUpstreamAuthModalOpen] = useState(false);
  const [isOpenApiModalOpen, setIsOpenApiModalOpen] = useState(false);
  const [isConsoleOpen, setIsConsoleOpen] = useState(false);
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchApplication = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await applicationsApi.getById(appId);
      setApplication(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [appId]);

  const fetchEndpoints = useCallback(async () => {
    setEndpointsLoading(true);
    try {
      const data = await applicationsApi.getApis(appId);
      setEndpoints(data);
    } catch (err) {
      console.error('Failed to load discovered endpoints', err);
    } finally {
      setEndpointsLoading(false);
    }
  }, [appId]);

  const fetchMetrics = useCallback(async () => {
    try {
      const data = await applicationsApi.getMetrics(appId);
      setMetrics(data.metrics || []);
    } catch (err) {
      console.error('Failed to load metrics', err);
    }
  }, [appId]);

  const fetchLogs = useCallback(
    async (page: number = 0) => {
      setLogsLoading(true);
      try {
        const data = await applicationsApi.getRequests(appId, page, 20);
        setLogs(data);
      } catch (err) {
        console.error('Failed to load request logs', err);
      } finally {
        setLogsLoading(false);
      }
    },
    [appId]
  );

  const fetchKeys = useCallback(async () => {
    setKeysLoading(true);
    try {
      const data = await apiKeysApi.list(appId);
      setKeys(data);
    } catch (err) {
      console.error('Failed to load API keys', err);
    } finally {
      setKeysLoading(false);
    }
  }, [appId]);

  useEffect(() => {
    if (!isNaN(appId)) {
      fetchApplication();
      fetchEndpoints();
      fetchMetrics();
      fetchKeys();
    }
  }, [appId, fetchApplication, fetchEndpoints, fetchMetrics, fetchKeys]);

  useEffect(() => {
    if (activeTab === 'requests' && !isNaN(appId)) {
      fetchLogs(logsPage);
    }
  }, [activeTab, logsPage, appId, fetchLogs]);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await applicationsApi.delete(appId);
      navigate('/applications');
    } catch (err) {
      setError(getErrorMessage(err));
      setIsDeleteConfirmOpen(false);
    } finally {
      setDeleting(false);
    }
  };

  const handleRevokeKey = async (keyId: number) => {
    try {
      await apiKeysApi.revoke(appId, keyId);
      setRevokingKeyId(null);
      fetchKeys();
    } catch (err) {
      alert(`Failed to revoke key: ${getErrorMessage(err)}`);
    }
  };

  const handleRegenerateKey = async (keyId: number) => {
    if (!window.confirm('Are you sure you want to regenerate this API key secret? The old secret will be revoked immediately.')) return;
    setRegeneratingKeyId(keyId);
    try {
      const updatedKey = await apiKeysApi.regenerate(appId, keyId);
      setNewlyCreatedKey(updatedKey);
      fetchKeys();
    } catch (err) {
      alert(`Failed to regenerate key: ${getErrorMessage(err)}`);
    } finally {
      setRegeneratingKeyId(null);
    }
  };

  const handleDeleteKey = async (keyId: number) => {
    try {
      await apiKeysApi.delete(appId, keyId);
      setDeletingKeyId(null);
      fetchKeys();
    } catch (err) {
      alert(`Failed to delete key: ${getErrorMessage(err)}`);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading application details..." />;
  }

  if (error || !application) {
    return (
      <div>
        <ErrorBanner message={error || 'Application not found'} onRetry={fetchApplication} />
        <Link to="/applications" className="btn btn-secondary" style={{ marginTop: '1rem' }}>
          <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
          Back to Applications
        </Link>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Breadcrumb & Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <Link to="/applications" className="btn btn-secondary btn-sm">
            <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
          </Link>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <h1 className="page-title" style={{ margin: 0 }}>
                {application.name}
              </h1>
              <StatusBadge status={application.healthStatus} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '0.25rem', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <Globe style={{ width: '0.875rem', height: '0.875rem' }} />
                {application.baseUrl}
              </span>
              <span>•</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <Clock style={{ width: '0.875rem', height: '0.875rem' }} />
                Last active: {application.lastSeenAt ? new Date(application.lastSeenAt).toLocaleString() : 'Never'}
              </span>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button onClick={() => setIsEditModalOpen(true)} className="btn btn-secondary btn-sm">
            <Edit2 style={{ width: '0.875rem', height: '0.875rem' }} />
            Edit Settings
          </button>
          <button onClick={() => setIsDeleteConfirmOpen(true)} className="btn btn-danger btn-sm">
            <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
            Delete
          </button>
        </div>
      </div>

      {/* Tabs Bar */}
      <div
        style={{
          display: 'flex',
          borderBottom: '1px solid var(--border-color)',
          gap: '0.5rem',
          overflowX: 'auto',
          paddingBottom: '2px',
        }}
      >
        <button
          onClick={() => setActiveTab('overview')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'overview' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'overview' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <LayoutDashboard style={{ width: '1rem', height: '1rem' }} />
          Overview
        </button>

        <button
          onClick={() => setActiveTab('apis')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'apis' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'apis' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <Compass style={{ width: '1rem', height: '1rem' }} />
          API Catalog ({endpoints.length})
        </button>

        <button
          onClick={() => setActiveTab('requests')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'requests' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'requests' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <FileText style={{ width: '1rem', height: '1rem' }} />
          Request Logs
        </button>

        <button
          onClick={() => setActiveTab('metrics')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'metrics' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'metrics' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <BarChart3 style={{ width: '1rem', height: '1rem' }} />
          Metrics & Latency
        </button>

        <button
          onClick={() => setActiveTab('errors')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'errors' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'errors' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <AlertOctagon style={{ width: '1rem', height: '1rem' }} />
          Error Analytics
        </button>

        <button
          onClick={() => setActiveTab('policies')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'policies' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'policies' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <Sliders style={{ width: '1rem', height: '1rem' }} />
          Traffic Policies
        </button>

        <button
          onClick={() => setActiveTab('alerts')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'alerts' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'alerts' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <ShieldAlert style={{ width: '1rem', height: '1rem' }} />
          Alert Rules
        </button>

        <button
          onClick={() => setActiveTab('keys')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'keys' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'keys' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <KeyRound style={{ width: '1rem', height: '1rem' }} />
          API Keys ({keys.length})
        </button>

        <button
          onClick={() => setActiveTab('audit')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            padding: '0.75rem 1rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: activeTab === 'audit' ? 'var(--primary)' : 'var(--text-muted)',
            borderBottom: activeTab === 'audit' ? '2px solid var(--primary)' : '2px solid transparent',
            background: 'none',
            borderTop: 'none',
            borderLeft: 'none',
            borderRight: 'none',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <History style={{ width: '1rem', height: '1rem' }} />
          Audit Logs
        </button>
      </div>

      {/* Tab Contents */}
      {activeTab === 'overview' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Phase 3 Circuit Breaker Live Status */}
          <CircuitBreakerWidget applicationId={appId} />

          {/* Connection & API Access Panel */}
          <ConnectionAccessCard
            application={application}
            keys={keys}
            endpointsCount={endpoints.length}
            onOpenImportModal={() => setIsOpenApiModalOpen(true)}
            onOpenTestConsole={() => setIsConsoleOpen(true)}
            onOpenCreateKeyModal={() => setIsCreateKeyModalOpen(true)}
            onOpenKeysTab={() => setActiveTab('keys')}
          />

          <ConnectionTestCard
            application={application}
            onStatusUpdated={(result) => {
              setApplication({ ...application, healthStatus: result.status, lastSeenAt: result.checkedAt });
              fetchMetrics();
            }}
          />

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
            <div className="card">
              <div className="card-header">
                <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>Service Health</h4>
                <StatusBadge status={application.healthStatus} />
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                Monitored via non-intrusive target probes
              </p>
              <div style={{ fontSize: '0.8125rem' }}>
                <strong>Connection Mode:</strong> {application.connectionMode}
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>API Catalog</h4>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
                  {endpoints.length} APIs
                </span>
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                Discovered automatically from live gateway traffic & OpenAPI specs
              </p>
              <button onClick={() => setActiveTab('apis')} className="btn btn-secondary btn-sm" style={{ width: '100%' }}>
                View Discovered Endpoints
              </button>
            </div>

            <div className="card">
              <div className="card-header">
                <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>Upstream Authentication</h4>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, padding: '0.2rem 0.5rem', borderRadius: 'var(--radius-sm)', backgroundColor: application.upstreamAuth?.configured ? 'var(--success-light)' : 'var(--bg-subtle)', color: application.upstreamAuth?.configured ? 'var(--success-text)' : 'var(--text-muted)' }}>
                  {application.upstreamAuth?.type || 'NONE'}
                </span>
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                {application.upstreamAuth?.configured 
                  ? `Authenticated as ${application.upstreamAuth.type} (${application.upstreamAuth.maskedSecret || 'Configured'})`
                  : 'No upstream authentication configured (Public endpoint)'}
              </p>
              <button onClick={() => setIsUpstreamAuthModalOpen(true)} className="btn btn-secondary btn-sm" style={{ width: '100%' }}>
                <ShieldCheck style={{ width: '0.875rem', height: '0.875rem' }} />
                {application.upstreamAuth?.configured ? 'Rotate / Update Auth' : 'Configure Upstream Auth'}
              </button>
            </div>

            <div className="card">
              <div className="card-header">
                <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>Access Keys</h4>
                <span style={{ fontSize: '0.875rem', fontWeight: 600 }}>{keys.length} Active</span>
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                Scoped SHA-256 hashed keys with per-key rate limits
              </p>
              <button onClick={() => setActiveTab('keys')} className="btn btn-secondary btn-sm" style={{ width: '100%' }}>
                Manage API Keys
              </button>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'apis' && (
        <ApiCatalogTab
          applicationId={appId}
          applicationBaseUrl={application?.baseUrl}
          endpoints={endpoints}
          loading={endpointsLoading}
          onRefresh={fetchEndpoints}
        />
      )}

      {activeTab === 'requests' && (
        <RequestLogsTable
          logsData={logs}
          loading={logsLoading}
          page={logsPage}
          onPageChange={(p) => setLogsPage(p)}
        />
      )}

      {activeTab === 'metrics' && (
        <MetricsOverview applicationId={appId} metrics={metrics} />
      )}

      {activeTab === 'errors' && (
        <ErrorAnalyticsTab applicationId={appId} />
      )}

      {activeTab === 'policies' && (
        <PolicyTab applicationId={appId} endpoints={endpoints} />
      )}

      {activeTab === 'alerts' && (
        <AlertsTab applicationId={appId} endpoints={endpoints} />
      )}

      {activeTab === 'keys' && (
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Application API Keys & Consumers</h3>
              <p className="card-subtitle">Generate and manage scoped API keys, monitor per-consumer traffic and latency percentiles</p>
            </div>
            <button onClick={() => setIsCreateKeyModalOpen(true)} className="btn btn-primary btn-sm">
              <Plus style={{ width: '0.875rem', height: '0.875rem' }} />
              Generate New Key
            </button>
          </div>

          {keysLoading ? (
            <div style={{ padding: '2rem' }}>
              <LoadingSpinner message="Loading API keys..." />
            </div>
          ) : keys.length === 0 ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              No API keys generated for this application yet.
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Key Identifier</th>
                    <th>Rate Limit</th>
                    <th>Status</th>
                    <th>Expires</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {keys.map((k) => (
                    <tr key={k.id}>
                      <td style={{ fontWeight: 600 }}>{k.name}</td>
                      <td>
                        <span
                          style={{
                            fontFamily: 'monospace',
                            fontSize: '0.75rem',
                            padding: '0.2rem 0.4rem',
                            background: 'var(--surface-subtle, #f1f5f9)',
                            borderRadius: '0.25rem',
                          }}
                        >
                          {k.maskedKey || `sk_••••••••${k.id}`}
                        </span>
                      </td>
                      <td>{k.rateLimitPerMinute} req/min</td>
                      <td>
                        <span
                          style={{
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            color: k.active ? 'var(--success)' : 'var(--danger)',
                          }}
                        >
                          {k.active ? '● Active' : '○ Revoked'}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                        {k.expiresAt ? new Date(k.expiresAt).toLocaleDateString() : 'Never'}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                          <button
                            onClick={() => setSelectedKeyForAnalytics(k)}
                            className="btn btn-secondary btn-sm flex items-center gap-1"
                            title="Consumer Analytics"
                          >
                            <Activity style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
                            <span>Usage</span>
                          </button>
                          <button
                            onClick={() => setEditingKey(k)}
                            className="btn btn-secondary btn-sm"
                            title="Edit Key"
                          >
                            <Edit2 style={{ width: '0.875rem', height: '0.875rem' }} />
                          </button>
                          <button
                            onClick={() => handleRegenerateKey(k.id)}
                            disabled={regeneratingKeyId === k.id}
                            className="btn btn-secondary btn-sm"
                            title="Regenerate Key Secret"
                          >
                            <RotateCw style={{ width: '0.875rem', height: '0.875rem' }} />
                          </button>
                          {k.active && (
                            <button
                              onClick={() => setRevokingKeyId(k.id)}
                              className="btn btn-secondary btn-sm"
                              style={{ color: 'var(--warning)' }}
                              title="Revoke Key"
                            >
                              Revoke
                            </button>
                          )}
                          <button
                            onClick={() => setDeletingKeyId(k.id)}
                            className="btn btn-secondary btn-sm"
                            style={{ color: 'var(--danger)' }}
                            title="Delete Key"
                          >
                            <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === 'audit' && (
        <AuditLogTab applicationId={appId} />
      )}

      {/* Edit App Modal */}
      {isEditModalOpen && (
        <EditApplicationModal
          application={application}
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          onSuccess={(updated) => {
            setApplication(updated);
            setIsEditModalOpen(false);
          }}
        />
      )}

      {/* Delete App Confirm */}
      <ConfirmDialog
        isOpen={isDeleteConfirmOpen}
        title="Delete Application"
        message={`Are you sure you want to permanently delete "${application.name}"? All associated API keys, telemetry logs, policies, and alert rules will also be deleted.`}
        confirmLabel={deleting ? 'Deleting...' : 'Delete Application'}
        cancelLabel="Cancel"
        isDangerous={true}
        onConfirm={handleDelete}
        onClose={() => setIsDeleteConfirmOpen(false)}
      />

      {/* Create Key Modal */}
      <CreateKeyModal
        isOpen={isCreateKeyModalOpen}
        applicationId={appId}
        onClose={() => setIsCreateKeyModalOpen(false)}
        onSuccess={(created) => {
          setIsCreateKeyModalOpen(false);
          setNewlyCreatedKey(created);
          fetchKeys();
        }}
      />

      {/* Edit Key Modal */}
      {editingKey && (
        <EditKeyModal
          applicationId={appId}
          apiKey={editingKey}
          isOpen={!!editingKey}
          onClose={() => setEditingKey(null)}
          onSuccess={() => {
            setEditingKey(null);
            fetchKeys();
          }}
        />
      )}

      {/* Show Key Created/Regenerated Secret Modal */}
      {newlyCreatedKey && (
        <ApiKeyModal
          apiKey={newlyCreatedKey}
          isOpen={!!newlyCreatedKey}
          onClose={() => setNewlyCreatedKey(null)}
        />
      )}

      {/* Revoke Key Confirm */}
      <ConfirmDialog
        isOpen={revokingKeyId !== null}
        title="Revoke API Key"
        message="Are you sure you want to revoke this API key? Any client requests using this key will immediately be rejected."
        confirmLabel="Revoke Key"
        cancelLabel="Cancel"
        isDangerous={true}
        onConfirm={() => revokingKeyId && handleRevokeKey(revokingKeyId)}
        onClose={() => setRevokingKeyId(null)}
      />

      {/* Delete Key Confirm */}
      <ConfirmDialog
        isOpen={deletingKeyId !== null}
        title="Delete API Key"
        message="Are you sure you want to permanently delete this API key?"
        confirmLabel="Delete Key"
        cancelLabel="Cancel"
        isDangerous={true}
        onConfirm={() => deletingKeyId && handleDeleteKey(deletingKeyId)}
        onClose={() => setDeletingKeyId(null)}
      />

      {/* Upstream Auth Modal */}
      {application && (
        <UpstreamAuthModal
          isOpen={isUpstreamAuthModalOpen}
          application={application}
          onClose={() => setIsUpstreamAuthModalOpen(false)}
          onSuccess={(updated) => {
            setApplication(updated);
          }}
        />
      )}

      {/* Phase 3 Consumer Analytics Drawer */}
      <ConsumerAnalyticsDrawer
        isOpen={!!selectedKeyForAnalytics}
        applicationId={appId}
        keyId={selectedKeyForAnalytics?.id || null}
        keyName={selectedKeyForAnalytics?.name || ''}
        onClose={() => setSelectedKeyForAnalytics(null)}
      />

      {/* Simplified Import & Auto-Discover Modal */}
      {application && (
        <ConnectApplicationModal
          isOpen={isOpenApiModalOpen}
          onClose={() => setIsOpenApiModalOpen(false)}
          initialAppName={application.name}
          initialUrl={application.baseUrl}
          onSuccess={() => {
            fetchEndpoints();
            fetchMetrics();
          }}
        />
      )}

      {/* Developer API Test Console Modal */}
      {application && isConsoleOpen && (
        <ApiTestConsoleModal
          isOpen={isConsoleOpen}
          onClose={() => setIsConsoleOpen(false)}
          applicationId={appId}
          initialEndpoint={endpoints[0] || null}
          onExecuted={() => {
            fetchEndpoints();
            fetchMetrics();
          }}
        />
      )}
    </div>
  );
};
