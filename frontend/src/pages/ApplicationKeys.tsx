import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { apiKeysApi } from '../api/apiKeys';
import { Application } from '../types/application';
import { ApiKey } from '../types/apiKey';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { CreateKeyModal } from '../components/apiKeys/CreateKeyModal';
import { EditKeyModal } from '../components/apiKeys/EditKeyModal';
import { ApiKeyModal } from '../components/apiKeys/ApiKeyModal';
import { getErrorMessage } from '../api/client';
import {
  ArrowLeft,
  KeyRound,
  Plus,
  CheckCircle2,
  Shield,
  Edit2,
  Trash2,
  RotateCw,
  ShieldAlert,
  Copy,
  Check,
} from 'lucide-react';

export const ApplicationKeys: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const appId = parseInt(id || '', 10);

  const [application, setApplication] = useState<Application | null>(null);
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [newlyCreatedKey, setNewlyCreatedKey] = useState<ApiKey | null>(null);

  // Key Actions State
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null);
  const [keyToRevoke, setKeyToRevoke] = useState<ApiKey | null>(null);
  const [keyToRegenerate, setKeyToRegenerate] = useState<ApiKey | null>(null);
  const [keyToDelete, setKeyToDelete] = useState<ApiKey | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [copiedKeyId, setCopiedKeyId] = useState<number | null>(null);

  const fetchData = useCallback(async () => {
    if (isNaN(appId)) {
      setError('Invalid Application ID');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const [appData, keysData] = await Promise.all([
        applicationsApi.getById(appId),
        apiKeysApi.list(appId),
      ]);
      setApplication(appData);
      setApiKeys(keysData);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [appId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleRevokeKey = async () => {
    if (!application || !keyToRevoke) return;
    setActionLoading(true);
    try {
      await apiKeysApi.revoke(application.id, keyToRevoke.id);
      setKeyToRevoke(null);
      fetchData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleRegenerateKey = async () => {
    if (!application || !keyToRegenerate) return;
    setActionLoading(true);
    try {
      const newKey = await apiKeysApi.regenerate(application.id, keyToRegenerate.id);
      setKeyToRegenerate(null);
      setNewlyCreatedKey(newKey);
      fetchData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteKey = async () => {
    if (!application || !keyToDelete) return;
    setActionLoading(true);
    try {
      await apiKeysApi.delete(application.id, keyToDelete.id);
      setKeyToDelete(null);
      fetchData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleCopyMaskedKey = (key: ApiKey) => {
    const textToCopy = key.apiKey || key.maskedKey || 'sk_sentinel_••••';
    navigator.clipboard.writeText(textToCopy);
    setCopiedKeyId(key.id);
    setTimeout(() => setCopiedKeyId(null), 2000);
  };

  if (loading) {
    return <LoadingSpinner message="Loading application API keys..." />;
  }

  if (error || !application) {
    return (
      <div>
        <Link to="/applications" className="btn btn-secondary btn-sm" style={{ marginBottom: '1rem' }}>
          <ArrowLeft style={{ width: '0.875rem', height: '0.875rem' }} />
          Back to Applications
        </Link>
        <ErrorBanner message={error || 'Application not found'} onRetry={fetchData} />
      </div>
    );
  }

  return (
    <div>
      {/* Navigation */}
      <div style={{ marginBottom: '1.25rem' }}>
        <Link
          to={`/applications/${application.id}`}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.375rem',
            fontSize: '0.8125rem',
            color: 'var(--text-muted)',
            textDecoration: 'none',
            fontWeight: 500,
          }}
        >
          <ArrowLeft style={{ width: '0.875rem', height: '0.875rem' }} />
          Back to {application.name} Details
        </Link>
      </div>

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
          <h1 className="page-title">API Keys — {application.name}</h1>
          <p className="page-desc">
            Manage scoped gateway API keys with Redis rate limiting and rotation controls
          </p>
        </div>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="btn btn-primary"
        >
          <Plus style={{ width: '1rem', height: '1rem' }} />
          Generate API Key
        </button>
      </div>

      {/* Security Info Card */}
      <div
        className="card"
        style={{
          padding: '1rem 1.25rem',
          backgroundColor: 'var(--primary-light)',
          border: '1px solid var(--primary-border)',
          marginBottom: '1.5rem',
          display: 'flex',
          gap: '0.75rem',
          alignItems: 'flex-start',
        }}
      >
        <Shield style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)', flexShrink: 0, marginTop: '0.125rem' }} />
        <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
          <strong style={{ color: 'var(--text-primary)' }}>Scoped Gateway Authentication:</strong> Requests sent with header <code style={{ fontFamily: 'var(--font-mono)', backgroundColor: '#e2e8f0', padding: '0.1rem 0.3rem', borderRadius: '4px' }}>X-Sentinel-API-Key</code> will automatically be mapped to this application, evaluated against rate limits, and logged for telemetry.
        </div>
      </div>

      {apiKeys.length === 0 ? (
        <div
          style={{
            padding: '3.5rem 1.5rem',
            textAlign: 'center',
            backgroundColor: 'var(--bg-surface)',
            border: '1px dashed var(--border-color)',
            borderRadius: 'var(--radius-lg)',
          }}
        >
          <KeyRound style={{ width: '3rem', height: '3rem', color: 'var(--text-muted)', margin: '0 auto 1rem' }} />
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.375rem' }}>
            No API Keys Created Yet
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', maxWidth: '28rem', margin: '0 auto 1.5rem' }}>
            Generate your first application-scoped API key to allow client applications to communicate through Sentinel.
          </p>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="btn btn-primary btn-sm"
          >
            <Plus style={{ width: '0.875rem', height: '0.875rem' }} />
            Generate First Key
          </button>
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Key Name</th>
                <th>Masked Secret</th>
                <th>Rate Limit</th>
                <th>Status</th>
                <th>Created At</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {apiKeys.map((key) => (
                <tr key={key.id}>
                  <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{key.name}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                      <code style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem' }}>
                        {key.maskedKey || 'sk_sentinel_••••••••'}
                      </code>
                      <button
                        onClick={() => handleCopyMaskedKey(key)}
                        title="Copy key format"
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          padding: '0.25rem',
                          color: 'var(--text-muted)',
                          display: 'flex',
                          alignItems: 'center',
                        }}
                      >
                        {copiedKeyId === key.id ? (
                          <Check style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
                        ) : (
                          <Copy style={{ width: '0.875rem', height: '0.875rem' }} />
                        )}
                      </button>
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-healthy">
                      {key.rateLimitPerMinute} req / min
                    </span>
                  </td>
                  <td>
                    {key.active ? (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.8125rem', color: 'var(--success-text)', fontWeight: 600 }}>
                        <CheckCircle2 style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
                        Active (SHA-256 Hashed)
                      </span>
                    ) : (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.8125rem', color: 'var(--danger-text)', fontWeight: 600 }}>
                        <ShieldAlert style={{ width: '0.875rem', height: '0.875rem', color: 'var(--danger)' }} />
                        Revoked
                      </span>
                    )}
                  </td>
                  <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    {new Date(key.createdAt).toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                      <button
                        onClick={() => setEditingKey(key)}
                        title="Edit key settings"
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem' }}
                      >
                        <Edit2 style={{ width: '0.75rem', height: '0.75rem' }} />
                        Edit
                      </button>

                      {key.active && (
                        <button
                          onClick={() => setKeyToRevoke(key)}
                          title="Revoke key"
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '0.25rem 0.5rem', color: 'var(--warning-text)' }}
                        >
                          <ShieldAlert style={{ width: '0.75rem', height: '0.75rem' }} />
                          Revoke
                        </button>
                      )}

                      <button
                        onClick={() => setKeyToRegenerate(key)}
                        title="Regenerate key secret"
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem' }}
                      >
                        <RotateCw style={{ width: '0.75rem', height: '0.75rem' }} />
                        Rotate
                      </button>

                      <button
                        onClick={() => setKeyToDelete(key)}
                        title="Delete key"
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.25rem 0.5rem', color: 'var(--danger)' }}
                      >
                        <Trash2 style={{ width: '0.75rem', height: '0.75rem' }} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Key Modal */}
      <CreateKeyModal
        isOpen={isCreateModalOpen}
        applicationId={application.id}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(newKey) => {
          setIsCreateModalOpen(false);
          setNewlyCreatedKey(newKey);
          fetchData();
        }}
      />

      {/* Edit Key Modal */}
      <EditKeyModal
        isOpen={!!editingKey}
        apiKey={editingKey}
        applicationId={application.id}
        onClose={() => setEditingKey(null)}
        onSuccess={() => {
          setEditingKey(null);
          fetchData();
        }}
      />

      {/* Revoke Key Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!keyToRevoke}
        onClose={() => setKeyToRevoke(null)}
        onConfirm={handleRevokeKey}
        title="Revoke API Key"
        message={`Are you sure you want to revoke "${keyToRevoke?.name}"? It will immediately stop authenticating gateway requests.`}
        confirmLabel="Revoke Key"
        isDangerous={true}
        isLoading={actionLoading}
      />

      {/* Regenerate Key Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!keyToRegenerate}
        onClose={() => setKeyToRegenerate(null)}
        onConfirm={handleRegenerateKey}
        title="Regenerate API Key"
        message={`Regenerating "${keyToRegenerate?.name}" will immediately invalidate the current secret and produce a new raw key. Are you sure you want to proceed?`}
        confirmLabel="Regenerate Key"
        isDangerous={false}
        isLoading={actionLoading}
      />

      {/* Delete Key Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!keyToDelete}
        onClose={() => setKeyToDelete(null)}
        onConfirm={handleDeleteKey}
        title="Delete API Key"
        message={`Are you sure you want to permanently delete "${keyToDelete?.name}"?`}
        confirmLabel="Delete Key"
        isDangerous={true}
        isLoading={actionLoading}
      />

      {/* Display Raw Key ONCE */}
      <ApiKeyModal
        isOpen={!!newlyCreatedKey}
        apiKey={newlyCreatedKey}
        onClose={() => setNewlyCreatedKey(null)}
      />
    </div>
  );
};
