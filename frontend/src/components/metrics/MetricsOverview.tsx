import React, { useEffect, useState } from 'react';
import { MetricItem } from '../../types/metrics';
import { TimeSeriesPoint, TrafficBreakdown } from '../../types/analytics';
import { getTimeSeries, getTrafficBreakdown, getApplicationAnalytics } from '../../api/analytics';
import { ApiEndpointAnalytics } from '../../types/application';
import { EmptyState } from '../common/EmptyState';
import { LoadingSpinner } from '../common/LoadingSpinner';
import {
  BarChart3,
  AlertTriangle,
  Clock,
  Activity,
  Zap,
  TrendingUp,
  RotateCcw,
} from 'lucide-react';

interface MetricsOverviewProps {
  applicationId: number;
  metrics?: MetricItem[];
}

export const MetricsOverview: React.FC<MetricsOverviewProps> = ({ applicationId, metrics = [] }) => {
  const [interval, setInterval] = useState<string>('minute');
  const [timeSeries, setTimeSeries] = useState<TimeSeriesPoint[]>([]);
  const [breakdown, setBreakdown] = useState<TrafficBreakdown | null>(null);
  const [analytics, setAnalytics] = useState<ApiEndpointAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchMetricsData = async () => {
    setLoading(true);
    try {
      const [tsData, bdData, anData] = await Promise.all([
        getTimeSeries(applicationId, { interval }),
        getTrafficBreakdown(applicationId),
        getApplicationAnalytics(applicationId),
      ]);
      setTimeSeries(tsData.points);
      setBreakdown(bdData);
      setAnalytics(anData);
    } catch (err) {
      console.error('Failed to load metrics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMetricsData();
  }, [applicationId, interval]);

  if (!loading && (!analytics || analytics.totalRequests === 0) && metrics.length === 0) {
    return (
      <EmptyState
        icon={BarChart3}
        title="No Telemetry Metrics Available"
        description="Sentinel has not observed any requests or health probes for this application yet. Run a connection test or generate gateway traffic to populate real metrics."
      />
    );
  }

  const maxPointCount = Math.max(...timeSeries.map((p) => p.totalRequests), 1);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Top Percentiles & Latency Cards */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-label">
            <Zap style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
            <span>Total Volume</span>
          </div>
          <div className="stat-value">{analytics ? analytics.totalRequests.toLocaleString() : 0}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            {analytics ? `${analytics.successCount} ok · ${analytics.errorCount} err` : ''}
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <Clock style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
            <span>Average Latency</span>
          </div>
          <div className="stat-value">{analytics ? `${analytics.avgLatencyMs}ms` : '0ms'}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Real round-trip time
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <TrendingUp style={{ width: '1rem', height: '1rem', color: 'var(--success)' }} />
            <span>P50 Latency</span>
          </div>
          <div className="stat-value">{analytics ? `${analytics.p50LatencyMs}ms` : '0ms'}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Median response time
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <AlertTriangle style={{ width: '1rem', height: '1rem', color: 'var(--warning)' }} />
            <span>P95 Latency</span>
          </div>
          <div className="stat-value">{analytics ? `${analytics.p95LatencyMs}ms` : '0ms'}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            95th percentile
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-label">
            <Activity style={{ width: '1rem', height: '1rem', color: 'var(--danger)' }} />
            <span>P99 Latency</span>
          </div>
          <div className="stat-value">{analytics ? `${analytics.p99LatencyMs}ms` : '0ms'}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Tail latency ceiling
          </div>
        </div>
      </div>

      {/* Time-Series Bucket Throughput Chart */}
      <div className="card">
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h3 className="card-title">Real-Time Traffic Throughput</h3>
            <p className="card-subtitle">Aggregated gateway requests and errors over time intervals</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>Interval:</span>
            <select
              value={interval}
              onChange={(e) => setInterval(e.target.value)}
              className="form-input"
              style={{ width: 'auto', padding: '0.25rem 0.5rem', fontSize: '0.8125rem' }}
            >
              <option value="minute">1 Minute Buckets</option>
              <option value="5minute">5 Minute Buckets</option>
              <option value="hour">1 Hour Buckets</option>
              <option value="day">1 Day Buckets</option>
            </select>
            <button onClick={fetchMetricsData} className="btn btn-secondary btn-sm">
              <RotateCcw style={{ width: '0.75rem', height: '0.75rem' }} />
            </button>
          </div>
        </div>

        <div style={{ padding: '1.5rem' }}>
          {loading ? (
            <LoadingSpinner message="Aggregating time-series telemetry..." />
          ) : timeSeries.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>No time-series data available</p>
          ) : (
            <div>
              {/* Visual Bars Container */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-end',
                  gap: '0.375rem',
                  height: '140px',
                  paddingBottom: '0.5rem',
                  borderBottom: '1px solid var(--border-color)',
                  overflowX: 'auto',
                }}
              >
                {timeSeries.map((pt, idx) => {
                  const barHeight = Math.max(4, (pt.totalRequests / maxPointCount) * 120);
                  const isErr = pt.errorRequests > 0;
                  return (
                    <div
                      key={idx}
                      title={`Time: ${new Date(pt.timestamp).toLocaleTimeString()} | Total: ${pt.totalRequests} | Errors: ${pt.errorRequests} | 429s: ${pt.rateLimitedRequests} | Latency: ${pt.avgLatencyMs}ms`}
                      style={{
                        flex: '1 0 16px',
                        maxWidth: '32px',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'flex-end',
                        alignItems: 'center',
                        height: '100%',
                        cursor: 'pointer',
                      }}
                    >
                      <div
                        style={{
                          width: '100%',
                          height: `${barHeight}px`,
                          background: isErr ? 'var(--danger)' : pt.totalRequests > 0 ? 'var(--primary)' : 'var(--surface-subtle, #e2e8f0)',
                          borderRadius: '0.25rem 0.25rem 0 0',
                          transition: 'height 0.2s',
                        }}
                      />
                    </div>
                  );
                })}
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                <span>{timeSeries.length > 0 ? new Date(timeSeries[0].timestamp).toLocaleTimeString() : ''}</span>
                <div style={{ display: 'flex', gap: '1rem' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <span style={{ width: '8px', height: '8px', borderRadius: '2px', background: 'var(--primary)', display: 'inline-block' }} /> Success
                  </span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    <span style={{ width: '8px', height: '8px', borderRadius: '2px', background: 'var(--danger)', display: 'inline-block' }} /> Errors / 429
                  </span>
                </div>
                <span>{timeSeries.length > 0 ? new Date(timeSeries[timeSeries.length - 1].timestamp).toLocaleTimeString() : ''}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Traffic Breakdown Distribution Cards */}
      {breakdown && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {/* Method Breakdown */}
          <div className="card">
            <div className="card-header">
              <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>Traffic by HTTP Method</h4>
            </div>
            <div style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {Object.entries(breakdown.methodCounts).map(([m, count]) => {
                const total = Object.values(breakdown.methodCounts).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;
                return (
                  <div key={m}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 600 }}>{m}</span>
                      <span style={{ color: 'var(--text-muted)' }}>{count} reqs ({pct}%)</span>
                    </div>
                    <div style={{ background: 'var(--surface-subtle, #f1f5f9)', borderRadius: '0.25rem', height: '0.5rem', overflow: 'hidden' }}>
                      <div style={{ background: 'var(--primary)', width: `${pct}%`, height: '100%' }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Status Class Breakdown */}
          <div className="card">
            <div className="card-header">
              <h4 className="card-title" style={{ fontSize: '0.9375rem' }}>Traffic by Status Class</h4>
            </div>
            <div style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {Object.entries(breakdown.statusClassCounts).map(([cls, count]) => {
                const total = Object.values(breakdown.statusClassCounts).reduce((a, b) => a + b, 0);
                const pct = total > 0 ? Math.round((count / total) * 100) : 0;
                const color = cls === '2xx' ? 'var(--success)' : cls === '3xx' ? 'var(--primary)' : cls === '4xx' ? 'var(--warning)' : 'var(--danger)';
                return (
                  <div key={cls}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 600 }}>HTTP {cls}</span>
                      <span style={{ color: 'var(--text-muted)' }}>{count} reqs ({pct}%)</span>
                    </div>
                    <div style={{ background: 'var(--surface-subtle, #f1f5f9)', borderRadius: '0.25rem', height: '0.5rem', overflow: 'hidden' }}>
                      <div style={{ background: color, width: `${pct}%`, height: '100%' }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
