import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { applicationsApi } from '../../api/applications';
import { Application, UpstreamAuthType, ConnectionTestResponse } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import { AlertCircle, CheckCircle2, ChevronRight, ChevronLeft, ShieldCheck, Lock, RefreshCw, Zap } from 'lucide-react';

interface CreateApplicationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newApp: Application) => void;
}

export const CreateApplicationModal: React.FC<CreateApplicationModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [step, setStep] = useState<number>(1);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [baseUrl, setBaseUrl] = useState('');
  
  // Upstream Auth
  const [authType, setAuthType] = useState<UpstreamAuthType>('NONE');
  const [headerName, setHeaderName] = useState('X-API-Key');
  const [queryParamName, setQueryParamName] = useState('apiKey');
  const [secret, setSecret] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdApp, setCreatedApp] = useState<Application | null>(null);
  
  // Connection Test
  const [testingConnection, setTestingConnection] = useState(false);
  const [testResult, setTestResult] = useState<ConnectionTestResponse | null>(null);

  const resetForm = () => {
    setStep(1);
    setName('');
    setDescription('');
    setBaseUrl('');
    setAuthType('NONE');
    setHeaderName('X-API-Key');
    setQueryParamName('apiKey');
    setSecret('');
    setUsername('');
    setPassword('');
    setError(null);
    setCreatedApp(null);
    setTestResult(null);
  };

  const handleClose = () => {
    if (createdApp) {
      onSuccess(createdApp);
    }
    resetForm();
    onClose();
  };

  const handleNextStep1 = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !baseUrl.trim()) {
      setError('Please provide both application name and base URL.');
      return;
    }
    setError(null);
    setStep(2);
  };

  const handleNextStep2 = (e: React.FormEvent) => {
    e.preventDefault();
    if (authType === 'API_KEY_HEADER' && (!headerName.trim() || !secret.trim())) {
      setError('Header name and API Key secret are required.');
      return;
    }
    if (authType === 'API_KEY_QUERY' && (!queryParamName.trim() || !secret.trim())) {
      setError('Query parameter name and API Key secret are required.');
      return;
    }
    if (authType === 'BEARER_TOKEN' && !secret.trim()) {
      setError('Bearer token secret is required.');
      return;
    }
    if (authType === 'BASIC_AUTH' && (!username.trim() || !password.trim())) {
      setError('Username and password are required for Basic Authentication.');
      return;
    }
    if (authType === 'CUSTOM_HEADER' && (!headerName.trim() || !secret.trim())) {
      setError('Header name and value are required.');
      return;
    }
    setError(null);
    setStep(3);
  };

  const handleCreateApplication = async () => {
    setError(null);
    setLoading(true);

    try {
      const upstreamAuth = authType === 'NONE' ? null : {
        type: authType,
        enabled: true,
        headerName: (authType === 'API_KEY_HEADER' || authType === 'CUSTOM_HEADER') ? headerName.trim() : undefined,
        queryParamName: authType === 'API_KEY_QUERY' ? queryParamName.trim() : undefined,
        secret: (authType === 'API_KEY_HEADER' || authType === 'API_KEY_QUERY' || authType === 'BEARER_TOKEN' || authType === 'CUSTOM_HEADER') ? secret.trim() : undefined,
        username: authType === 'BASIC_AUTH' ? username.trim() : undefined,
        password: authType === 'BASIC_AUTH' ? password.trim() : undefined,
      };

      const created = await applicationsApi.create({
        name: name.trim(),
        description: description.trim() ? description.trim() : undefined,
        baseUrl: baseUrl.trim(),
        upstreamAuth,
      });

      setCreatedApp(created);
      setStep(4);

      // Automatically run zero-pollution live connection test
      runConnectionTest(created.id);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const runConnectionTest = async (appId: number) => {
    setTestingConnection(true);
    setTestResult(null);
    try {
      const res = await applicationsApi.testConnection(appId);
      setTestResult(res);
    } catch (err) {
      setTestResult({
        applicationId: appId,
        reachable: false,
        status: 'UNAVAILABLE',
        latencyMs: null,
        message: getErrorMessage(err),
        checkedAt: new Date().toISOString(),
      });
    } finally {
      setTestingConnection(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Universal Application Onboarding Wizard">
      {/* Wizard Step Indicator */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
        {[
          { num: 1, label: 'App Info' },
          { num: 2, label: 'Upstream Auth' },
          { num: 3, label: 'Discovery Mode' },
          { num: 4, label: 'Connection Test' },
          { num: 5, label: 'Ready' }
        ].map((s) => (
          <div key={s.num} style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', opacity: step === s.num ? 1 : 0.6 }}>
            <div style={{
              width: '1.5rem',
              height: '1.5rem',
              borderRadius: '50%',
              backgroundColor: step >= s.num ? 'var(--primary)' : 'var(--bg-card)',
              border: '1px solid var(--primary)',
              color: '#fff',
              fontSize: '0.75rem',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              {step > s.num ? '✓' : s.num}
            </div>
            <span style={{ fontSize: '0.8125rem', fontWeight: step === s.num ? 600 : 400, color: step === s.num ? 'var(--text-primary)' : 'var(--text-secondary)' }}>
              {s.label}
            </span>
          </div>
        ))}
      </div>

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

      {/* STEP 1: Application Information */}
      {step === 1 && (
        <form onSubmit={handleNextStep1}>
          <div className="form-group">
            <label className="form-label" htmlFor="app-name">Application Name *</label>
            <input
              id="app-name"
              type="text"
              required
              className="form-input"
              placeholder="e.g. Payments Gateway API, Core Order Service, Inventory Service"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="app-url">Upstream Base Target URL *</label>
            <input
              id="app-url"
              type="text"
              required
              className="form-input"
              placeholder="e.g. https://api.customer.com or http://127.0.0.1:9090"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
            />
            <span style={{ display: 'block', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              The existing deployed location of your microservice or REST backend.
            </span>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="app-desc">Description (Optional)</label>
            <textarea
              id="app-desc"
              className="form-textarea"
              rows={2}
              placeholder="e.g. Production transactional payments system"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
            <button type="button" onClick={handleClose} className="btn btn-secondary btn-sm">Cancel</button>
            <button type="submit" className="btn btn-primary btn-sm">
              Next: Upstream Auth <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
            </button>
          </div>
        </form>
      )}

      {/* STEP 2: Upstream Authentication Strategy */}
      {step === 2 && (
        <form onSubmit={handleNextStep2}>
          <div style={{ marginBottom: '1rem' }}>
            <label className="form-label">Upstream Authentication Scheme</label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem', marginTop: '0.5rem' }}>
              {[
                { type: 'NONE' as UpstreamAuthType, title: 'No Auth (Public)', desc: 'Upstream requires no authentication' },
                { type: 'BEARER_TOKEN' as UpstreamAuthType, title: 'Bearer Token', desc: 'Authorization: Bearer <token>' },
                { type: 'API_KEY_HEADER' as UpstreamAuthType, title: 'API Key (Header)', desc: 'Custom header like X-API-Key' },
                { type: 'API_KEY_QUERY' as UpstreamAuthType, title: 'API Key (Query Param)', desc: '?apiKey=secret in query string' },
                { type: 'BASIC_AUTH' as UpstreamAuthType, title: 'Basic Auth', desc: 'Authorization: Basic <base64>' },
                { type: 'CUSTOM_HEADER' as UpstreamAuthType, title: 'Custom Header', desc: 'Arbitrary custom security header' },
              ].map((item) => (
                <div
                  key={item.type}
                  onClick={() => setAuthType(item.type)}
                  style={{
                    border: `1.5px solid ${authType === item.type ? 'var(--primary)' : 'var(--border-color)'}`,
                    backgroundColor: authType === item.type ? 'var(--primary-light)' : 'var(--bg-card)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.75rem',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease'
                  }}
                >
                  <div style={{ fontWeight: 600, fontSize: '0.8125rem', color: authType === item.type ? 'var(--primary)' : 'var(--text-primary)' }}>
                    {item.title}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.125rem' }}>
                    {item.desc}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Dynamic auth fields */}
          {authType === 'BEARER_TOKEN' && (
            <div className="form-group">
              <label className="form-label">Upstream Bearer Token *</label>
              <input
                type="password"
                required
                className="form-input"
                placeholder="Enter secret token"
                value={secret}
                onChange={(e) => setSecret(e.target.value)}
              />
            </div>
          )}

          {authType === 'API_KEY_HEADER' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Header Name *</label>
                <input
                  type="text"
                  required
                  className="form-input"
                  placeholder="e.g. X-API-Key"
                  value={headerName}
                  onChange={(e) => setHeaderName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">API Key Secret *</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  placeholder="Enter secret key"
                  value={secret}
                  onChange={(e) => setSecret(e.target.value)}
                />
              </div>
            </div>
          )}

          {authType === 'API_KEY_QUERY' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Query Param Name *</label>
                <input
                  type="text"
                  required
                  className="form-input"
                  placeholder="e.g. apiKey, token"
                  value={queryParamName}
                  onChange={(e) => setQueryParamName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">API Key Secret *</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  placeholder="Enter secret key"
                  value={secret}
                  onChange={(e) => setSecret(e.target.value)}
                />
              </div>
            </div>
          )}

          {authType === 'BASIC_AUTH' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Username *</label>
                <input
                  type="text"
                  required
                  className="form-input"
                  placeholder="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Password *</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  placeholder="Password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            </div>
          )}

          {authType === 'CUSTOM_HEADER' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">Header Name *</label>
                <input
                  type="text"
                  required
                  className="form-input"
                  placeholder="e.g. X-Internal-Token"
                  value={headerName}
                  onChange={(e) => setHeaderName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Header Value / Secret *</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  placeholder="Enter secret value"
                  value={secret}
                  onChange={(e) => setSecret(e.target.value)}
                />
              </div>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            <Lock style={{ width: '0.875rem', height: '0.875rem', color: 'var(--success)' }} />
            <span>Upstream secrets are encrypted at rest with AES-256-GCM and never exposed to API consumers or client responses.</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
            <button type="button" onClick={() => setStep(1)} className="btn btn-secondary btn-sm">
              <ChevronLeft style={{ width: '0.875rem', height: '0.875rem' }} /> Back
            </button>
            <button type="submit" className="btn btn-primary btn-sm">
              Next: Discovery Mode <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
            </button>
          </div>
        </form>
      )}

      {/* STEP 3: Discovery & Non-Blocking Observation */}
      {step === 3 && (
        <div>
          <div style={{ padding: '1rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              <Zap style={{ width: '1.125rem', height: '1.125rem', color: 'var(--primary)' }} />
              <span>Zero-Intrusion Dynamic Traffic Discovery</span>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
              Sentinel operates in <strong>OBSERVATION</strong> mode with automatic API discovery. As consumers send traffic through the Sentinel Universal Gateway, Sentinel automatically discovers endpoints, maps path variables (<code>/users/{'{id}'}</code>), enforces rate limits, captures telemetry, and generates real-time analytics.
            </p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem', fontSize: '0.8125rem' }}>
              <CheckCircle2 style={{ width: '1rem', height: '1rem', color: 'var(--success)', flexShrink: 0, marginTop: '0.125rem' }} />
              <span><strong>Separation of Credentials:</strong> Consumer calls use Sentinel API Keys (<code>X-Sentinel-API-Key: sk_sentinel_...</code>). Sentinel automatically signs and forwards upstream requests with your configured secret.</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem', fontSize: '0.8125rem' }}>
              <CheckCircle2 style={{ width: '1rem', height: '1rem', color: 'var(--success)', flexShrink: 0, marginTop: '0.125rem' }} />
              <span><strong>Fail-Safe Isolation:</strong> If Sentinel encounters unexpected load, upstream traffic continues smoothly and health status is tracked independently.</span>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <button type="button" onClick={() => setStep(2)} className="btn btn-secondary btn-sm" disabled={loading}>
              <ChevronLeft style={{ width: '0.875rem', height: '0.875rem' }} /> Back
            </button>
            <button type="button" onClick={handleCreateApplication} disabled={loading} className="btn btn-primary btn-sm">
              {loading ? 'Creating Application...' : 'Confirm & Test Connectivity'} <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
            </button>
          </div>
        </div>
      )}

      {/* STEP 4: Live Zero-Pollution Connection Test */}
      {step === 4 && (
        <div>
          <div style={{ textAlign: 'center', padding: '1.5rem 0' }}>
            {testingConnection ? (
              <div>
                <RefreshCw style={{ width: '2.5rem', height: '2.5rem', color: 'var(--primary)', animation: 'spin 1s linear infinite', margin: '0 auto 1rem' }} />
                <h4 style={{ margin: '0 0 0.5rem', fontSize: '1.125rem' }}>Probing Upstream Application...</h4>
                <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  Sentinel is testing connectivity and upstream authentication against <code>{baseUrl}</code> without polluting telemetry or request logs.
                </p>
              </div>
            ) : testResult ? (
              <div>
                {testResult.reachable ? (
                  <CheckCircle2 style={{ width: '3rem', height: '3rem', color: 'var(--success)', margin: '0 auto 0.75rem' }} />
                ) : (
                  <AlertCircle style={{ width: '3rem', height: '3rem', color: 'var(--warning)', margin: '0 auto 0.75rem' }} />
                )}
                <h4 style={{ margin: '0 0 0.5rem', fontSize: '1.125rem' }}>
                  {testResult.reachable ? 'Upstream Connection Successful!' : 'Upstream Probe Warning'}
                </h4>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                  {testResult.message}
                </p>

                <div style={{ display: 'inline-flex', gap: '1.5rem', padding: '0.75rem 1.5rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', textAlign: 'left' }}>
                  <div>
                    <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Status</div>
                    <div style={{ fontWeight: 600, color: testResult.status === 'HEALTHY' ? 'var(--success)' : 'var(--warning)' }}>
                      {testResult.status}
                    </div>
                  </div>
                  {testResult.latencyMs !== null && (
                    <div>
                      <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Probe Latency</div>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{testResult.latencyMs} ms</div>
                    </div>
                  )}
                  <div>
                    <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Auth Verified</div>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{authType}</div>
                  </div>
                </div>
              </div>
            ) : null}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
            <button
              type="button"
              onClick={() => createdApp && runConnectionTest(createdApp.id)}
              disabled={testingConnection}
              className="btn btn-secondary btn-sm"
            >
              <RefreshCw style={{ width: '0.875rem', height: '0.875rem' }} /> Re-Test
            </button>
            <button
              type="button"
              onClick={() => setStep(5)}
              className="btn btn-primary btn-sm"
            >
              Next: Gateway Setup <ChevronRight style={{ width: '0.875rem', height: '0.875rem' }} />
            </button>
          </div>
        </div>
      )}

      {/* STEP 5: Final Onboarding Completion */}
      {step === 5 && createdApp && (
        <div>
          <div style={{ textAlign: 'center', padding: '1rem 0 1.25rem' }}>
            <ShieldCheck style={{ width: '2.75rem', height: '2.75rem', color: 'var(--primary)', margin: '0 auto 0.5rem' }} />
            <h4 style={{ margin: '0 0 0.25rem', fontSize: '1.125rem' }}>Application Successfully Onboarded!</h4>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
              <strong>{createdApp.name}</strong> is now registered and monitored by Sentinel.
            </p>
          </div>

          <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', marginBottom: '1.25rem' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.375rem' }}>
              Sentinel Gateway Target URL
            </div>
            <code style={{ display: 'block', padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-card)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', fontSize: '0.8125rem', color: 'var(--primary)', fontWeight: 600, fontFamily: 'var(--font-mono)' }}>
              http://localhost:8080/api/v1/gateway/*
            </code>

            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: '0.875rem', marginBottom: '0.375rem' }}>
              API Discovery Request Format
            </div>
            <pre style={{
              backgroundColor: 'var(--bg-card)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-sm)',
              padding: '0.625rem 0.75rem',
              fontSize: '0.75rem',
              fontFamily: 'var(--font-mono)',
              color: 'var(--text-primary)',
              margin: 0,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all'
            }}>
              {`curl -X GET "http://localhost:8080/api/v1/gateway/api/example" \\\n  -H "X-Sentinel-API-Key: sk_sentinel_..."`}
            </pre>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: '0.5rem 0 0' }}>
              Create an API Key in the application dashboard and route traffic through Sentinel. All active routes will automatically be discovered in your <strong>API Catalog</strong>.
            </p>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button
              type="button"
              onClick={handleClose}
              className="btn btn-primary btn-sm"
            >
              Open Application Dashboard
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
};
