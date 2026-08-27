import React, { useState, useEffect } from 'react';
import { ApiKey, UpdateApiKeyRequest } from '../../types/apiKey';
import { apiKeysApi } from '../../api/apiKeys';
import { Modal } from '../common/Modal';
import { ErrorBanner } from '../common/ErrorBanner';
import { getErrorMessage } from '../../api/client';

interface EditKeyModalProps {
  isOpen: boolean;
  apiKey: ApiKey | null;
  applicationId: number;
  onClose: () => void;
  onSuccess: (updated: ApiKey) => void;
}

export const EditKeyModal: React.FC<EditKeyModalProps> = ({
  isOpen,
  apiKey,
  applicationId,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [rateLimitPerMinute, setRateLimitPerMinute] = useState(60);
  const [active, setActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (apiKey) {
      setName(apiKey.name);
      setRateLimitPerMinute(apiKey.rateLimitPerMinute);
      setActive(apiKey.active);
      setError(null);
    }
  }, [apiKey]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!apiKey) return;

    if (!name.trim()) {
      setError('Key name cannot be empty');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload: UpdateApiKeyRequest = {
        name: name.trim(),
        rateLimitPerMinute: Number(rateLimitPerMinute),
        active,
      };
      const updated = await apiKeysApi.update(applicationId, apiKey.id, payload);
      onSuccess(updated);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  if (!apiKey) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Edit API Key">
      {error && <ErrorBanner message={error} />}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label" htmlFor="edit-key-name">
            Key Name
          </label>
          <input
            id="edit-key-name"
            type="text"
            className="form-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="edit-rate-limit">
            Rate Limit (Requests / Minute)
          </label>
          <input
            id="edit-rate-limit"
            type="number"
            min="1"
            max="100000"
            className="form-input"
            value={rateLimitPerMinute}
            onChange={(e) => setRateLimitPerMinute(parseInt(e.target.value, 10) || 60)}
            required
          />
          <small style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '0.25rem', display: 'block' }}>
            Enforced per-minute quota backed by Redis counters.
          </small>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="edit-status">
            Status
          </label>
          <select
            id="edit-status"
            className="form-input"
            value={active ? 'true' : 'false'}
            onChange={(e) => setActive(e.target.value === 'true')}
          >
            <option value="true">Active (Authenticating)</option>
            <option value="false">Revoked / Inactive (Rejects Traffic)</option>
          </select>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
          <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
