import React, { useState, useEffect } from 'react';
import { Modal } from '../common/Modal';
import { applicationsApi } from '../../api/applications';
import { ConnectAndDiscoverResponse } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import {
  Sparkles,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Copy,
  Check,
  ShieldCheck,
} from 'lucide-react';

interface ConnectApplicationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (appId: number) => void;
  initialAppName?: string;
  initialUrl?: string;
  initialApiKey?: string;
}

export const ConnectApplicationModal: React.FC<ConnectApplicationModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  initialAppName = '',
  initialUrl = '',
  initialApiKey = '',
}) => {
  const [applicationName, setApplicationName] = useState(initialAppName);
  const [backendUrl, setBackendUrl] = useState(initialUrl);
  const [apiKey, setApiKey] = useState(initialApiKey);

  const [loading, setLoading] = useState(false);
  const [progressStep, setProgressStep] = useState<number>(0);
  const [result, setResult] = useState<ConnectAndDiscoverResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [copiedUrl, setCopiedUrl] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setApplicationName(initialAppName || '');
      setBackendUrl(initialUrl || '');
      setApiKey(initialApiKey || '');
      setResult(null);
      setError(null);
      setProgressStep(0);
      setLoading(false);
    }
  }, [isOpen, initialAppName, initialUrl, initialApiKey]);

  const handleCopy = async (text: string, setter: (val: boolean) => void) => {
    try {
      await navigator.clipboard.writeText(text);
      setter(true);
      setTimeout(() => setter(false), 2000);
    } catch {
      // Fallback
    }
  };

  const handleImportApplication = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!applicationName.trim()) {
      setError('Please enter an application name.');
      return;
    }
    if (!backendUrl.trim()) {
      setError('Please enter the backend URL.');
      return;
    }

    setError(null);
    setLoading(true);
    setProgressStep(1);

    try {
      setTimeout(() => setProgressStep(2), 500);
      setTimeout(() => setProgressStep(3), 1000);

      const res = await applicationsApi.connectAndDiscover({
        applicationName: applicationName.trim(),
        sentinelUrl: backendUrl.trim(),
        apiKey: apiKey.trim() ? apiKey.trim() : undefined,
      });

      setProgressStep(4);
      setResult(res);
    } catch (err: any) {
      setProgressStep(0);
      const rawMsg = getErrorMessage(err);
      setError(
        rawMsg && !rawMsg.includes('Internal') && !rawMsg.includes('Exception')
          ? rawMsg
          : 'Unable to reach the application through the supplied Sentinel connection. Check the backend URL and Sentinel API key.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleFinish = () => {
    if (result && onSuccess) {
      onSuccess(result.applicationId);
    }
    onClose();
  };

  const maskApiKey = (key: string) => {
    if (!key) return '';
    if (key.length <= 16) return key;
    return `${key.slice(0, 12)}••••${key.slice(-4)}`;
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={result ? "✓ APPLICATION CONNECTED" : "Import Application"}
      maxWidth={result ? "640px" : "540px"}
    >
      {!result ? (
        <form onSubmit={handleImportApplication}>
          <div style={{ marginBottom: '1.25rem' }}>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: '0 0 1.25rem', lineHeight: '1.4' }}>
              Connect your application once using its name, backend URL, and Sentinel API key. Sentinel automatically checks the backend and discovers its APIs.
            </p>

            {error && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.75rem',
                  padding: '0.875rem 1rem',
                  backgroundColor: 'var(--danger-light)',
                  border: '1px solid var(--danger-border)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger-text)',
                  fontSize: '0.8125rem',
                  marginBottom: '1.25rem',
                }}
              >
                <AlertCircle style={{ width: '1.25rem', height: '1.25rem', flexShrink: 0, marginTop: '0.125rem' }} />
                <div>
                  <strong>Connection Failed</strong>
                  <div style={{ marginTop: '0.25rem' }}>{error}</div>
                </div>
              </div>
            )}

            {/* 1. Application Name */}
            <div className="form-group">
              <label className="form-label">
                Application Name <span style={{ color: 'var(--danger)' }}>*</span>
              </label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. PixelVault-Clean, My Local API"
                value={applicationName}
                onChange={(e) => setApplicationName(e.target.value)}
                disabled={loading}
                required
              />
            </div>

            {/* 2. Backend URL */}
            <div className="form-group">
              <label className="form-label">
                Backend URL <span style={{ color: 'var(--danger)' }}>*</span>
              </label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. https://pixelvault-clean-api.onrender.com or http://localhost:5000"
                value={backendUrl}
                onChange={(e) => setBackendUrl(e.target.value)}
                disabled={loading}
                required
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginTop: '0.25rem' }}>
                Supports both local environments (e.g. <code>http://localhost:5000</code>) and hosted services.
              </span>
            </div>

            {/* 3. Sentinel API Key */}
            <div className="form-group">
              <label className="form-label">
                Sentinel API Key <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(Optional - will auto-generate if blank)</span>
              </label>
              <input
                type="password"
                className="form-control"
                placeholder="sk_sentinel_****************"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                disabled={loading}
              />
            </div>
          </div>

          {/* Loading Progress State */}
          {loading && (
            <div
              style={{
                padding: '1rem',
                backgroundColor: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <RefreshCw style={{ width: '1rem', height: '1rem', color: 'var(--primary)', animation: 'spin 1s linear infinite' }} />
                <span style={{ fontSize: '0.875rem', fontWeight: 600 }}>
                  Connecting to {applicationName || 'Application'}...
                </span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem', fontSize: '0.8125rem' }}>
                <div style={{ color: progressStep >= 1 ? 'var(--success)' : 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                  {progressStep >= 1 ? <CheckCircle2 style={{ width: '0.875rem', height: '0.875rem' }} /> : '○'} Connection established
                </div>
                <div style={{ color: progressStep >= 2 ? 'var(--success)' : 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                  {progressStep >= 2 ? <CheckCircle2 style={{ width: '0.875rem', height: '0.875rem' }} /> : '○'} Backend health verified
                </div>
                <div style={{ color: progressStep >= 3 ? 'var(--success)' : 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                  {progressStep >= 3 ? <CheckCircle2 style={{ width: '0.875rem', height: '0.875rem' }} /> : '○'} API discovery completed
                </div>
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
            <button type="button" onClick={onClose} className="btn btn-secondary btn-sm" disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary btn-sm" disabled={loading}>
              {loading ? (
                <>
                  <RefreshCw style={{ width: '0.875rem', height: '0.875rem', animation: 'spin 1s linear infinite' }} />
                  Importing...
                </>
              ) : (
                <>
                  <Sparkles style={{ width: '0.875rem', height: '0.875rem' }} />
                  Import Application
                </>
              )}
            </button>
          </div>
        </form>
      ) : (
        /* SUCCESS RESULT SCREEN */
        <div>
          <div style={{ textAlign: 'center', padding: '0.5rem 0 1.25rem' }}>
            <ShieldCheck style={{ width: '3rem', height: '3rem', color: 'var(--success)', margin: '0 auto 0.5rem' }} />
            <h4 style={{ margin: '0 0 0.25rem', fontSize: '1.25rem', color: 'var(--text-primary)', fontWeight: 700 }}>
              {result.applicationName}
            </h4>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
              Backend: <code>{result.backendUrl}</code>
            </p>
          </div>

          {/* Key Details Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, 1fr)',
              gap: '0.75rem',
              padding: '0.875rem',
              backgroundColor: 'var(--bg-subtle)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              marginBottom: '1rem',
            }}
          >
            <div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Health Status</div>
              <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: result.backendHealthy ? 'var(--success)' : 'var(--warning)' }}>
                {result.healthStatus} ✓
              </div>
            </div>
            <div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>APIs Discovered</div>
              <div style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--primary)' }}>
                {result.apisDiscoveredCount} Endpoints
              </div>
            </div>
          </div>

          {/* Sentinel Gateway URL */}
          <div className="form-group" style={{ marginBottom: '0.875rem' }}>
            <label className="form-label" style={{ fontSize: '0.75rem', marginBottom: '0.25rem' }}>Sentinel Gateway URL</label>
            <div style={{ position: 'relative' }}>
              <div className="code-box" style={{ paddingRight: '7rem', color: 'var(--primary)', fontWeight: 600, fontSize: '0.8125rem', fontFamily: 'var(--font-mono)' }}>
                {result.sentinelGatewayUrl}/*
              </div>
              <button
                type="button"
                onClick={() => handleCopy(result.sentinelGatewayUrl, setCopiedUrl)}
                className="btn btn-secondary btn-xs"
                style={{
                  position: 'absolute',
                  right: '0.375rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                }}
              >
                {copiedUrl ? <Check style={{ width: '0.75rem', height: '0.75rem', color: 'var(--success)' }} /> : <Copy style={{ width: '0.75rem', height: '0.75rem' }} />}
                {copiedUrl ? 'Copied' : 'Copy Gateway URL'}
              </button>
            </div>
          </div>

          {/* Sentinel API Key */}
          {result.apiKey && (
            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ fontSize: '0.75rem', marginBottom: '0.25rem' }}>Sentinel API Key</label>
              <div style={{ position: 'relative' }}>
                <div className="code-box" style={{ paddingRight: '6.5rem', fontSize: '0.8125rem', fontFamily: 'var(--font-mono)' }}>
                  {maskApiKey(result.apiKey)}
                </div>
                <button
                  type="button"
                  onClick={() => handleCopy(result.apiKey, setCopiedKey)}
                  className="btn btn-secondary btn-xs"
                  style={{
                    position: 'absolute',
                    right: '0.375rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                  }}
                >
                  {copiedKey ? <Check style={{ width: '0.75rem', height: '0.75rem', color: 'var(--success)' }} /> : <Copy style={{ width: '0.75rem', height: '0.75rem' }} />}
                  {copiedKey ? 'Copied' : 'Copy API Key'}
                </button>
              </div>
            </div>
          )}

          {/* Discovered APIs List */}
          {result.discoveredApis && result.discoveredApis.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.375rem' }}>
                Discovered Endpoints ({result.discoveredApis.length})
              </div>
              <div
                style={{
                  maxHeight: '140px',
                  overflowY: 'auto',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--bg-card)',
                }}
              >
                {result.discoveredApis.map((ep, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.625rem',
                      padding: '0.375rem 0.75rem',
                      borderBottom: idx < result.discoveredApis.length - 1 ? '1px solid var(--border-color)' : 'none',
                      fontSize: '0.75rem',
                    }}
                  >
                    <span
                      style={{
                        padding: '0.125rem 0.375rem',
                        borderRadius: 'var(--radius-sm)',
                        fontWeight: 700,
                        fontSize: '0.6875rem',
                        backgroundColor: ep.method === 'GET' ? 'var(--success-light)' : ep.method === 'POST' ? 'var(--primary-light)' : 'var(--warning-light)',
                        color: ep.method === 'GET' ? 'var(--success-text)' : ep.method === 'POST' ? 'var(--primary)' : 'var(--warning-text)',
                      }}
                    >
                      {ep.method}
                    </span>
                    <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)', flex: 1 }}>
                      {ep.normalizedPath}
                    </span>
                    {ep.summary && (
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.6875rem', maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {ep.summary}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
            <button
              type="button"
              onClick={handleFinish}
              className="btn btn-primary btn-sm"
            >
              View API Catalog
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
};
