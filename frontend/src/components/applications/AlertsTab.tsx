import React, { useEffect, useState } from 'react';
import { Alert, AlertRule, CreateAlertRuleRequest, AlertRuleType } from '../../types/alert';
import { ApiEndpoint } from '../../types/application';
import {
  listAlerts,
  listAlertRules,
  createAlertRule,
  deleteAlertRule,
  acknowledgeAlert,
  resolveAlert,
} from '../../api/alerts';
import { getErrorMessage } from '../../api/client';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorBanner } from '../common/ErrorBanner';
import { EmptyState } from '../common/EmptyState';
import {
  Bell,
  Plus,
  Trash2,
  ShieldAlert,
  Check,
  X,
} from 'lucide-react';

interface AlertsTabProps {
  applicationId: number;
  endpoints: ApiEndpoint[];
}

export const AlertsTab: React.FC<AlertsTabProps> = ({ applicationId, endpoints }) => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [ruleType, setRuleType] = useState<AlertRuleType>('HIGH_ERROR_RATE');
  const [threshold, setThreshold] = useState(10);
  const [windowSeconds, setWindowSeconds] = useState(300);
  const [endpointId, setEndpointId] = useState<string>('');
  const [creating, setCreating] = useState(false);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const fetchAlertsData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [alertsData, rulesData] = await Promise.all([
        listAlerts(applicationId),
        listAlertRules(applicationId),
      ]);
      setAlerts(alertsData);
      setRules(rulesData);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlertsData();
  }, [applicationId]);

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setError(null);

    const payload: CreateAlertRuleRequest = {
      type: ruleType,
      threshold,
      windowSeconds,
      endpointId: endpointId ? parseInt(endpointId, 10) : null,
      enabled: true,
    };

    try {
      await createAlertRule(applicationId, payload);
      setIsCreateModalOpen(false);
      await fetchAlertsData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteRule = async (ruleId: number) => {
    if (!window.confirm('Delete this alert rule?')) return;
    try {
      await deleteAlertRule(applicationId, ruleId);
      await fetchAlertsData();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleAcknowledge = async (alertId: number) => {
    setActionLoading(alertId);
    try {
      await acknowledgeAlert(alertId);
      await fetchAlertsData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(null);
    }
  };

  const handleResolve = async (alertId: number) => {
    setActionLoading(alertId);
    try {
      await resolveAlert(alertId);
      await fetchAlertsData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {error && <ErrorBanner message={error} onRetry={fetchAlertsData} />}

      {/* Alert Rules Section */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Configured Alert Rules</h3>
            <p className="card-subtitle">
              Continuous monitoring thresholds for error rate spikes, latency surges, and availability
            </p>
          </div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="btn btn-primary btn-sm"
          >
            <Plus style={{ width: '0.875rem', height: '0.875rem' }} />
            Add Alert Rule
          </button>
        </div>

        {loading ? (
          <div style={{ padding: '2rem' }}>
            <LoadingSpinner message="Loading alert configuration..." />
          </div>
        ) : rules.length === 0 ? (
          <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            No alert rules defined yet. Click "Add Alert Rule" to configure real-time threshold monitoring.
          </div>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Rule Type</th>
                  <th>Scope</th>
                  <th>Threshold</th>
                  <th>Window</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => {
                  const ep = endpoints.find((e) => e.id === rule.endpointId);
                  return (
                    <tr key={rule.id}>
                      <td style={{ fontWeight: 600 }}>
                        <span
                          style={{
                            padding: '0.2rem 0.5rem',
                            borderRadius: '0.25rem',
                            fontSize: '0.75rem',
                            background: 'rgba(59, 130, 246, 0.1)',
                            color: 'var(--primary)',
                          }}
                        >
                          {rule.type}
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8125rem' }}>
                        {ep ? `${ep.method} ${ep.normalizedPath}` : '🌐 Entire Application'}
                      </td>
                      <td style={{ fontWeight: 600 }}>
                        {rule.type === 'HIGH_ERROR_RATE'
                          ? `${rule.threshold}% Error Rate`
                          : rule.type === 'HIGH_LATENCY'
                          ? `${rule.threshold}ms Latency`
                          : rule.type === 'EXCESSIVE_429'
                          ? `${rule.threshold} Throttled Reqs`
                          : `${rule.threshold} Failures`}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                        {rule.windowSeconds}s ({Math.round(rule.windowSeconds / 60)} min)
                      </td>
                      <td>
                        <span
                          style={{
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            color: rule.enabled ? 'var(--success)' : 'var(--text-muted)',
                          }}
                        >
                          {rule.enabled ? '● Active' : '○ Paused'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          onClick={() => handleDeleteRule(rule.id)}
                          className="btn btn-secondary btn-sm"
                          style={{ color: 'var(--danger)' }}
                        >
                          <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Triggered Alerts History */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Triggered Alerts History</h3>
            <p className="card-subtitle">
              Live alert instances triggered by real traffic anomalies
            </p>
          </div>
          <ShieldAlert style={{ width: '1.25rem', height: '1.25rem', color: 'var(--danger)' }} />
        </div>

        {loading ? (
          <div style={{ padding: '2rem' }}>
            <LoadingSpinner message="Loading alert history..." />
          </div>
        ) : alerts.length === 0 ? (
          <EmptyState
            icon={Bell}
            title="No Alerts Triggered"
            description="All systems are operating normally. When metric anomalies exceed your configured thresholds, triggered alerts will appear here."
          />
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Severity</th>
                  <th>Message</th>
                  <th>Observed Value</th>
                  <th>Triggered Time</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td>
                      <span
                        style={{
                          padding: '0.2rem 0.5rem',
                          borderRadius: '0.25rem',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          background:
                            alert.status === 'ACTIVE'
                              ? 'rgba(239, 68, 68, 0.15)'
                              : alert.status === 'ACKNOWLEDGED'
                              ? 'rgba(245, 158, 11, 0.15)'
                              : 'rgba(34, 197, 94, 0.15)',
                          color:
                            alert.status === 'ACTIVE'
                              ? 'var(--danger)'
                              : alert.status === 'ACKNOWLEDGED'
                              ? 'var(--warning)'
                              : 'var(--success)',
                        }}
                      >
                        {alert.status}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>{alert.severity}</span>
                    </td>
                    <td style={{ fontWeight: 500, fontSize: '0.875rem' }}>{alert.message}</td>
                    <td style={{ fontWeight: 600, fontSize: '0.8125rem' }}>
                      {alert.triggeredValue} <span style={{ color: 'var(--text-muted)' }}>(Threshold: {alert.threshold})</span>
                    </td>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {new Date(alert.triggeredAt).toLocaleString()}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.375rem' }}>
                        {alert.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleAcknowledge(alert.id)}
                            disabled={actionLoading === alert.id}
                            className="btn btn-secondary btn-sm"
                          >
                            Ack
                          </button>
                        )}
                        {alert.status !== 'RESOLVED' && (
                          <button
                            onClick={() => handleResolve(alert.id)}
                            disabled={actionLoading === alert.id}
                            className="btn btn-primary btn-sm"
                            style={{ background: 'var(--success)', borderColor: 'var(--success)' }}
                          >
                            <Check style={{ width: '0.75rem', height: '0.75rem' }} />
                            Resolve
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Alert Rule Modal */}
      {isCreateModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <div
            className="card"
            style={{
              width: '100%',
              maxWidth: '520px',
              background: 'var(--surface-card, #ffffff)',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
            }}
          >
            <div className="card-header">
              <h3 className="card-title">Create Alert Rule</h3>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="btn btn-secondary btn-sm"
              >
                <X style={{ width: '1rem', height: '1rem' }} />
              </button>
            </div>
            <form onSubmit={handleCreateRule} style={{ padding: '1.5rem' }}>
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Alert Condition Type</label>
                <select
                  value={ruleType}
                  onChange={(e) => setRuleType(e.target.value as AlertRuleType)}
                  className="form-input"
                >
                  <option value="HIGH_ERROR_RATE">High Error Rate (% of requests failing)</option>
                  <option value="HIGH_LATENCY">High Latency (P95 latency exceeding ms)</option>
                  <option value="API_UNAVAILABLE">Service Unavailable / 502 Bad Gateway</option>
                  <option value="EXCESSIVE_429">Excessive Rate Limit Violations (429 count)</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Target Scope</label>
                <select
                  value={endpointId}
                  onChange={(e) => setEndpointId(e.target.value)}
                  className="form-input"
                >
                  <option value="">🌐 Entire Application</option>
                  {endpoints.map((ep) => (
                    <option key={ep.id} value={String(ep.id)}>
                      {ep.method} {ep.normalizedPath}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">
                  {ruleType === 'HIGH_ERROR_RATE'
                    ? 'Error Rate Threshold (%)'
                    : ruleType === 'HIGH_LATENCY'
                    ? 'Latency Threshold (ms)'
                    : ruleType === 'EXCESSIVE_429'
                    ? '429 Violations Threshold (Count)'
                    : 'Failure Threshold (Count)'}
                </label>
                <input
                  type="number"
                  min="1"
                  value={threshold}
                  onChange={(e) => setThreshold(parseFloat(e.target.value) || 1)}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                <label className="form-label">Evaluation Window (Seconds)</label>
                <select
                  value={windowSeconds}
                  onChange={(e) => setWindowSeconds(parseInt(e.target.value, 10))}
                  className="form-input"
                >
                  <option value={60}>60 Seconds (1 Minute)</option>
                  <option value={300}>300 Seconds (5 Minutes)</option>
                  <option value={600}>600 Seconds (10 Minutes)</option>
                  <option value={3600}>3600 Seconds (1 Hour)</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="btn btn-primary"
                >
                  {creating ? 'Creating Rule...' : 'Create Alert Rule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
