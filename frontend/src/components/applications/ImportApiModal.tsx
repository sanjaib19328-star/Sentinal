import React, { useState, useEffect } from 'react';
import { Modal } from '../common/Modal';
import { openApiApi } from '../../api/openapi';
import { OpenApiImportResponse } from '../../types/openapi';
import { Application } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import {
  FileCode,
  Globe,
  Upload,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Code2,
  Compass,
} from 'lucide-react';

interface ImportApiModalProps {
  isOpen: boolean;
  onClose: () => void;
  application: Application;
  onSuccess?: (result?: OpenApiImportResponse) => void;
}

export const ImportApiModal: React.FC<ImportApiModalProps> = ({
  isOpen,
  onClose,
  application,
  onSuccess,
}) => {
  const getCleanBase = (rawUrl?: string): string => {
    if (!rawUrl) return '';
    return rawUrl.split('?')[0].split('#')[0].replace(/\/$/, '');
  };

  const defaultSpecUrl = application?.baseUrl
    ? `${getCleanBase(application.baseUrl)}/api/v1/openapi.json`
    : '';

  const [activeTab, setActiveTab] = useState<'url' | 'content'>('url');
  const [specUrl, setSpecUrl] = useState(defaultSpecUrl);
  const [specContent, setSpecContent] = useState('');
  const [fileName, setFileName] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<OpenApiImportResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      const clean = getCleanBase(application?.baseUrl);
      const url = clean ? `${clean}/api/v1/openapi.json` : '';
      setSpecUrl(url);
      setSpecContent('');
      setFileName(null);
      setResult(null);
      setError(null);
      setLoading(false);
    }
  }, [isOpen, application]);

  const presetPaths = [
    '/api/v1/openapi.json',
    '/v3/api-docs',
    '/openapi.json',
    '/swagger/v1/swagger.json',
    '/api-docs',
    '/docs',
  ];

  const handlePresetClick = (path: string) => {
    const clean = getCleanBase(application?.baseUrl);
    if (clean) {
      setSpecUrl(`${clean}${path}`);
    } else {
      setSpecUrl(path);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      setSpecContent(content || '');
    };
    reader.readAsText(file);
  };

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (activeTab === 'url' && !specUrl.trim()) {
      setError('Please provide an OpenAPI / Swagger specification URL.');
      return;
    }

    if (activeTab === 'content' && !specContent.trim()) {
      setError('Please paste OpenAPI JSON/YAML content or upload a specification file.');
      return;
    }

    setLoading(true);

    try {
      const payload =
        activeTab === 'url'
          ? { specUrl: specUrl.trim() }
          : { specContent: specContent.trim() };

      const res = await openApiApi.importSpec(application.id, payload);
      setResult(res);
      if (onSuccess) {
        onSuccess(res);
      }
    } catch (err: any) {
      const msg = getErrorMessage(err);
      setError(msg || 'Failed to import APIs. Please verify your OpenAPI spec URL or schema.');
    } finally {
      setLoading(false);
    }
  };

  const getMethodBadgeClass = (method: string) => {
    switch (method.toUpperCase()) {
      case 'GET':
        return 'badge-healthy';
      case 'POST':
        return 'badge-primary';
      case 'PUT':
      case 'PATCH':
        return 'badge-degraded';
      case 'DELETE':
        return 'badge-unavailable';
      default:
        return 'badge-unknown';
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={result ? "✓ APIs Imported" : "Import APIs"}
      maxWidth={result ? "620px" : "540px"}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Application context badge */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0.625rem 0.875rem',
            backgroundColor: 'var(--bg-subtle)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Compass style={{ width: 16, height: 16, color: 'var(--primary)' }} />
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {application?.name || 'Current Application'}
            </span>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
            {application?.baseUrl}
          </span>
        </div>

        {/* Success View */}
        {result ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '0.75rem',
                padding: '1rem',
                backgroundColor: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                borderRadius: 'var(--radius-md)',
              }}
            >
              <CheckCircle2 style={{ width: 22, height: 22, color: 'var(--success)', flexShrink: 0, marginTop: 2 }} />
              <div>
                <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--success-text)', margin: 0 }}>
                  APIs Imported Successfully!
                </h4>
                <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: '0.25rem 0 0 0' }}>
                  Successfully imported <strong>{result.endpointsImported}</strong> APIs into{' '}
                  <strong>{application?.name}</strong> ({result.totalDocumentedEndpoints} total documented APIs).
                </p>
              </div>
            </div>

            {/* Metrics Breakdown */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '0.75rem',
              }}
            >
              <div
                style={{
                  padding: '0.75rem',
                  backgroundColor: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)' }}>
                  {result.endpointsImported}
                </div>
                <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  New Endpoints
                </div>
              </div>

              <div
                style={{
                  padding: '0.75rem',
                  backgroundColor: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)' }}>
                  {result.schemasCount || 0}
                </div>
                <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Schemas Parsed
                </div>
              </div>

              <div
                style={{
                  padding: '0.75rem',
                  backgroundColor: 'var(--bg-card)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)' }}>
                  {result.parametersCount || 0}
                </div>
                <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Parameters
                </div>
              </div>
            </div>

            {/* Endpoints preview list */}
            {result.endpoints && result.endpoints.length > 0 && (
              <div>
                <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                  Discovered Endpoints:
                </div>
                <div
                  style={{
                    maxHeight: '160px',
                    overflowY: 'auto',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--bg-card)',
                  }}
                >
                  {result.endpoints.map((ep, idx) => (
                    <div
                      key={ep.id || idx}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.5rem 0.75rem',
                        borderBottom: idx < result.endpoints.length - 1 ? '1px solid var(--border-color)' : 'none',
                        fontSize: '0.8125rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: 0 }}>
                        <span className={`badge ${getMethodBadgeClass(ep.method)}`} style={{ fontSize: '0.6875rem', padding: '0.1rem 0.35rem' }}>
                          {ep.method}
                        </span>
                        <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                          {ep.normalizedPath}
                        </span>
                      </div>
                      {ep.summary && (
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.5rem', whiteSpace: 'nowrap' }}>
                          {ep.summary}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-primary btn-sm"
              >
                View in API Catalog
              </button>
            </div>
          </div>
        ) : (
          /* Input Form */
          <form onSubmit={handleImport} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Tab selection */}
            <div
              style={{
                display: 'flex',
                backgroundColor: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '0.25rem',
                border: '1px solid var(--border-color)',
                gap: '0.25rem',
              }}
            >
              <button
                type="button"
                onClick={() => setActiveTab('url')}
                style={{
                  flex: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.375rem',
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  border: 'none',
                  cursor: 'pointer',
                  backgroundColor: activeTab === 'url' ? 'var(--bg-card)' : 'transparent',
                  color: activeTab === 'url' ? 'var(--primary)' : 'var(--text-muted)',
                  boxShadow: activeTab === 'url' ? 'var(--shadow-sm)' : 'none',
                }}
              >
                <Globe style={{ width: 14, height: 14 }} />
                From Spec URL
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('content')}
                style={{
                  flex: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.375rem',
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  border: 'none',
                  cursor: 'pointer',
                  backgroundColor: activeTab === 'content' ? 'var(--bg-card)' : 'transparent',
                  color: activeTab === 'content' ? 'var(--primary)' : 'var(--text-muted)',
                  boxShadow: activeTab === 'content' ? 'var(--shadow-sm)' : 'none',
                }}
              >
                <Code2 style={{ width: 14, height: 14 }} />
                Paste JSON / YAML
              </button>
            </div>

            {error && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.5rem',
                  padding: '0.75rem',
                  backgroundColor: 'var(--danger-light)',
                  border: '1px solid var(--danger-border)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger-text)',
                  fontSize: '0.8125rem',
                }}
              >
                <AlertCircle style={{ width: 16, height: 16, flexShrink: 0, marginTop: 2 }} />
                <span>{error}</span>
              </div>
            )}

            {activeTab === 'url' ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div className="form-group">
                  <label className="form-label" style={{ fontSize: '0.8125rem', fontWeight: 600 }}>
                    OpenAPI / Swagger Spec URL <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="url"
                    className="form-control"
                    value={specUrl}
                    onChange={(e) => setSpecUrl(e.target.value)}
                    placeholder="https://api.example.com/v3/api-docs"
                    style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem' }}
                    required
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem', display: 'block' }}>
                    Sentinel automatically imports and parses endpoints directly into this application.
                  </span>
                </div>

                {/* Common Presets */}
                <div>
                  <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '0.375rem' }}>
                    Common Endpoints:
                  </label>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.375rem' }}>
                    {presetPaths.map((path) => (
                      <button
                        key={path}
                        type="button"
                        onClick={() => handlePresetClick(path)}
                        className="btn btn-secondary btn-xs"
                        style={{
                          fontSize: '0.75rem',
                          fontFamily: 'var(--font-mono)',
                          padding: '0.2rem 0.5rem',
                        }}
                      >
                        {path}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div className="form-group">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
                    <label className="form-label" style={{ fontSize: '0.8125rem', fontWeight: 600, margin: 0 }}>
                      Specification Content (JSON or YAML) <span style={{ color: 'var(--danger)' }}>*</span>
                    </label>
                    <label
                      className="btn btn-secondary btn-xs"
                      style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', cursor: 'pointer', gap: '0.25rem' }}
                    >
                      <Upload style={{ width: 12, height: 12 }} />
                      <span>{fileName ? fileName : 'Upload File'}</span>
                      <input
                        type="file"
                        accept=".json,.yaml,.yml"
                        onChange={handleFileUpload}
                        style={{ display: 'none' }}
                      />
                    </label>
                  </div>
                  <textarea
                    className="form-control"
                    rows={7}
                    value={specContent}
                    onChange={(e) => setSpecContent(e.target.value)}
                    placeholder={`{\n  "openapi": "3.0.0",\n  "info": { "title": "${application?.name || 'App'} API", "version": "1.0.0" },\n  "paths": { ... }\n}`}
                    style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem', lineHeight: '1.4' }}
                  />
                </div>
              </div>
            )}

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: '0.5rem',
                marginTop: '0.5rem',
                paddingTop: '0.75rem',
                borderTop: '1px solid var(--border-color)',
              }}
            >
              <button
                type="button"
                onClick={onClose}
                className="btn btn-secondary btn-sm"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary btn-sm"
                disabled={loading}
                style={{ gap: '0.375rem' }}
              >
                {loading ? (
                  <>
                    <RefreshCw style={{ width: 14, height: 14, animation: 'spin 1s linear infinite' }} />
                    <span>Importing APIs...</span>
                  </>
                ) : (
                  <>
                    <FileCode style={{ width: 14, height: 14 }} />
                    <span>Import APIs</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  );
};
