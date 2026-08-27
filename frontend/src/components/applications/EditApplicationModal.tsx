import React, { useState, useEffect } from 'react';
import { Modal } from '../common/Modal';
import { applicationsApi } from '../../api/applications';
import { Application } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import { AlertCircle, Save } from 'lucide-react';

interface EditApplicationModalProps {
  isOpen: boolean;
  application: Application | null;
  onClose: () => void;
  onSuccess: (updatedApp: Application) => void;
}

export const EditApplicationModal: React.FC<EditApplicationModalProps> = ({
  isOpen,
  application,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  const [active, setActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (application) {
      setName(application.name);
      setDescription(application.description || '');
      setBaseUrl(application.baseUrl);
      setActive(application.active);
      setError(null);
    }
  }, [application]);

  if (!application) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !baseUrl.trim()) {
      setError('Name and Base URL are required.');
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const updated = await applicationsApi.update(application.id, {
        name: name.trim(),
        description: description.trim() ? description.trim() : undefined,
        baseUrl: baseUrl.trim(),
        active,
      });
      onSuccess(updated);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Edit Application: ${application.name}`}>
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
          <label className="form-label" htmlFor="edit-name">
            Application Name
          </label>
          <input
            id="edit-name"
            type="text"
            required
            className="form-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="edit-url">
            Base Target URL
          </label>
          <input
            id="edit-url"
            type="text"
            required
            className="form-input"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="edit-desc">
            Description
          </label>
          <textarea
            id="edit-desc"
            className="form-textarea"
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <input
            id="edit-active"
            type="checkbox"
            checked={active}
            onChange={(e) => setActive(e.target.checked)}
            style={{ width: '1rem', height: '1rem', accentColor: 'var(--primary)' }}
          />
          <label htmlFor="edit-active" style={{ fontSize: '0.875rem', color: 'var(--text-primary)', cursor: 'pointer' }}>
            Active (Enabled in Sentinel)
          </label>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
          <button type="button" onClick={onClose} className="btn btn-secondary btn-sm" disabled={loading}>
            Cancel
          </button>
          <button type="submit" disabled={loading} className="btn btn-primary btn-sm">
            <Save style={{ width: '0.875rem', height: '0.875rem' }} />
            {loading ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
