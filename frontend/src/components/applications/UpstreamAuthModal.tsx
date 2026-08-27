import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { applicationsApi } from '../../api/applications';
import { Application, UpstreamAuthType, UpstreamAuthConfigRequest } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import { AlertCircle, Lock, ShieldCheck, Trash2 } from 'lucide-react';

interface UpstreamAuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  application: Application;
  onSuccess: (updatedApp: Application) => void;
}

export const UpstreamAuthModal: React.FC<UpstreamAuthModalProps> = ({
  isOpen,
  onClose,
  application,
  onSuccess,
}) => {
  const currentAuth = application.upstreamAuth;
  const [authType, setAuthType] = useState<UpstreamAuthType>(currentAuth?.type || 'NONE');
  const [headerName, setHeaderName] = useState(currentAuth?.headerName || 'X-API-Key');
  const [queryParamName, setQueryParamName] = useState(currentAuth?.queryParamName || 'apiKey');
  const [secret, setSecret] = useState('');
  const [username, setUsername] = useState(currentAuth?.username || '');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (authType === 'API_KEY_HEADER' && (!headerName.trim() || (!secret.trim() && !currentAuth?.configured))) {
      setError('Header name and API Key secret are required.');
      return;
    }
    if (authType === 'API_KEY_QUERY' && (!queryParamName.trim() || (!secret.trim() && !currentAuth?.configured))) {
      setError('Query param name and API Key secret are required.');
      return;
    }
    if (authType === 'BEARER_TOKEN' && !secret.trim() && !currentAuth?.configured) {
      setError('Bearer token is required.');
      return;
    }
    if (authType === 'BASIC_AUTH' && (!username.trim() || (!password.trim() && !currentAuth?.configured))) {
      setError('Username and password are required.');
      return;
    }
    if (authType === 'CUSTOM_HEADER' && (!headerName.trim() || (!secret.trim() && !currentAuth?.configured))) {
      setError('Header name and value are required.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      if (authType === 'NONE') {
        await applicationsApi.deleteUpstreamAuth(application.id);
        const updated = await applicationsApi.getById(application.id);
        onSuccess(updated);
        onClose();
        return;
      }

      const req: UpstreamAuthConfigRequest = {
        type: authType,
        enabled: true,
        headerName: (authType === 'API_KEY_HEADER' || authType === 'CUSTOM_HEADER') ? headerName.trim() : undefined,
        queryParamName: authType === 'API_KEY_QUERY' ? queryParamName.trim() : undefined,
        secret: secret.trim() ? secret.trim() : undefined,
        username: authType === 'BASIC_AUTH' ? username.trim() : undefined,
        password: password.trim() ? password.trim() : undefined,
      };

      await applicationsApi.updateUpstreamAuth(application.id, req);
      const updated = await applicationsApi.getById(application.id);
      onSuccess(updated);
      onClose();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDisable = async () => {
    setError(null);
    setLoading(true);
    try {
      await applicationsApi.deleteUpstreamAuth(application.id);
      const updated = await applicationsApi.getById(application.id);
      onSuccess(updated);
      onClose();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Configure Upstream Authentication">
      {error && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', padding: '0.75rem 1rem', backgroundColor: 'var(--danger-light)', border: '1px solid var(--danger-border)', borderRadius: 'var(--radius-md)', color: 'var(--danger-text)', fontSize: '0.8125rem', marginBottom: '1.25rem' }}>
          <AlertCircle style={{ width: '1.125rem', height: '1.125rem', flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSave}>
        <div style={{ marginBottom: '1.25rem' }}>
          <label className="form-label">Upstream Authentication Scheme</label>
          <select
            className="form-input"
            value={authType}
            onChange={(e) => setAuthType(e.target.value as UpstreamAuthType)}
          >
            <option value="NONE">None (Public Upstream)</option>
            <option value="BEARER_TOKEN">Bearer Token (Authorization: Bearer ...)</option>
            <option value="API_KEY_HEADER">API Key Header (e.g. X-API-Key)</option>
            <option value="API_KEY_QUERY">API Key Query Parameter (?apiKey=...)</option>
            <option value="BASIC_AUTH">Basic Authentication (username/password)</option>
            <option value="CUSTOM_HEADER">Custom Header (Custom header name & value)</option>
          </select>
        </div>

        {authType === 'BEARER_TOKEN' && (
          <div className="form-group">
            <label className="form-label">
              Bearer Token {currentAuth?.configured && <span style={{ color: 'var(--text-muted)' }}>(Leave empty to keep existing: {currentAuth.maskedSecret})</span>}
            </label>
            <input
              type="password"
              className="form-input"
              placeholder="Enter new token to rotate"
              value={secret}
              onChange={(e) => setSecret(e.target.value)}
            />
          </div>
        )}

        {authType === 'API_KEY_HEADER' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Header Name</label>
              <input
                type="text"
                required
                className="form-input"
                value={headerName}
                onChange={(e) => setHeaderName(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="form-label">
                API Key {currentAuth?.configured && <span style={{ color: 'var(--text-muted)' }}>({currentAuth.maskedSecret})</span>}
              </label>
              <input
                type="password"
                className="form-input"
                placeholder="Enter new secret"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
              />
            </div>
          </div>
        )}

        {authType === 'API_KEY_QUERY' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Param Name</label>
              <input
                type="text"
                required
                className="form-input"
                value={queryParamName}
                onChange={(e) => setQueryParamName(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="form-label">
                API Key {currentAuth?.configured && <span style={{ color: 'var(--text-muted)' }}>({currentAuth.maskedSecret})</span>}
              </label>
              <input
                type="password"
                className="form-input"
                placeholder="Enter new secret"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
              />
            </div>
          </div>
        )}

        {authType === 'BASIC_AUTH' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                type="text"
                required
                className="form-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-input"
                placeholder={currentAuth?.configured ? "••••••••" : "Password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>
        )}

        {authType === 'CUSTOM_HEADER' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
            <div className="form-group">
              <label className="form-label">Header Name</label>
              <input
                type="text"
                required
                className="form-input"
                value={headerName}
                onChange={(e) => setHeaderName(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="form-label">
                Header Value {currentAuth?.configured && <span style={{ color: 'var(--text-muted)' }}>({currentAuth.maskedSecret})</span>}
              </label>
              <input
                type="password"
                className="form-input"
                placeholder="Enter new value"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
              />
            </div>
          </div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          <Lock style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
          <span>Encrypted with AES-256-GCM. Never exposed in API responses or logs.</span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
          {currentAuth && currentAuth.type !== 'NONE' ? (
            <button
              type="button"
              onClick={handleDisable}
              disabled={loading}
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--danger)' }}
            >
              <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} /> Disable Auth
            </button>
          ) : <div />}

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary btn-sm" disabled={loading}>
              Cancel
            </button>
            <button type="submit" disabled={loading} className="btn btn-primary btn-sm">
              <ShieldCheck style={{ width: '0.875rem', height: '0.875rem' }} />
              {loading ? 'Saving...' : 'Save Upstream Auth'}
            </button>
          </div>
        </div>
      </form>
    </Modal>
  );
};
