import React, { useState } from 'react';
import {
  Sparkles,
  UploadCloud,
  FileCode,
  X,
  ShieldAlert,
  Play,
  Key,
  CheckCircle2,
  Trash2,
} from 'lucide-react';
import { AiTestSession } from '../../types/conversation';
import { ApiKey } from '../../types/apiKey';

interface AiTestInputModalProps {
  isOpen: boolean;
  onClose: () => void;
  session: AiTestSession | null;
  apiKeys: ApiKey[];
  onProvideInput: (data: {
    inputKey?: string;
    inputValue?: string;
    fileBase64?: string;
    fileName?: string;
    fileContentType?: string;
  }) => Promise<void>;
  onContinueTest: (approveDestructive: boolean) => Promise<void>;
  onCancelTest: () => Promise<void>;
  loading?: boolean;
}

export const AiTestInputModal: React.FC<AiTestInputModalProps> = ({
  isOpen,
  onClose,
  session,
  apiKeys,
  onProvideInput,
  onContinueTest,
  onCancelTest,
  loading = false,
}) => {
  const [selectedFile, setSelectedFile] = useState<{
    base64: string;
    name: string;
    size: number;
    type: string;
  } | null>(null);

  const [customKey, setCustomKey] = useState('');
  const [approveDestructive, setApproveDestructive] = useState(true);

  if (!isOpen || !session) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async () => {
      const result = reader.result as string;
      const base64 = result.split(',')[1] || result;
      const fileData = {
        base64,
        name: file.name,
        size: file.size,
        type: file.type || 'image/png',
      };
      setSelectedFile(fileData);
      await onProvideInput({
        inputKey: 'file_base64',
        inputValue: base64,
        fileBase64: base64,
        fileName: file.name,
        fileContentType: file.type || 'image/png',
      });
    };
    reader.readAsDataURL(file);
  };

  const handleApiKeySubmit = async () => {
    if (!customKey.trim()) return;
    await onProvideInput({
      inputKey: 'apiKey',
      inputValue: customKey.trim(),
    });
    setCustomKey('');
  };

  const missingInputs = session.missingInputs || [];
  const isReady = missingInputs.length === 0;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.5)',
        backdropFilter: 'blur(3px)',
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
      }}
      onClick={onClose} // Clicking outside closes the modal UI without destroying session
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '560px',
          backgroundColor: '#ffffff',
          borderRadius: 'var(--radius-lg)',
          boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
          padding: '1.5rem',
          maxHeight: '90vh',
          overflowY: 'auto',
          position: 'relative',
        }}
        onClick={(e) => e.stopPropagation()} // Prevent outside click from propagating
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <div
              style={{
                width: '2.25rem',
                height: '2.25rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: '#eff6ff',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Sparkles style={{ width: '1.25rem', height: '1.25rem' }} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.0625rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                Autonomous AI Test Engine
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: 0 }}>
                Session: <code>{session.sessionId}</code> ({session.applicationName})
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.25rem', color: 'var(--text-muted)' }}
            title="Close popup (Session will be preserved)"
          >
            <X style={{ width: '1.125rem', height: '1.125rem' }} />
          </button>
        </div>

        {/* Status Callout */}
        <div
          style={{
            backgroundColor: isReady ? '#ecfdf5' : '#fffbeb',
            border: `1px solid ${isReady ? '#a7f3d0' : '#fde68a'}`,
            borderRadius: 'var(--radius-md)',
            padding: '0.875rem 1rem',
            marginBottom: '1.25rem',
            fontSize: '0.8125rem',
            color: isReady ? '#065f46' : '#92400e',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.625rem',
          }}
        >
          {isReady ? (
            <CheckCircle2 style={{ width: '1.125rem', height: '1.125rem', color: '#10b981', flexShrink: 0, marginTop: '0.1rem' }} />
          ) : (
            <ShieldAlert style={{ width: '1.125rem', height: '1.125rem', color: '#f59e0b', flexShrink: 0, marginTop: '0.1rem' }} />
          )}
          <div>
            <strong>{isReady ? 'Ready for Execution' : 'Sentinel needs a few inputs before this test can continue.'}</strong>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.75rem', opacity: 0.9 }}>
              {isReady
                ? 'All required test inputs and credentials are ready. You can now launch the full autonomous pipeline.'
                : 'Please provide the missing inputs below. Sentinel has verified that downstream parameters (like image_id) will be automatically captured.'}
            </p>
          </div>
        </div>

        {/* Missing Inputs List */}
        {!isReady && (
          <div style={{ marginBottom: '1.5rem' }}>
            <h4 style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Required Inputs ({missingInputs.length})
            </h4>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {missingInputs.map((item, idx) => (
                <div
                  key={idx}
                  style={{
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.875rem',
                    backgroundColor: 'var(--bg-subtle)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.375rem' }}>
                    <span style={{ fontWeight: 600, fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
                      {item.inputType === 'FILE' ? '📁 Test File Input' : item.inputType === 'API_KEY' ? '🔑 API Authentication' : `Parameter: ${item.inputKey}`}
                    </span>
                    <span className="pill-badge pill-badge-amber" style={{ fontSize: '0.6875rem' }}>
                      Required
                    </span>
                  </div>

                  <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                    {item.prompt}
                  </p>

                  {/* File Upload Component */}
                  {item.inputType === 'FILE' && (
                    <div>
                      <input
                        type="file"
                        id="modal-file-input"
                        style={{ display: 'none' }}
                        onChange={handleFileUpload}
                      />
                      <label
                        htmlFor="modal-file-input"
                        className="btn btn-secondary btn-sm"
                        style={{ width: '100%', justifyContent: 'center', cursor: 'pointer', gap: '0.5rem' }}
                      >
                        <UploadCloud style={{ width: '1rem', height: '1rem' }} />
                        {selectedFile ? `Replace: ${selectedFile.name}` : 'Upload Test Image (.png / .jpg)'}
                      </label>
                    </div>
                  )}

                  {/* API Key Input */}
                  {item.inputType === 'API_KEY' && (
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <input
                        type="password"
                        className="form-input"
                        placeholder="Enter API Key / Token..."
                        value={customKey}
                        onChange={(e) => setCustomKey(e.target.value)}
                        style={{ fontSize: '0.8125rem', flex: 1 }}
                      />
                      <button className="btn btn-primary btn-sm" onClick={handleApiKeySubmit}>
                        Set Key
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Provided Context Display */}
        {(selectedFile || session.fileBase64 || Object.keys(session.providedInputs || {}).length > 0) && (
          <div style={{ marginBottom: '1.25rem' }}>
            <h4 style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Inputs Received
            </h4>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.375rem' }}>
              {(selectedFile || session.fileBase64) && (
                <div className="console-file-chip" style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>
                  <FileCode style={{ width: '0.875rem', height: '0.875rem', color: 'var(--primary)' }} />
                  <span>{selectedFile?.name || session.fileName || 'sentinel_test_image.png'}</span>
                </div>
              )}
              {apiKeys.length > 0 && (
                <div className="console-file-chip" style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>
                  <Key style={{ width: '0.875rem', height: '0.875rem', color: '#10b981' }} />
                  <span>Active Developer API Key Attached</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Destructive Approval Checkbox */}
        <div style={{ marginBottom: '1.5rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)' }}>
          <label style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', cursor: 'pointer', fontSize: '0.8125rem', color: 'var(--text-primary)' }}>
            <input
              type="checkbox"
              checked={approveDestructive}
              onChange={(e) => setApproveDestructive(e.target.checked)}
              style={{ marginTop: '0.15rem' }}
            />
            <span>
              <strong>Approve state-changing operations:</strong> Grant permission for clean/transform steps during this autonomous run.
            </span>
          </label>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.75rem' }}>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            style={{ color: '#ef4444', gap: '0.25rem' }}
            onClick={onCancelTest}
          >
            <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
            Cancel Test
          </button>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onClose}
            >
              Close (Keep Session)
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={loading}
              onClick={() => onContinueTest(approveDestructive)}
              style={{ gap: '0.375rem' }}
            >
              <Play style={{ width: '0.875rem', height: '0.875rem' }} />
              {loading ? 'Running AI Tests...' : 'Continue Test'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
