import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { ApiKey } from '../../types/apiKey';
import { ShieldAlert, Copy, Check } from 'lucide-react';

interface ApiKeyModalProps {
  isOpen: boolean;
  apiKey: ApiKey | null;
  onClose: () => void;
}

export const ApiKeyModal: React.FC<ApiKeyModalProps> = ({
  isOpen,
  apiKey,
  onClose,
}) => {
  const [copiedKey, setCopiedKey] = useState(false);
  const [copiedUrl, setCopiedUrl] = useState(false);
  const [copiedCurl, setCopiedCurl] = useState(false);

  if (!apiKey || !apiKey.apiKey) return null;

  const gatewayBaseUrl = window.location.port === '5173' || window.location.port === '3000'
    ? 'http://localhost:8080/api/v1/gateway'
    : `${window.location.origin}/api/v1/gateway`;

  const sampleCurl = `curl -X GET "${gatewayBaseUrl}/api/example" \\\n  -H "X-Sentinel-API-Key: ${apiKey.apiKey}"`;

  const copyToClipboard = async (text: string, setter: (val: boolean) => void) => {
    try {
      await navigator.clipboard.writeText(text);
      setter(true);
      setTimeout(() => setter(false), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="New API Key & Gateway Entrypoint Generated" maxWidth="580px">
      <div
        style={{
          display: 'flex',
          gap: '0.75rem',
          padding: '0.875rem 1rem',
          backgroundColor: 'var(--warning-light)',
          border: '1px solid var(--warning-border)',
          borderRadius: 'var(--radius-md)',
          color: 'var(--warning-text)',
          fontSize: '0.8125rem',
          marginBottom: '1.25rem',
        }}
      >
        <ShieldAlert style={{ width: '1.25rem', height: '1.25rem', flexShrink: 0, marginTop: '0.125rem' }} />
        <div>
          <strong>Security Notice:</strong> Please copy this API key now. Sentinel stores only SHA-256 hashes and will never display the raw key again.
        </div>
      </div>

      <div className="form-group">
        <label className="form-label">Key Name & Quota</label>
        <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
          {apiKey.name} <span style={{ color: 'var(--text-secondary)', fontWeight: 400 }}>(Rate limit: {apiKey.rateLimitPerMinute} req/min)</span>
        </div>
      </div>

      {/* Raw API Key */}
      <div className="form-group">
        <label className="form-label">Raw API Key</label>
        <div style={{ position: 'relative' }}>
          <div className="code-box" style={{ paddingRight: '3rem', wordBreak: 'break-all', fontFamily: 'var(--font-mono)' }}>
            {apiKey.apiKey}
          </div>
          <button
            onClick={() => copyToClipboard(apiKey.apiKey || '', setCopiedKey)}
            className="btn btn-secondary btn-sm"
            style={{
              position: 'absolute',
              right: '0.5rem',
              top: '50%',
              transform: 'translateY(-50%)',
              padding: '0.25rem 0.5rem',
            }}
          >
            {copiedKey ? (
              <>
                <Check style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
                Copied
              </>
            ) : (
              <>
                <Copy style={{ width: '0.875rem', height: '0.875rem' }} />
                Copy Key
              </>
            )}
          </button>
        </div>
      </div>

      {/* Sentinel Gateway URL */}
      <div className="form-group" style={{ marginTop: '1.25rem' }}>
        <label className="form-label">Sentinel Gateway Base URL</label>
        <div style={{ position: 'relative' }}>
          <div className="code-box" style={{ paddingRight: '3rem', color: 'var(--primary)', fontWeight: 600, fontFamily: 'var(--font-mono)' }}>
            {gatewayBaseUrl}/*
          </div>
          <button
            onClick={() => copyToClipboard(gatewayBaseUrl, setCopiedUrl)}
            className="btn btn-secondary btn-sm"
            style={{
              position: 'absolute',
              right: '0.5rem',
              top: '50%',
              transform: 'translateY(-50%)',
              padding: '0.25rem 0.5rem',
            }}
          >
            {copiedUrl ? (
              <>
                <Check style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
                Copied
              </>
            ) : (
              <>
                <Copy style={{ width: '0.875rem', height: '0.875rem' }} />
                Copy URL
              </>
            )}
          </button>
        </div>
      </div>

      {/* Traffic Discovery & cURL Format */}
      <div className="form-group" style={{ marginTop: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
          <label className="form-label" style={{ marginBottom: 0 }}>API Traffic Discovery Request Format</label>
          <button
            type="button"
            onClick={() => copyToClipboard(sampleCurl, setCopiedCurl)}
            className="btn btn-secondary btn-xs"
          >
            {copiedCurl ? (
              <>
                <Check style={{ width: '0.75rem', height: '0.75rem', color: 'var(--success)' }} />
                Copied cURL
              </>
            ) : (
              <>
                <Copy style={{ width: '0.75rem', height: '0.75rem' }} />
                Copy cURL
              </>
            )}
          </button>
        </div>
        <pre style={{
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-md)',
          padding: '0.75rem',
          fontSize: '0.8125rem',
          fontFamily: 'var(--font-mono)',
          color: 'var(--text-primary)',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-all',
          margin: 0
        }}>
          {sampleCurl}
        </pre>
        <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.375rem' }}>
          Any request routed through this URL with your <code>X-Sentinel-API-Key</code> is automatically authenticated, rate-limited, and discovered in your <strong>API Catalog</strong>.
        </span>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
        <button onClick={onClose} className="btn btn-primary btn-sm">
          I Have Saved This Key & URL
        </button>
      </div>
    </Modal>
  );
};
