import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { applicationsApi } from '../api/applications';
import { apiKeysApi } from '../api/apiKeys';
import { Application, ApiEndpoint } from '../types/application';
import { ApiKey } from '../types/apiKey';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { ImportApiModal } from '../components/applications/ImportApiModal';
import { Link } from 'react-router-dom';
import {
  Layers,
  Globe,
  FileCode,
  Copy,
  Check,
  Terminal,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';

export const Profile: React.FC = () => {
  const { user } = useAuth();
  const [applications, setApplications] = useState<Application[]>([]);
  const [appKeys, setAppKeys] = useState<{ [appId: number]: ApiKey[] }>({});
  const [appApis, setAppApis] = useState<{ [appId: number]: ApiEndpoint[] }>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [selectedAppForImport, setSelectedAppForImport] = useState<Application | null>(null);

  const gatewayBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080/api/v1/gateway`;

  const fetchProfileData = async () => {
    setLoading(true);
    setError(null);
    try {
      const apps = await applicationsApi.list();
      setApplications(apps);

      // Fetch keys and endpoints in parallel for all apps
      const keysMap: { [appId: number]: ApiKey[] } = {};
      const apisMap: { [appId: number]: ApiEndpoint[] } = {};

      await Promise.all(
        apps.map(async (app) => {
          try {
            const [keys, apis] = await Promise.all([
              apiKeysApi.list(app.id),
              applicationsApi.getApis(app.id),
            ]);
            keysMap[app.id] = keys;
            apisMap[app.id] = apis;
          } catch {
            // Keep going for other apps
          }
        })
      );

      setAppKeys(keysMap);
      setAppApis(apisMap);
    } catch (err: any) {
      setError(err.message || 'Failed to load profile applications');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileData();
  }, []);

  const handleCopy = (text: string, identifier: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(identifier);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  if (loading) {
    return <LoadingSpinner message="Loading developer profile & connection credentials..." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Header */}
      <div>
        <h1 className="page-title" style={{ margin: 0 }}>
          Developer Profile & API Connections
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
          Overview of your operator profile, registered applications, OpenAPI spec URLs, and Sentinel API keys
        </p>
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchProfileData} />}

      {/* User Info Card */}
      <div
        className="card"
        style={{
          padding: '1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
          background: 'linear-gradient(135deg, var(--bg-card) 0%, var(--bg-surface) 100%)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div
            style={{
              width: '4rem',
              height: '4rem',
              borderRadius: 'var(--radius-full)',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px solid var(--primary-border)',
              fontSize: '1.5rem',
              fontWeight: 700,
            }}
          >
            {user?.name?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                {user?.name || 'Operator'}
              </h2>
              <span className="pill-badge pill-badge-green" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                <ShieldCheck style={{ width: 12, height: 12 }} />
                Verified Operator
              </span>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              {user?.email}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              <span>User ID: <code>#{user?.id || 1}</code></span>
              <span>•</span>
              <span>Member Since: <strong>{user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active'}</strong></span>
              <span>•</span>
              <span>Monitored Apps: <strong>{applications.length}</strong></span>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Link to="/applications" className="btn btn-primary btn-sm">
            <Layers style={{ width: 14, height: 14 }} />
            View Applications
          </Link>
        </div>
      </div>

      {/* Global Sentinel Gateway Reference */}
      <div
        className="card"
        style={{
          padding: '1.25rem',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          backgroundColor: 'var(--bg-card)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
          <Terminal style={{ width: 18, height: 18, color: 'var(--primary)' }} />
          <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
            Sentinel Gateway Global Forwarding Root
          </h3>
        </div>
        <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: '0 0 1rem 0' }}>
          Forward all API consumer requests through Sentinel to enable real-time observability, circuit breaking, token rate limits, and audit telemetry.
        </p>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-md)',
            fontFamily: 'var(--font-mono)',
            fontSize: '0.8125rem',
            color: 'var(--primary)',
            fontWeight: 600,
          }}
        >
          <span>{gatewayBaseUrl}/*</span>
          <button
            type="button"
            onClick={() => handleCopy(gatewayBaseUrl, 'globalGateway')}
            className="btn btn-ghost btn-sm"
            style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.25rem' }}
          >
            {copiedKey === 'globalGateway' ? (
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

      {/* Applications & API Credentials Directory */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
              Your Applications & Connection Endpoints
            </h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: '0.15rem 0 0 0' }}>
              Target backend URLs, OpenAPI discovery specs, and active Sentinel API Keys for each application
            </p>
          </div>
        </div>

        {applications.length === 0 ? (
          <div className="card" style={{ padding: '2rem', textAlign: 'center' }}>
            <Layers style={{ width: '2.5rem', height: '2.5rem', color: 'var(--text-muted)', margin: '0 auto 0.75rem' }} />
            <h4 style={{ margin: 0, fontSize: '1rem', color: 'var(--text-primary)' }}>No Applications Registered</h4>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              Add an application to get started with API observability and auto-discovery.
            </p>
            <Link to="/applications" className="btn btn-primary btn-sm" style={{ marginTop: '1rem' }}>
              Import Application
            </Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {applications.map((app) => {
              const keys = appKeys[app.id] || [];
              const activeKey = keys.find((k) => k.active) || keys[0];
              const endpoints = appApis[app.id] || [];

              // Robust sanitize base URL (strip tracking query params like ?utm_source=... and trailing slashes)
              const cleanBase = app.baseUrl
                ? app.baseUrl.split('?')[0].split('#')[0].replace(/\/$/, '')
                : '';
              const candidateSpecUrls = [
                `${cleanBase}/api/v1/openapi.json`,
                `${cleanBase}/v3/api-docs`,
                `${cleanBase}/openapi.json`,
                `${cleanBase}/swagger/v1/swagger.json`,
                `${cleanBase}/api-docs`,
              ];

              return (
                <div
                  key={app.id}
                  className="card"
                  style={{
                    padding: '1.25rem',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-lg)',
                    backgroundColor: 'var(--bg-card)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '1rem',
                  }}
                >
                  {/* App Title & Quick Actions */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
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
                          border: '1px solid var(--primary-border)',
                        }}
                      >
                        <Globe style={{ width: '1.25rem', height: '1.25rem' }} />
                      </div>
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <h4 style={{ fontSize: '1.0625rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                            {app.name}
                          </h4>
                          <StatusBadge status={app.healthStatus} />
                        </div>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          Application ID: <code>#{app.id}</code> • {endpoints.length} APIs Cataloged
                        </span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        onClick={() => setSelectedAppForImport(app)}
                        className="btn btn-secondary btn-sm"
                        style={{ gap: '0.375rem' }}
                      >
                        <FileCode style={{ width: 14, height: 14 }} />
                        Import APIs
                      </button>
                      <Link to={`/applications/${app.id}`} className="btn btn-primary btn-sm" style={{ gap: '0.375rem' }}>
                        <span>Inspect Application</span>
                        <ArrowRight style={{ width: 14, height: 14 }} />
                      </Link>
                    </div>
                  </div>

                  {/* Attributes Grid */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1rem' }}>
                    {/* 1. Target Backend URL */}
                    <div
                      style={{
                        padding: '0.875rem',
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
                        <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                          Target Backend URL
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', color: 'var(--text-primary)', fontWeight: 600, wordBreak: 'break-all' }}>
                          {app.baseUrl || 'Not configured'}
                        </div>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '0.375rem' }}>
                        <button
                          type="button"
                          onClick={() => handleCopy(app.baseUrl, `base-${app.id}`)}
                          className="btn btn-ghost btn-xs"
                          style={{ gap: '0.25rem' }}
                        >
                          {copiedKey === `base-${app.id}` ? (
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

                    {/* 2. Primary OpenAPI Spec URL */}
                    <div
                      style={{
                        padding: '0.875rem',
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
                          <span style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                            OpenAPI / Swagger Spec URL
                          </span>
                          <span style={{ fontSize: '0.6875rem', color: 'var(--primary)', fontWeight: 600 }}>Recommended</span>
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', color: 'var(--text-primary)', fontWeight: 600, wordBreak: 'break-all' }}>
                          {candidateSpecUrls[0]}
                        </div>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.375rem' }}>
                        <button
                          type="button"
                          onClick={() => setSelectedAppForImport(app)}
                          className="btn btn-link btn-xs"
                          style={{ padding: 0, fontSize: '0.75rem' }}
                        >
                          Import into Catalog →
                        </button>
                        <button
                          type="button"
                          onClick={() => handleCopy(candidateSpecUrls[0], `spec-${app.id}`)}
                          className="btn btn-ghost btn-xs"
                          style={{ gap: '0.25rem' }}
                        >
                          {copiedKey === `spec-${app.id}` ? (
                            <>
                              <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                              <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                            </>
                          ) : (
                            <>
                              <Copy style={{ width: 12, height: 12 }} />
                              <span>Copy Spec URL</span>
                            </>
                          )}
                        </button>
                      </div>
                    </div>

                    {/* 3. Active Sentinel API Key */}
                    <div
                      style={{
                        padding: '0.875rem',
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
                          <span style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                            Developer API Key
                          </span>
                          <span className="pill-badge pill-badge-blue" style={{ fontSize: '0.6875rem' }}>
                            {activeKey ? `${activeKey.rateLimitPerMinute} req/min` : 'No Key'}
                          </span>
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', color: 'var(--text-primary)', fontWeight: 600 }}>
                          {activeKey ? (activeKey.maskedKey || `sk_sentinel_••••••••${activeKey.id}`) : 'No active API key'}
                        </div>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.375rem' }}>
                        <Link to={`/applications/${app.id}/keys`} style={{ fontSize: '0.75rem', color: 'var(--primary)', textDecoration: 'none', fontWeight: 600 }}>
                          Manage Keys ({keys.length}) →
                        </Link>
                        {activeKey && (
                          <button
                            type="button"
                            onClick={() => handleCopy(activeKey.maskedKey || `sk_sentinel_${activeKey.id}`, `key-${app.id}`)}
                            className="btn btn-ghost btn-xs"
                            style={{ gap: '0.25rem' }}
                          >
                            {copiedKey === `key-${app.id}` ? (
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
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Sample cURL Header Format */}
                  <div
                    style={{
                      padding: '0.625rem 0.875rem',
                      backgroundColor: 'var(--bg-subtle)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.75rem',
                      fontFamily: 'var(--font-mono)',
                      color: 'var(--text-secondary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                    }}
                  >
                    <span>
                      curl -X GET &quot;{gatewayBaseUrl}/api/v1/health&quot; -H &quot;X-Sentinel-API-Key: {activeKey?.maskedKey || 'YOUR_KEY'}&quot;
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopy(`curl -X GET "${gatewayBaseUrl}/api/v1/health" -H "X-Sentinel-API-Key: ${activeKey?.maskedKey || 'YOUR_KEY'}"`, `curl-${app.id}`)}
                      className="btn btn-ghost btn-xs"
                      style={{ padding: '0.15rem 0.4rem', fontSize: '0.6875rem' }}
                    >
                      {copiedKey === `curl-${app.id}` ? 'Copied' : 'Copy cURL'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Import APIs Modal for Selected App */}
      {selectedAppForImport && (
        <ImportApiModal
          isOpen={!!selectedAppForImport}
          application={selectedAppForImport}
          onClose={() => setSelectedAppForImport(null)}
          onSuccess={() => {
            fetchProfileData();
          }}
        />
      )}
    </div>
  );
};
