import React from 'react';
import { Modal } from './Modal';
import { AlertCircle } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDangerous?: boolean;
  isLoading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  isDangerous = false,
  isLoading = false,
}) => {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth="440px">
      <div style={{ display: 'flex', gap: '0.875rem', alignItems: 'flex-start', marginBottom: '1.5rem' }}>
        {isDangerous && (
          <div
            style={{
              backgroundColor: 'var(--danger-light)',
              borderRadius: '50%',
              padding: '0.5rem',
              color: 'var(--danger)',
              flexShrink: 0,
            }}
          >
            <AlertCircle style={{ width: '1.5rem', height: '1.5rem' }} />
          </div>
        )}
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
          {message}
        </p>
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
        <button
          type="button"
          onClick={onClose}
          disabled={isLoading}
          className="btn btn-secondary btn-sm"
        >
          {cancelLabel}
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={isLoading}
          className={`btn btn-sm ${isDangerous ? 'btn-danger' : 'btn-primary'}`}
        >
          {isLoading ? 'Processing...' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
};
