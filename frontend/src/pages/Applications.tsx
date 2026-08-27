import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { Application, HealthStatus } from '../types/application';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { CreateApplicationModal } from '../components/applications/CreateApplicationModal';
import { ConnectApplicationModal } from '../components/applications/ConnectApplicationModal';
import { getErrorMessage } from '../api/client';
import {
  Layers,
  Plus,
  Search,
  Globe,
  Trash2,
  KeyRound,
  Activity,
} from 'lucide-react';

export const Applications: React.FC = () => {
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<HealthStatus | 'ALL'>('ALL');

  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [appToDelete, setAppToDelete] = useState<Application | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchApplications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await applicationsApi.list();
      setApplications(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const handleDeleteConfirm = async () => {
    if (!appToDelete) return;
    setDeleting(true);
    try {
      await applicationsApi.delete(appToDelete.id);
      setApplications(applications.filter((a) => a.id !== appToDelete.id));
      setAppToDelete(null);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeleting(false);
    }
  };

  const filteredApps = applications.filter((app) => {
    const matchesSearch =
      app.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      app.baseUrl.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (app.description && app.description.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || app.healthStatus === statusFilter;

    return matchesSearch && matchesStatus;
  });

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div>
          <h1 className="page-title">Applications</h1>
          <p className="page-desc">
            Connect your application once using its name, backend URL, and Sentinel API key. Sentinel automatically checks the backend and discovers its APIs.
          </p>
        </div>
        <button
          onClick={() => setIsConnectModalOpen(true)}
          className="btn btn-primary"
        >
          <Plus style={{ width: '1rem', height: '1rem' }} />
          Import Application
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchApplications} />}

      {/* Filter and Search Bar */}
      <div
        className="card"
        style={{
          padding: '0.875rem 1.25rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ position: 'relative', flex: '1', minWidth: '240px', maxWidth: '400px' }}>
          <Search
            style={{
              position: 'absolute',
              left: '0.75rem',
              top: '50%',
              transform: 'translateY(-50%)',
              width: '1rem',
              height: '1rem',
              color: 'var(--text-muted)',
            }}
          />
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '2.25rem' }}
            placeholder="Search by name, URL, or description..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Status Filter Buttons */}
        <div style={{ display: 'flex', gap: '0.375rem', flexWrap: 'wrap' }}>
          {(['ALL', 'HEALTHY', 'DEGRADED', 'UNAVAILABLE', 'UNKNOWN'] as const).map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className="btn btn-sm"
              style={{
                backgroundColor: statusFilter === status ? 'var(--primary-light)' : 'var(--bg-surface)',
                color: statusFilter === status ? 'var(--primary)' : 'var(--text-secondary)',
                borderColor: statusFilter === status ? 'var(--primary-border)' : 'var(--border-color)',
                fontWeight: statusFilter === status ? 600 : 500,
              }}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner message="Loading applications..." />
      ) : applications.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="No Registered Applications"
          description="Register your first application to start monitoring health status and receiving gateway telemetry."
          action={{
            label: 'Register First Application',
            onClick: () => setIsCreateModalOpen(true),
          }}
        />
      ) : filteredApps.length === 0 ? (
        <EmptyState
          icon={Search}
          title="No Matching Applications Found"
          description="Try adjusting your search query or status filter criteria."
        />
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Application</th>
                <th>Target Base URL</th>
                <th>Connection Mode</th>
                <th>Last Observed</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredApps.map((app) => (
                <tr key={app.id}>
                  <td>
                    <StatusBadge status={app.healthStatus} size="sm" />
                  </td>
                  <td>
                    <Link
                      to={`/applications/${app.id}`}
                      style={{ fontWeight: 600, color: 'var(--text-primary)', textDecoration: 'none' }}
                    >
                      {app.name}
                    </Link>
                    {app.description && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {app.description}
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem' }}>
                      <Globe style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)' }} />
                      <span style={{ wordBreak: 'break-all' }}>{app.baseUrl}</span>
                    </div>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--primary)' }}>
                      {app.connectionMode}
                    </span>
                  </td>
                  <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    {app.lastSeenAt ? new Date(app.lastSeenAt).toLocaleString() : 'Never (UNKNOWN)'}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.5rem' }}>
                      <Link
                        to={`/applications/${app.id}`}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.3rem 0.6rem' }}
                        title="View Details & Probe"
                      >
                        <Activity style={{ width: '0.875rem', height: '0.875rem' }} />
                        Inspect
                      </Link>
                      <Link
                        to={`/applications/${app.id}/keys`}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.3rem 0.6rem' }}
                        title="API Keys"
                      >
                        <KeyRound style={{ width: '0.875rem', height: '0.875rem' }} />
                        Keys
                      </Link>
                      <button
                        onClick={() => setAppToDelete(app)}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.3rem 0.5rem', color: 'var(--danger)' }}
                        title="Delete Application"
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

      {/* Modals */}
      <ConnectApplicationModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
        onSuccess={() => {
          setIsConnectModalOpen(false);
          fetchApplications();
        }}
      />

      <CreateApplicationModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(newApp) => {
          setIsCreateModalOpen(false);
          setApplications([newApp, ...applications]);
        }}
      />

      <ConfirmDialog
        isOpen={!!appToDelete}
        onClose={() => setAppToDelete(null)}
        onConfirm={handleDeleteConfirm}
        title="Delete Application"
        message={`Are you sure you want to delete "${appToDelete?.name}"? All associated API keys, request logs, and telemetry will be permanently deleted.`}
        confirmLabel="Delete Application"
        isDangerous={true}
        isLoading={deleting}
      />
    </div>
  );
};
