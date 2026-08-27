import React, { useEffect, useState } from 'react';
import { ApiPolicy, SavePolicyRequest } from '../../types/policy';
import { ApiEndpoint } from '../../types/application';
import {
  getApplicationPolicy,
  saveApplicationPolicy,
  deleteApplicationPolicy,
  getEndpointPolicy,
  saveEndpointPolicy,
  deleteEndpointPolicy,
} from '../../api/policies';
import { getErrorMessage } from '../../api/client';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorBanner } from '../common/ErrorBanner';
import {
  Save,
  RotateCcw,
  Trash2,
  CheckCircle,
  Sliders,
  Layers,
  ShieldAlert,
  RefreshCw,
} from 'lucide-react';

interface PolicyTabProps {
  applicationId: number;
  endpoints: ApiEndpoint[];
}

export const PolicyTab: React.FC<PolicyTabProps> = ({ applicationId, endpoints }) => {
  const [selectedEndpointId, setSelectedEndpointId] = useState<string>('APP'); // 'APP' or endpointId
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form State
  const [enabled, setEnabled] = useState(true);
  const [rateLimit, setRateLimit] = useState(60);
  const [rateLimitWindowSeconds, setRateLimitWindowSeconds] = useState(60);
  const [quotaLimit, setQuotaLimit] = useState<string>('');
  const [quotaWindowSeconds, setQuotaWindowSeconds] = useState<string>('86400');
  const [timeoutMs, setTimeoutMs] = useState(5000);
  const [maxRequestBodyBytes, setMaxRequestBodyBytes] = useState<string>('');
  const [allowedMethods, setAllowedMethods] = useState('');
  const [ipWhitelist, setIpWhitelist] = useState('');

  // Phase 3: Retries & Circuit Breaker
  const [retryCount, setRetryCount] = useState(0);
  const [retryDelayMs, setRetryDelayMs] = useState(100);
  const [retryNonIdempotent, setRetryNonIdempotent] = useState(false);
  const [circuitBreakerEnabled, setCircuitBreakerEnabled] = useState(true);
  const [circuitFailureThreshold, setCircuitFailureThreshold] = useState(5);
  const [circuitRecoveryTimeoutSeconds, setCircuitRecoveryTimeoutSeconds] = useState(30);

  const fetchPolicy = async () => {
    setLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      if (selectedEndpointId === 'APP') {
        const p = await getApplicationPolicy(applicationId);
        populateForm(p);
      } else {
        const epId = parseInt(selectedEndpointId, 10);
        const p = await getEndpointPolicy(applicationId, epId);
        populateForm(p);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const populateForm = (p: ApiPolicy) => {
    setEnabled(p.enabled);
    setRateLimit(p.rateLimit);
    setRateLimitWindowSeconds(p.rateLimitWindowSeconds);
    setQuotaLimit(p.quotaLimit !== null && p.quotaLimit !== undefined ? String(p.quotaLimit) : '');
    setQuotaWindowSeconds(p.quotaWindowSeconds !== null && p.quotaWindowSeconds !== undefined ? String(p.quotaWindowSeconds) : '86400');
    setTimeoutMs(p.timeoutMs || 5000);
    setMaxRequestBodyBytes(p.maxRequestBodyBytes !== null && p.maxRequestBodyBytes !== undefined ? String(p.maxRequestBodyBytes) : '');
    setAllowedMethods(p.allowedMethods || '');
    setIpWhitelist(p.ipWhitelist || '');

    // Phase 3
    setRetryCount(p.retryCount || 0);
    setRetryDelayMs(p.retryDelayMs || 100);
    setRetryNonIdempotent(!!p.retryNonIdempotent);
    setCircuitBreakerEnabled(p.circuitBreakerEnabled !== undefined ? p.circuitBreakerEnabled : true);
    setCircuitFailureThreshold(p.circuitFailureThreshold || 5);
    setCircuitRecoveryTimeoutSeconds(p.circuitRecoveryTimeoutSeconds || 30);
  };

  useEffect(() => {
    fetchPolicy();
  }, [applicationId, selectedEndpointId]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccessMessage(null);

    const payload: SavePolicyRequest = {
      enabled,
      rateLimit,
      rateLimitWindowSeconds,
      quotaLimit: quotaLimit ? parseInt(quotaLimit, 10) : null,
      quotaWindowSeconds: quotaLimit && quotaWindowSeconds ? parseInt(quotaWindowSeconds, 10) : null,
      timeoutMs,
      maxRequestBodyBytes: maxRequestBodyBytes ? parseInt(maxRequestBodyBytes, 10) : null,
      allowedMethods: allowedMethods.trim() ? allowedMethods.trim().toUpperCase() : null,
      ipWhitelist: ipWhitelist.trim() ? ipWhitelist.trim() : null,
      retryCount,
      retryDelayMs,
      retryNonIdempotent,
      circuitBreakerEnabled,
      circuitFailureThreshold,
      circuitRecoveryTimeoutSeconds,
    };

    try {
      if (selectedEndpointId === 'APP') {
        const updated = await saveApplicationPolicy(applicationId, payload);
        populateForm(updated);
        setSuccessMessage('Application-level traffic policy saved successfully.');
      } else {
        const epId = parseInt(selectedEndpointId, 10);
        const updated = await saveEndpointPolicy(applicationId, epId, payload);
        populateForm(updated);
        setSuccessMessage('Endpoint-level traffic policy saved successfully.');
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to reset this policy to defaults?')) return;
    setSaving(true);
    setError(null);
    setSuccessMessage(null);

    try {
      if (selectedEndpointId === 'APP') {
        await deleteApplicationPolicy(applicationId);
        setSuccessMessage('Application policy reset to defaults.');
      } else {
        const epId = parseInt(selectedEndpointId, 10);
        await deleteEndpointPolicy(applicationId, epId);
        setSuccessMessage('Endpoint policy removed.');
      }
      await fetchPolicy();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header & Target Selector */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h3 className="card-title">Traffic Policies & Reliability Control</h3>
            <p className="card-subtitle">
              Configure multi-level throttling, quotas, timeouts, automatic retries, and circuit breaker protection
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-muted)' }}>Policy Scope:</span>
            <select
              value={selectedEndpointId}
              onChange={(e) => setSelectedEndpointId(e.target.value)}
              className="form-input"
              style={{ width: 'auto', minWidth: '220px', padding: '0.375rem 0.75rem', fontSize: '0.875rem' }}
            >
              <option value="APP">🌐 Entire Application (Default)</option>
              {endpoints.map((ep) => (
                <option key={ep.id} value={String(ep.id)}>
                  📍 {ep.method} {ep.normalizedPath}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error && <ErrorBanner message={error} onRetry={fetchPolicy} />}

        {successMessage && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              background: 'rgba(34, 197, 94, 0.1)',
              border: '1px solid rgba(34, 197, 94, 0.3)',
              color: 'var(--success-text)',
              padding: '0.75rem 1rem',
              borderRadius: '0.5rem',
              margin: '1rem',
              fontSize: '0.875rem',
            }}
          >
            <CheckCircle style={{ width: '1rem', height: '1rem', color: 'var(--success)' }} />
            <span>{successMessage}</span>
          </div>
        )}

        {loading ? (
          <div style={{ padding: '2rem' }}>
            <LoadingSpinner message="Loading policy configuration..." />
          </div>
        ) : (
          <form onSubmit={handleSave} style={{ padding: '1.5rem' }}>
            {/* Scope Badge */}
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.375rem 0.75rem',
                borderRadius: '0.375rem',
                background: selectedEndpointId === 'APP' ? 'rgba(59, 130, 246, 0.1)' : 'rgba(168, 85, 247, 0.1)',
                color: selectedEndpointId === 'APP' ? 'var(--primary)' : '#9333ea',
                fontSize: '0.8125rem',
                fontWeight: 600,
                marginBottom: '1.5rem',
              }}
            >
              {selectedEndpointId === 'APP' ? (
                <>
                  <Layers style={{ width: '0.875rem', height: '0.875rem' }} />
                  <span>Enforcing at Application Scope</span>
                </>
              ) : (
                <>
                  <Sliders style={{ width: '0.875rem', height: '0.875rem' }} />
                  <span>Enforcing at Specific Endpoint Override</span>
                </>
              )}
            </div>

            {/* Policy Enable Toggle */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '1rem',
                background: 'var(--surface-subtle, #f8fafc)',
                borderRadius: '0.5rem',
                marginBottom: '1.5rem',
                border: '1px solid var(--border-color)',
              }}
            >
              <div>
                <div style={{ fontWeight: 600, fontSize: '0.9375rem' }}>Policy Enforcement Active</div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                  When disabled, requests pass through without rate or quota restrictions from this policy
                </div>
              </div>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={enabled}
                  onChange={(e) => setEnabled(e.target.checked)}
                  style={{ width: '1.25rem', height: '1.25rem', accentColor: 'var(--primary)' }}
                />
                <span style={{ fontWeight: 600, fontSize: '0.875rem' }}>{enabled ? 'Enabled' : 'Disabled'}</span>
              </label>
            </div>

            {/* Section 1: Throttling & Quotas */}
            <div style={{ marginBottom: '1.75rem' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem' }}>
                Rate Limits & Quota Controls
              </h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">Rate Limit (Max Requests)</label>
                  <input
                    type="number"
                    min="1"
                    max="1000000"
                    value={rateLimit}
                    onChange={(e) => setRateLimit(parseInt(e.target.value, 10) || 1)}
                    className="form-input"
                    required
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Requests allowed per time window</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Rate Limit Window (Seconds)</label>
                  <select
                    value={rateLimitWindowSeconds}
                    onChange={(e) => setRateLimitWindowSeconds(parseInt(e.target.value, 10))}
                    className="form-input"
                  >
                    <option value={10}>10 Seconds (Burst)</option>
                    <option value={60}>60 Seconds (1 Minute)</option>
                    <option value={300}>300 Seconds (5 Minutes)</option>
                    <option value={3600}>3600 Seconds (1 Hour)</option>
                  </select>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Sliding window duration in Redis</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Quota Limit (Optional)</label>
                  <input
                    type="number"
                    min="1"
                    placeholder="Unlimited"
                    value={quotaLimit}
                    onChange={(e) => setQuotaLimit(e.target.value)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Leave blank for unlimited total</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Quota Window Period</label>
                  <select
                    value={quotaWindowSeconds}
                    onChange={(e) => setQuotaWindowSeconds(e.target.value)}
                    className="form-input"
                    disabled={!quotaLimit}
                  >
                    <option value="3600">Hourly (1 Hour)</option>
                    <option value="86400">Daily (24 Hours)</option>
                    <option value="604800">Weekly (7 Days)</option>
                    <option value="2592000">Monthly (30 Days)</option>
                  </select>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Accumulation window</span>
                </div>
              </div>
            </div>

            {/* Section 2: Phase 3 Automatic Retries & Gateway Reliability */}
            <div style={{ marginBottom: '1.75rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <RefreshCw style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Automatic Safe Retries
                </h4>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">Retry Attempts (0-5)</label>
                  <input
                    type="number"
                    min="0"
                    max="5"
                    value={retryCount}
                    onChange={(e) => setRetryCount(parseInt(e.target.value, 10) || 0)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Retries on 502/503/504 or connect errors</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Retry Backoff Delay (ms)</label>
                  <input
                    type="number"
                    min="10"
                    max="5000"
                    value={retryDelayMs}
                    onChange={(e) => setRetryDelayMs(parseInt(e.target.value, 10) || 100)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Delay before next retry attempt</span>
                </div>

                <div className="form-group" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginTop: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={retryNonIdempotent}
                      onChange={(e) => setRetryNonIdempotent(e.target.checked)}
                      style={{ width: '1.125rem', height: '1.125rem', accentColor: 'var(--primary)' }}
                    />
                    <span style={{ fontWeight: 600, fontSize: '0.8125rem' }}>Allow Retries on POST/PUT/PATCH/DELETE</span>
                  </label>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    Safe by default: Only GET/HEAD retry unless explicitly allowed here.
                  </span>
                </div>
              </div>
            </div>

            {/* Section 3: Phase 3 Circuit Breaker */}
            <div style={{ marginBottom: '1.75rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
                <ShieldAlert style={{ width: '1rem', height: '1rem', color: '#f43f5e' }} />
                <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Circuit Breaker Fast-Failing
                </h4>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginTop: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={circuitBreakerEnabled}
                      onChange={(e) => setCircuitBreakerEnabled(e.target.checked)}
                      style={{ width: '1.125rem', height: '1.125rem', accentColor: 'var(--primary)' }}
                    />
                    <span style={{ fontWeight: 600, fontSize: '0.8125rem' }}>Enable Circuit Breaker</span>
                  </label>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    Trips to OPEN to fast-fail traffic (503) when downstream fails repeatedly
                  </span>
                </div>

                <div className="form-group">
                  <label className="form-label">Consecutive Failure Threshold</label>
                  <input
                    type="number"
                    min="1"
                    max="50"
                    value={circuitFailureThreshold}
                    onChange={(e) => setCircuitFailureThreshold(parseInt(e.target.value, 10) || 5)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Failures before tripping circuit OPEN</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Recovery Timeout (Seconds)</label>
                  <input
                    type="number"
                    min="5"
                    max="600"
                    value={circuitRecoveryTimeoutSeconds}
                    onChange={(e) => setCircuitRecoveryTimeoutSeconds(parseInt(e.target.value, 10) || 30)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Cooldown before attempting HALF_OPEN trial probes</span>
                </div>
              </div>
            </div>

            {/* Section 4: Security, Methods & Body Limits */}
            <div style={{ marginBottom: '1.75rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border-color)' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1rem' }}>
                Timeouts & Payload Security
              </h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem' }}>
                <div className="form-group">
                  <label className="form-label">Target Gateway Timeout (ms)</label>
                  <input
                    type="number"
                    min="500"
                    max="60000"
                    value={timeoutMs}
                    onChange={(e) => setTimeoutMs(parseInt(e.target.value, 10) || 5000)}
                    className="form-input"
                    required
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Max duration to wait for backend (500-60000ms)</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Max Request Body (Bytes)</label>
                  <input
                    type="number"
                    min="1"
                    placeholder="Unlimited (e.g. 1048576 for 1MB)"
                    value={maxRequestBodyBytes}
                    onChange={(e) => setMaxRequestBodyBytes(e.target.value)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Rejects with 413 if exceeded</span>
                </div>

                <div className="form-group">
                  <label className="form-label">Allowed HTTP Methods</label>
                  <input
                    type="text"
                    placeholder="e.g. GET,POST,PUT (empty for all)"
                    value={allowedMethods}
                    onChange={(e) => setAllowedMethods(e.target.value)}
                    className="form-input"
                    style={{ textTransform: 'uppercase' }}
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Restricts allowed methods; non-matching return 405</span>
                </div>

                <div className="form-group">
                  <label className="form-label">IP Whitelist (Optional)</label>
                  <input
                    type="text"
                    placeholder="e.g. 127.0.0.1, 10.0.0.0/24"
                    value={ipWhitelist}
                    onChange={(e) => setIpWhitelist(e.target.value)}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Comma-separated IPs allowed</span>
                </div>
              </div>
            </div>

            {/* Actions Bar */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginTop: '2rem',
                paddingTop: '1.25rem',
                borderTop: '1px solid var(--border-color)',
              }}
            >
              <button
                type="button"
                onClick={handleDelete}
                disabled={saving}
                className="btn btn-danger btn-sm"
              >
                <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
                Reset to Defaults
              </button>

              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button
                  type="button"
                  onClick={fetchPolicy}
                  disabled={saving}
                  className="btn btn-secondary"
                >
                  <RotateCcw style={{ width: '1rem', height: '1rem' }} />
                  Reload
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="btn btn-primary"
                >
                  <Save style={{ width: '1rem', height: '1rem' }} />
                  {saving ? 'Saving Policy...' : 'Save Traffic Policy'}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
