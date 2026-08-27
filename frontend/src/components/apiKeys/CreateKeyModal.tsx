import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { apiKeysApi } from '../../api/apiKeys';
import { ApiKey } from '../../types/apiKey';
import { getErrorMessage } from '../../api/client';
import { AlertCircle, KeyRound } from 'lucide-react';

interface CreateKeyModalProps {
  isOpen: boolean;
  applicationId: number;
  onClose: () => void;
  onSuccess: (newKey: ApiKey) => void;
}

export const CreateKeyModal: React.FC<CreateKeyModalProps> = ({
  isOpen,
  applicationId,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [rateLimit, setRateLimit] = useState<number>(60);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const resetForm = () => {
    setName('');
    setRateLimit(60);
    setError(null);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Key name is required.');
      return;
    }

    if (rateLimit < 1) {
      setError('Rate limit must be at least 1 request per minute.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const created = await apiKeysApi.create(applicationId, {
        name: name.trim(),
        rateLimitPerMinute: rateLimit,
      });
      resetForm();
      onSuccess(created);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Generate Application API Key">
      {error && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.625rem',
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--danger-light)',
            border: '1px solid var(--danger-border)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--danger-text)',
            fontSize: '0.8125rem',
            marginBottom: '1.25rem',
          }}
        >
          <AlertCircle style={{ width: '1.125rem', height: '1.125rem', flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label" htmlFor="key-name">
            Key Name / Description *
          </label>
          <input
            id="key-name"
            type="text"
            required
            className="form-input"
            placeholder="e.g. Production Client / Backend Worker"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="key-ratelimit">
            Rate Limit (Requests per minute) *
          </label>
          <input
            id="key-ratelimit"
            type="number"
            min={1}
            max={10000}
            required
            className="form-input"
            value={rateLimit}
            onChange={(e) => setRateLimit(parseInt(e.target.value, 10) || 60)}
          />
          <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Enforced atomically by Sentinel Redis rate limiter.
          </span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
          <button type="button" onClick={handleClose} className="btn btn-secondary btn-sm" disabled={loading}>
            Cancel
          </button>
          <button type="submit" disabled={loading} className="btn btn-primary btn-sm">
            <KeyRound style={{ width: '0.875rem', height: '0.875rem' }} />
            {loading ? 'Generating...' : 'Generate Key'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
