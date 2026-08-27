import React, { useState } from 'react';
import { Application } from '../../types/application';
import { ApiKey } from '../../types/apiKey';
import {
  Globe,
  KeyRound,
  FileCode,
  Copy,
  Check,
  Terminal,
  Compass,
  Play,
  Bot,
  Plus,
  ShieldCheck,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface ConnectionAccessCardProps {
  application: Application;
  keys: ApiKey[];
  endpointsCount: number;
  onOpenImportModal: () => void;
  onOpenTestConsole: () => void;
  onOpenCreateKeyModal: () => void;
  onOpenKeysTab: () => void;
}

export const ConnectionAccessCard: React.FC<ConnectionAccessCardProps> = ({
  application,
  keys,
  endpointsCount,
  onOpenImportModal,
  onOpenTestConsole,
  onOpenCreateKeyModal,
  onOpenKeysTab,
}) => {
  const navigate = useNavigate();
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const gatewayBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080/api/v1/gateway`;
  const sampleEndpointPath = '/api/v1/health';
  const sampleGatewayUrl = `${gatewayBaseUrl}${sampleEndpointPath}`;

  const activeKey = keys.find((k) => k.active) || keys[0];
  const maskedKeyDisplay = activeKey ? activeKey.maskedKey || `sk_••••••••${activeKey.id}` : 'No active key';

  const sampleCurl = `# Send request through Sentinel Gateway:\ncurl -X GET "${sampleGatewayUrl}" \\\n  -H "X-Sentinel-API-Key: ${
    activeKey?.maskedKey || 'YOUR_SENTINEL_API_KEY'
  }"`;

  const copyToClipboard = (text: string, fieldName: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => setCopiedField(null), 2000);
  };

  return (
    <div
      className="card"
      style={{
        padding: '1.25rem',
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-sm)',
        backgroundColor: 'var(--bg-card)',
      }}
    >
      {/* Header & Quick Action Badges */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <div
            style={{
              width: '2.25rem',
              height: '2.25rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <ShieldCheck style={{ width: '1.25rem', height: '1.25rem' }} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                Connection & API Access
              </h4>
              <span className="pill-badge pill-badge-green">Gateway Ready</span>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0.15rem 0 0 0' }}>
              URLs, routing endpoints, authentication headers, and discovery actions for this application
            </p>
          </div>
        </div>

        {/* Quick Actions */}
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button
            type="button"
            onClick={onOpenImportModal}
            className="btn btn-primary btn-sm"
            style={{ gap: '0.375rem' }}
          >
            <FileCode style={{ width: 14, height: 14 }} />
            Discover / Import OpenAPI
          </button>
          <button
            type="button"
            onClick={onOpenTestConsole}
            className="btn btn-secondary btn-sm"
            style={{ gap: '0.375rem' }}
          >
            <Play style={{ width: 14, height: 14 }} />
            Test in Console
          </button>
          <button
            type="button"
            onClick={() => navigate('/assistant')}
            className="btn btn-secondary btn-sm"
            style={{ gap: '0.375rem' }}
          >
            <Bot style={{ width: 14, height: 14 }} />
            AI Assistant
          </button>
        </div>
      </div>

      {/* Grid of Key Connection Attributes */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '1rem',
        }}
      >
        {/* 1. Application Target URL */}
        <div
          style={{
            padding: '0.875rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Application Upstream Base URL
              </span>
              <Globe style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            </div>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.8125rem',
                color: 'var(--text-primary)',
                fontWeight: 600,
                wordBreak: 'break-all',
              }}
            >
              {application.baseUrl || 'http://localhost:8080'}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target Backend</span>
            <button
              type="button"
              onClick={() => copyToClipboard(application.baseUrl, 'baseUrl')}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
            >
              {copiedField === 'baseUrl' ? (
                <>
                  <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                  <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                </>
              ) : (
                <>
                  <Copy style={{ width: 12, height: 12 }} />
                  <span>Copy URL</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* 2. OpenAPI Specification URL */}
        <div
          style={{
            padding: '0.875rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                OpenAPI / Swagger Spec URL
              </span>
              <FileCode style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            </div>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.8125rem',
                color: 'var(--text-primary)',
                fontWeight: 600,
                wordBreak: 'break-all',
              }}
            >
              {application.baseUrl ? `${application.baseUrl.replace(/\/$/, '')}/v3/api-docs` : 'http://localhost:8081/v3/api-docs'}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Documentation Spec</span>
            <button
              type="button"
              onClick={() => copyToClipboard(application.baseUrl ? `${application.baseUrl.replace(/\/$/, '')}/v3/api-docs` : 'http://localhost:8081/v3/api-docs', 'specUrl')}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
            >
              {copiedField === 'specUrl' ? (
                <>
                  <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                  <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                </>
              ) : (
                <>
                  <Copy style={{ width: 12, height: 12 }} />
                  <span>Copy URL</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* 3. Sentinel Gateway Base URL */}
        <div
          style={{
            padding: '0.875rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Sentinel Gateway Base URL
              </span>
              <Terminal style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            </div>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.8125rem',
                color: 'var(--text-primary)',
                fontWeight: 600,
                wordBreak: 'break-all',
              }}
            >
              {gatewayBaseUrl}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Forwarding Root</span>
            <button
              type="button"
              onClick={() => copyToClipboard(gatewayBaseUrl, 'gatewayBaseUrl')}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
            >
              {copiedField === 'gatewayBaseUrl' ? (
                <>
                  <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                  <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                </>
              ) : (
                <>
                  <Copy style={{ width: 12, height: 12 }} />
                  <span>Copy Gateway URL</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* 3. Developer API Key Status & Usage */}
        <div
          style={{
            padding: '0.875rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Developer API Key
              </span>
              <KeyRound style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            </div>

            {activeKey ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.2rem' }}>
                <code
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '0.8125rem',
                    color: 'var(--text-primary)',
                    fontWeight: 600,
                  }}
                >
                  {maskedKeyDisplay}
                </code>
                <span className="pill-badge pill-badge-blue" style={{ fontSize: '0.6875rem' }}>
                  {activeKey.rateLimitPerMinute} req/min
                </span>
              </div>
            ) : (
              <div style={{ fontSize: '0.8125rem', color: 'var(--warning-text)', fontWeight: 600 }}>
                No active API key created
              </div>
            )}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
            <button
              type="button"
              onClick={onOpenKeysTab}
              className="btn btn-link btn-sm"
              style={{ padding: 0, fontSize: '0.75rem' }}
            >
              Manage Keys ({keys.length})
            </button>

            {activeKey ? (
              <button
                type="button"
                onClick={() => copyToClipboard(activeKey.maskedKey || `sk_••••••••${activeKey.id}`, 'apiKey')}
                className="btn btn-ghost btn-sm"
                style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
              >
                {copiedField === 'apiKey' ? (
                  <>
                    <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                    <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                  </>
                ) : (
                  <>
                    <Copy style={{ width: 12, height: 12 }} />
                    <span>Copy Key</span>
                  </>
                )}
              </button>
            ) : (
              <button
                type="button"
                onClick={onOpenCreateKeyModal}
                className="btn btn-primary btn-sm"
                style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
              >
                <Plus style={{ width: 12, height: 12 }} />
                Create Key
              </button>
            )}
          </div>
        </div>

        {/* 4. API Catalog Onboarding Status */}
        <div
          style={{
            padding: '0.875rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '0.5rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                API Catalog Onboarding
              </span>
              <Compass style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--primary)' }}>
                {endpointsCount} APIs
              </span>
              <span className={`pill-badge ${endpointsCount > 0 ? 'pill-badge-green' : 'pill-badge-amber'}`}>
                {endpointsCount > 0 ? 'Cataloged' : 'Awaiting Spec'}
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              OpenAPI + Auto-Discovery
            </span>
            <button
              type="button"
              onClick={onOpenImportModal}
              className="btn btn-ghost btn-sm"
              style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem', color: 'var(--primary)' }}
            >
              <FileCode style={{ width: 12, height: 12 }} />
              <span>Import Spec</span>
            </button>
          </div>
        </div>
      </div>

      {/* Gateway Request Format Snippet & cURL */}
      <div
        style={{
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-md)',
          backgroundColor: 'var(--bg-main)',
          padding: '1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.625rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            <Terminal style={{ width: 14, height: 14, color: 'var(--primary)' }} />
            <span>How to Route API Requests Through Sentinel:</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <button
              type="button"
              onClick={() => copyToClipboard('X-Sentinel-API-Key', 'headerName')}
              className="btn btn-ghost btn-sm"
              style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
            >
              {copiedField === 'headerName' ? 'Copied Header Name' : 'Copy Header Name'}
            </button>
            <button
              type="button"
              onClick={() => copyToClipboard(sampleCurl, 'curl')}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '0.75rem', padding: '0.25rem 0.625rem', gap: '0.25rem' }}
            >
              {copiedField === 'curl' ? (
                <>
                  <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                  <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied cURL</span>
                </>
              ) : (
                <>
                  <Copy style={{ width: 12, height: 12 }} />
                  <span>Copy cURL Command</span>
                </>
              )}
            </button>
          </div>
        </div>

        <pre
          className="console-code-viewer"
          style={{
            margin: 0,
            padding: '0.75rem 1rem',
            fontSize: '0.8125rem',
            color: '#86efac',
            borderRadius: 'var(--radius-sm)',
            overflowX: 'auto',
          }}
        >
          {sampleCurl}
        </pre>
      </div>
    </div>
  );
};
