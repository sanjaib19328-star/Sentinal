import React, { useEffect, useState } from 'react';
import { consumersApi } from '../../api/consumers';
import { ConsumerKeyAnalytics } from '../../types/consumer';
import { KeyRound } from 'lucide-react';

export const TopConsumersTable: React.FC = () => {
  const [consumers, setConsumers] = useState<ConsumerKeyAnalytics[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTopConsumers();
  }, []);

  const loadTopConsumers = async () => {
    try {
      const res = await consumersApi.getGlobalTopConsumers(5);
      setConsumers(res);
    } catch (err) {
      console.error('Failed to load top consumers', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading || consumers.length === 0) return null;

  return (
    <div className="card" style={{ marginBottom: '1.5rem' }}>
      <div className="card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <KeyRound style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
          <div>
            <h3 className="card-title">Top API Consumers & Keys</h3>
            <p className="card-subtitle">Heaviest traffic generators with error rate and latency percentiles</p>
          </div>
        </div>
      </div>

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Consumer Key</th>
              <th>Total Requests</th>
              <th>Error Rate</th>
              <th>Avg Latency</th>
              <th>P95 Latency</th>
              <th>Top Endpoint</th>
              <th style={{ textAlign: 'right' }}>Last Active</th>
            </tr>
          </thead>
          <tbody>
            {consumers.map((c) => (
              <tr key={c.apiKeyId}>
                <td>
                  <div>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{c.keyName}</div>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {c.keyPrefix}... (Key #{c.apiKeyId})
                    </span>
                  </div>
                </td>
                <td style={{ fontWeight: 600 }}>{c.totalRequests.toLocaleString()}</td>
                <td>
                  <span style={{ color: c.errorRate > 5 ? 'var(--danger-text)' : 'inherit', fontWeight: c.errorRate > 0 ? 600 : 400 }}>
                    {c.errorRate}%
                  </span>
                </td>
                <td style={{ fontFamily: 'monospace' }}>{c.avgLatencyMs}ms</td>
                <td style={{ fontFamily: 'monospace', color: 'var(--primary)' }}>{c.p95LatencyMs}ms</td>
                <td>
                  {c.topEndpoints && c.topEndpoints.length > 0 ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.75rem' }}>
                      <span className="badge badge-primary" style={{ fontSize: '0.6875rem', padding: '0.1rem 0.35rem' }}>
                        {c.topEndpoints[0].method}
                      </span>
                      <code style={{ fontFamily: 'monospace' }}>{c.topEndpoints[0].normalizedPath}</code>
                    </div>
                  ) : (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>-</span>
                  )}
                </td>
                <td style={{ textAlign: 'right', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {c.lastUsedAt ? new Date(c.lastUsedAt).toLocaleTimeString() : 'Never'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
