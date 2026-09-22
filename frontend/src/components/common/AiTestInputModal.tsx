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
  Minus,
  Maximize2,
  AlertCircle,
} from 'lucide-react';
import { AiTestSession } from '../../types/conversation';
import { ApiKey } from '../../types/apiKey';
import { processImageFile } from '../../utils/imageCompressor';

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
  onContinueTest: (
    approveDestructive: boolean,
    selectedFile?: {
      base64: string;
      name: string;
      type: string;
    } | null
  ) => Promise<void>;
  onCancelTest: () => Promise<void>;
  loading?: boolean;
  errorMessage?: string | null;
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
  errorMessage = null,
}) => {
  const [selectedFile, setSelectedFile] = useState<{
    base64: string;
    name: string;
    size: number;
    type: string;
  } | null>(null);

  const [customKey, setCustomKey] = useState('');
  const [approveDestructive, setApproveDestructive] = useState(true);
  const [isMinimized, setIsMinimized] = useState(false);

  if (!isOpen || !session) return null;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const fileData = await processImageFile(file);
      setSelectedFile(fileData);
      await onProvideInput({
        inputKey: 'file_base64',
        inputValue: fileData.base64,
        fileBase64: fileData.base64,
        fileName: fileData.name,
        fileContentType: fileData.type,
      });
    } catch (err) {
      console.error('Failed to process uploaded file', err);
      alert('Could not process the selected image.');
    }
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

  // Minimized floating card at bottom right
  if (isMinimized) {
    return (
      <div
        style={{
          position: 'fixed',
          bottom: '1.5rem',
          right: '1.5rem',
          zIndex: 100,
          backgroundColor: '#ffffff',
          borderRadius: 'var(--radius-lg)',
          boxShadow: '0 10px 25px -5px rgb(0 0 0 / 0.2), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
          border: '1px solid var(--border-color)',
          padding: '0.875rem 1.25rem',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          maxWidth: '420px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <div
            style={{
              width: '2rem',
              height: '2rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: loading ? '#fef3c7' : '#eff6ff',
              color: loading ? '#d97706' : 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {loading ? (
              <div className="status-dot status-dot-healthy animate-spin" style={{ width: '0.75rem', height: '0.75rem' }} />
            ) : (
              <Sparkles style={{ width: '1.125rem', height: '1.125rem' }} />
            )}
          </div>
          <div>
            <div style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <span>AI Test Engine</span>
              <span
                className={`pill-badge ${loading ? 'pill-badge-amber' : isReady ? 'pill-badge-green' : 'pill-badge-blue'}`}
                style={{ fontSize: '0.65rem', padding: '0.1rem 0.35rem' }}
              >
                {loading ? 'Running Tests...' : isReady ? 'Ready' : 'Input Needed'}
              </span>
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {session.applicationName}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', marginLeft: 'auto' }}>
          <button
            onClick={() => setIsMinimized(false)}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.35rem', color: 'var(--text-secondary)' }}
            title="Maximize AI Test Engine modal"
          >
            <Maximize2 style={{ width: '1rem', height: '1rem' }} />
          </button>
          <button
            onClick={onClose}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.35rem', color: 'var(--text-muted)' }}
            title="Close popup (Session preserved)"
          >
            <X style={{ width: '1rem', height: '1rem' }} />
          </button>
        </div>
      </div>
    );
  }

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

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            <button
              onClick={() => setIsMinimized(true)}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.25rem', color: 'var(--text-muted)' }}
              title="Minimize to floating widget"
            >
              <Minus style={{ width: '1.125rem', height: '1.125rem' }} />
            </button>
            <button
              onClick={onClose}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.25rem', color: 'var(--text-muted)' }}
              title="Close popup (Session will be preserved)"
            >
              <X style={{ width: '1.125rem', height: '1.125rem' }} />
            </button>
          </div>
        </div>

        {/* Error Banner */}
        {errorMessage && (
          <div
            style={{
              backgroundColor: '#fef2f2',
              border: '1px solid #fecaca',
              borderRadius: 'var(--radius-md)',
              padding: '0.75rem 1rem',
              marginBottom: '1rem',
              fontSize: '0.8125rem',
              color: '#b91c1c',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <AlertCircle style={{ width: '1.125rem', height: '1.125rem', flexShrink: 0 }} />
            <span>{errorMessage}</span>
          </div>
        )}

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
                  <span>{selectedFile?.name || session.fileName || 'No file selected'}</span>
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
              onClick={() => {
                const effectiveFile = selectedFile || (session?.fileBase64 ? {
                  base64: session.fileBase64,
                  name: session.fileName || 'uploaded_image.png',
                  size: Math.round((session.fileBase64.length * 3) / 4),
                  type: session.fileContentType || 'image/png',
                } : null);
                onContinueTest(approveDestructive, effectiveFile);
              }}
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
