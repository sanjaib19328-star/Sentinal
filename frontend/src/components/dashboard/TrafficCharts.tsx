import React from 'react';
import { GlobalDashboardResponse } from '../../types/analytics';
import { BarChart2, PieChart } from 'lucide-react';

interface TrafficChartsProps {
  summary: GlobalDashboardResponse | null;
}

export const TrafficCharts: React.FC<TrafficChartsProps> = ({ summary }) => {
  if (!summary) return null;

  const total = summary.totalRequests || 0;
  const successRate = summary.overallSuccessRate || 0;
  const errorRate = summary.overallErrorRate || 0;
  const rateLimitRate = summary.overall429Rate || 0;

  const successCount = Math.round((total * successRate) / 100);
  const errorCount = Math.round((total * errorRate) / 100);
  const throttledCount = Math.round((total * rateLimitRate) / 100);

  // App breakdown
  const appSummaries = summary.applicationSummaries || (summary as any).topApplications || [];
  const totalAppRequests = appSummaries.reduce((acc: number, a: any) => acc + (a.totalRequests || a.requestCount || 0), 0) || 1;

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
      
      {/* Status Code Distribution Card */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Status Code Distribution</h3>
            <p className="card-subtitle">Gateway traffic responses by status class</p>
          </div>
          <PieChart style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
        </div>

        <div style={{ padding: '1.25rem' }}>
          {total === 0 ? (
            <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>
              No traffic recorded yet.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {/* 2xx Success */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.375rem' }}>
                  <span style={{ fontWeight: 600, color: 'var(--success-text)' }}>2xx Success ({summary.overallSuccessRate}%)</span>
                  <span style={{ fontFamily: 'monospace' }}>{successCount.toLocaleString()} reqs</span>
                </div>
                <div style={{ height: '8px', background: 'var(--surface-subtle, #1e293b)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${summary.overallSuccessRate}%`, background: 'var(--success)' }} />
                </div>
              </div>

              {/* 4xx / 5xx Errors */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.375rem' }}>
                  <span style={{ fontWeight: 600, color: 'var(--danger-text)' }}>4xx / 5xx Errors ({summary.overallErrorRate}%)</span>
                  <span style={{ fontFamily: 'monospace' }}>{errorCount.toLocaleString()} reqs</span>
                </div>
                <div style={{ height: '8px', background: 'var(--surface-subtle, #1e293b)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${summary.overallErrorRate}%`, background: 'var(--danger)' }} />
                </div>
              </div>

              {/* 429 Throttled */}
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.375rem' }}>
                  <span style={{ fontWeight: 600, color: 'var(--warning-text)' }}>429 Rate Limited ({summary.overall429Rate}%)</span>
                  <span style={{ fontFamily: 'monospace' }}>{throttledCount.toLocaleString()} reqs</span>
                </div>
                <div style={{ height: '8px', background: 'var(--surface-subtle, #1e293b)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${summary.overall429Rate}%`, background: 'var(--warning)' }} />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Traffic Distribution by Connected Application */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Traffic by Application</h3>
            <p className="card-subtitle">Request volume split across managed microservices</p>
          </div>
          <BarChart2 style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
        </div>

        <div style={{ padding: '1.25rem' }}>
          {appSummaries.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '0.875rem' }}>
              No applications registered yet.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {appSummaries.slice(0, 4).map((app: any) => {
                const reqCount = app.totalRequests || app.requestCount || 0;
                const pct = Math.round((reqCount * 100) / totalAppRequests);
                return (
                  <div key={app.id}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{app.name}</span>
                      <span style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>
                        {reqCount.toLocaleString()} reqs ({pct}%)
                      </span>
                    </div>
                    <div style={{ height: '6px', background: 'var(--surface-subtle, #1e293b)', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${pct}%`, background: 'var(--primary)' }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
