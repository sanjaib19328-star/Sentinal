import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { applicationsApi } from '../api/applications';
import { getGlobalDashboard } from '../api/analytics';
import { acknowledgeAlert, resolveAlert } from '../api/alerts';
import { Application } from '../types/application';
import { GlobalDashboardResponse } from '../types/analytics';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorBanner } from '../components/common/ErrorBanner';
import { CreateApplicationModal } from '../components/applications/CreateApplicationModal';
import { SystemHealthWidget } from '../components/common/SystemHealthWidget';
import { TopConsumersTable } from '../components/common/TopConsumersTable';
import { TrafficCharts } from '../components/dashboard/TrafficCharts';
import { getErrorMessage } from '../api/client';
import {
  Layers,
  CheckCircle2,
  Plus,
  ArrowRight,
  Globe,
  Activity,
  Zap,
  TrendingUp,
  AlertOctagon,
  Clock,
  ShieldAlert,
  Check,
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const [applications, setApplications] = useState<Application[]>([]);
  const [summary, setSummary] = useState<GlobalDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [alertActionLoading, setAlertActionLoading] = useState<number | null>(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [appsData, summaryData] = await Promise.all([
        applicationsApi.list(),
        getGlobalDashboard(),
      ]);
      setApplications(appsData);
      setSummary(summaryData);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleAcknowledgeAlert = async (alertId: number) => {
    setAlertActionLoading(alertId);
    try {
      await acknowledgeAlert(alertId);
      await fetchDashboardData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setAlertActionLoading(null);
    }
  };

  const handleResolveAlert = async (alertId: number) => {
    setAlertActionLoading(alertId);
    try {
      await resolveAlert(alertId);
      await fetchDashboardData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setAlertActionLoading(null);
    }
  };

  const totalApps = summary ? summary.totalApplications : applications.length;
  const healthyApps = summary ? summary.healthyApplications : applications.filter((a) => a.healthStatus === 'HEALTHY').length;
  const degradedApps = summary ? summary.degradedApplications : applications.filter((a) => a.healthStatus === 'DEGRADED').length;
  const unavailableApps = summary ? summary.downApplications : applications.filter((a) => a.healthStatus === 'UNAVAILABLE').length;

  return (
    <div>
      {/* Page Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.75rem',
        }}
      >
        <div>
          <h1 className="page-title">Executive API Gateway Operations</h1>
          <p className="page-subtitle">
            Centralized traffic routing, policies, system health, and developer analytics
          </p>
        </div>

        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="btn btn-primary"
        >
          <Plus style={{ width: '1.125rem', height: '1.125rem' }} />
          Register Application
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchDashboardData} />}

      {loading ? (
        <LoadingSpinner message="Aggregating cluster telemetry and gateway telemetry..." />
      ) : (
        <>
          {/* Phase 3 System Health Widget */}
          <SystemHealthWidget />

          {/* Active Incidents Banner (if any) */}
          {summary && summary.activeAlerts && summary.activeAlerts.length > 0 && (
            <div
              style={{
                marginBottom: '1.5rem',
                backgroundColor: 'rgba(239, 68, 68, 0.08)',
                border: '1px solid rgba(239, 68, 68, 0.25)',
                borderRadius: 'var(--radius-lg)',
                padding: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--danger-text)', fontWeight: 600 }}>
                  <ShieldAlert style={{ width: '1.25rem', height: '1.25rem', color: 'var(--danger)' }} />
                  <span>Active Gateway Incidents ({summary.activeAlerts.length})</span>
                </div>
                <Link to="/alerts" className="btn btn-secondary btn-sm" style={{ fontSize: '0.75rem' }}>
                  Manage Alerts
                </Link>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {summary.activeAlerts.slice(0, 3).map((incident) => (
                  <div
                    key={incident.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      backgroundColor: 'var(--bg-surface)',
                      padding: '0.625rem 0.875rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      fontSize: '0.8125rem',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span
                        className={`badge ${incident.severity === 'CRITICAL' ? 'badge-unavailable' : 'badge-degraded'}`}
                        style={{ fontSize: '0.6875rem' }}
                      >
                        {incident.severity}
                      </span>
                      <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                        App #{incident.applicationId}:
                      </span>
                      <span style={{ color: 'var(--text-secondary)' }}>{incident.message}</span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {new Date(incident.triggeredAt).toLocaleTimeString()}
                      </span>
                      {incident.status === 'ACTIVE' && (
                        <button
                          onClick={() => handleAcknowledgeAlert(incident.id)}
                          disabled={alertActionLoading === incident.id}
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                        >
                          Ack
                        </button>
                      )}
                      <button
                        onClick={() => handleResolveAlert(incident.id)}
                        disabled={alertActionLoading === incident.id}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', color: 'var(--success)' }}
                      >
                        <Check style={{ width: '0.75rem', height: '0.75rem' }} />
                        Resolve
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Primary KPI Metrics Cards */}
          <div className="kpi-grid">
            <div className="kpi-card">
              <div className="kpi-header">
                <span className="kpi-title">Applications</span>
                <div className="kpi-icon-pill" style={{ backgroundColor: '#eff6ff', color: 'var(--primary)' }}>
                  <Layers style={{ width: '1rem', height: '1rem' }} />
                </div>
              </div>
              <div className="kpi-value">{totalApps}</div>
              <div className="kpi-footer">
                <span style={{ color: 'var(--success)' }}>{healthyApps} Healthy</span> · 
                <span style={{ color: degradedApps > 0 ? 'var(--warning)' : 'inherit' }}>{degradedApps} Degraded</span> · 
                <span style={{ color: unavailableApps > 0 ? 'var(--danger)' : 'inherit' }}>{unavailableApps} Down</span>
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-header">
                <span className="kpi-title">Gateway Traffic</span>
                <div className="kpi-icon-pill" style={{ backgroundColor: '#eff6ff', color: 'var(--primary)' }}>
                  <Activity style={{ width: '1rem', height: '1rem' }} />
                </div>
              </div>
              <div className="kpi-value">{summary ? summary.totalRequests.toLocaleString() : '0'}</div>
              <div className="kpi-footer">
                Throughput: {summary ? summary.requestsPerMinute : 0} req/min
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-header">
                <span className="kpi-title">Success Rate</span>
                <div className="kpi-icon-pill" style={{ backgroundColor: '#ecfdf5', color: 'var(--success)' }}>
                  <CheckCircle2 style={{ width: '1rem', height: '1rem' }} />
                </div>
              </div>
              <div className="kpi-value" style={{ color: 'var(--success-text)' }}>
                {summary ? `${summary.overallSuccessRate}%` : '100%'}
              </div>
              <div className="kpi-footer">
                Error Rate: <strong style={{ color: summary && summary.overallErrorRate > 0 ? 'var(--danger)' : 'inherit' }}>{summary ? `${summary.overallErrorRate}%` : '0%'}</strong>
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-header">
                <span className="kpi-title">429 Throttled</span>
                <div className="kpi-icon-pill" style={{ backgroundColor: '#fffbeb', color: 'var(--warning)' }}>
                  <Zap style={{ width: '1rem', height: '1rem' }} />
                </div>
              </div>
              <div className="kpi-value" style={{ color: summary && summary.overall429Rate > 0 ? 'var(--warning-text)' : 'inherit' }}>
                {summary ? `${summary.overall429Rate}%` : '0%'}
              </div>
              <div className="kpi-footer">
                Multi-level rate limit & quotas
              </div>
            </div>

            <div className="kpi-card">
              <div className="kpi-header">
                <span className="kpi-title">Avg / P95 Latency</span>
                <div className="kpi-icon-pill" style={{ backgroundColor: '#eff6ff', color: 'var(--primary)' }}>
                  <Clock style={{ width: '1rem', height: '1rem' }} />
                </div>
              </div>
              <div className="kpi-value">
                {summary ? `${summary.avgLatencyMs}ms` : '0ms'}
              </div>
              <div className="kpi-footer">
                P95 Percentile: {summary ? `${summary.p95LatencyMs}ms` : '0ms'}
              </div>
            </div>
          </div>

          {/* Phase 4 Traffic Breakdown Charts */}
          <TrafficCharts summary={summary} />

          {/* Phase 3 Top Consumers Table */}
          <TopConsumersTable />

          {/* Grid Layout: Top APIs & Errors */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
            {/* Top APIs Cross-Application */}
            <div className="card">
              <div className="card-header">
                <div>
                  <h3 className="card-title">Top APIs by Traffic</h3>
                  <p className="card-subtitle">Most active endpoints across all connected applications</p>
                </div>
                <TrendingUp style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
              </div>
              {summary && summary.topApis && summary.topApis.length > 0 ? (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Method</th>
                        <th>Path</th>
                        <th style={{ textAlign: 'right' }}>Requests</th>
                        <th style={{ textAlign: 'right' }}>Avg Latency</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.topApis.map((api, idx) => {
                        const m = api.method.toUpperCase();
                        let cls = 'method-pill-other';
                        if (m === 'GET') cls = 'method-pill-get';
                        else if (m === 'POST') cls = 'method-pill-post';
                        else if (m === 'PUT' || m === 'PATCH') cls = 'method-pill-put';
                        else if (m === 'DELETE') cls = 'method-pill-delete';
                        return (
                          <tr key={idx}>
                            <td>
                              <span className={`method-pill ${cls}`}>
                                {m}
                              </span>
                            </td>
                            <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', fontWeight: 600 }}>{api.normalizedPath}</td>
                            <td style={{ textAlign: 'right', fontWeight: 600 }}>{api.count.toLocaleString()}</td>
                            <td style={{ textAlign: 'right', color: 'var(--text-muted)' }}>{Math.round(api.metricValue)}ms</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p style={{ padding: '1.5rem', color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>
                  No gateway traffic recorded yet. Send requests through the Sentinel gateway to see live top APIs.
                </p>
              )}
            </div>

            {/* Recent Gateway Errors Feed */}
            <div className="card">
              <div className="card-header">
                <div>
                  <h3 className="card-title">Recent Gateway Errors</h3>
                  <p className="card-subtitle">Latest 4xx / 5xx HTTP responses across applications</p>
                </div>
                <AlertOctagon style={{ width: '1.25rem', height: '1.25rem', color: 'var(--danger)' }} />
              </div>
              {summary && summary.recentErrors && summary.recentErrors.length > 0 ? (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Status</th>
                        <th>Method & Path</th>
                        <th>Latency</th>
                        <th style={{ textAlign: 'right' }}>Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.recentErrors.slice(0, 5).map((err, idx) => (
                        <tr key={idx}>
                          <td>
                            <span
                              style={{
                                padding: '0.2rem 0.5rem',
                                borderRadius: 'var(--radius-sm)',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                fontFamily: 'var(--font-mono)',
                                background: err.statusCode >= 500 ? '#fef2f2' : '#fffbeb',
                                color: err.statusCode >= 500 ? '#b91c1c' : '#b45309',
                                border: `1px solid ${err.statusCode >= 500 ? '#fecaca' : '#fde68a'}`,
                              }}
                            >
                              {err.statusCode}
                            </span>
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem' }}>
                            <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{err.method}</span> {err.path}
                          </td>
                          <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>{err.latencyMs}ms</td>
                          <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            {new Date(err.timestamp).toLocaleTimeString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p style={{ padding: '1.5rem', color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>
                  No errors observed. All gateway traffic is returning successful responses!
                </p>
              )}
            </div>
          </div>

          {/* Applications Table */}
          <div className="card">
            <div className="card-header">
              <div>
                <h3 className="card-title">Connected Applications</h3>
                <p className="card-subtitle">
                  Managed applications with policies, API catalogs, keys, and alerts
                </p>
              </div>
              {totalApps > 0 && (
                <Link to="/applications" className="btn btn-secondary btn-sm">
                  View All ({totalApps})
                  <ArrowRight style={{ width: '0.875rem', height: '0.875rem' }} />
                </Link>
              )}
            </div>

            {totalApps === 0 ? (
              <EmptyState
                icon={Layers}
                title="No Applications Connected"
                description="You have not registered any applications with Sentinel yet. Register your first service to start monitoring health and gateway traffic."
                action={{
                  label: 'Register First Application',
                  onClick: () => setIsCreateModalOpen(true),
                }}
              />
            ) : (
              <div className="table-container">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Status</th>
                      <th>Application</th>
                      <th>Target URL</th>
                      <th>Traffic (Reqs)</th>
                      <th>Error Rate</th>
                      <th>Avg Latency</th>
                      <th style={{ textAlign: 'right' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {applications.slice(0, 6).map((app) => {
                      const summariesList = summary?.applicationSummaries || (summary as any)?.topApplications || [];
                      const appSummary: any = summariesList.find((s: any) => s.id === app.id);
                      const reqCount = appSummary ? (appSummary.totalRequests ?? appSummary.requestCount ?? 0) : 0;
                      return (
                        <tr key={app.id}>
                          <td>
                            <StatusBadge status={app.healthStatus} size="sm" />
                          </td>
                          <td>
                            <Link
                              to={`/applications/${app.id}`}
                              style={{
                                fontWeight: 600,
                                color: 'var(--text-primary)',
                                textDecoration: 'none',
                              }}
                            >
                              {app.name}
                            </Link>
                            {app.description && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                {app.description}
                              </div>
                            )}
                          </td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem' }}>
                              <Globe style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)' }} />
                              <span>{app.baseUrl}</span>
                            </div>
                          </td>
                          <td style={{ fontWeight: 600, fontSize: '0.875rem' }}>
                            {reqCount.toLocaleString()}
                          </td>
                          <td>
                            <span
                              style={{
                                fontSize: '0.8125rem',
                                color: appSummary && appSummary.errorRate > 5 ? 'var(--danger)' : 'inherit',
                                fontWeight: appSummary && appSummary.errorRate > 0 ? 600 : 'normal',
                              }}
                            >
                              {appSummary ? `${appSummary.errorRate}%` : '0%'}
                            </span>
                          </td>
                          <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                            {appSummary ? `${appSummary.avgLatencyMs}ms` : '0ms'}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <Link
                              to={`/applications/${app.id}`}
                              className="btn btn-secondary btn-sm"
                              style={{ padding: '0.25rem 0.625rem' }}
                            >
                              Manage
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* Create Modal */}
      <CreateApplicationModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(newApp) => {
          setIsCreateModalOpen(false);
          setApplications([newApp, ...applications]);
          fetchDashboardData();
        }}
      />
    </div>
  );
};
