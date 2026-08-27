import React, { useEffect, useState } from 'react';
import { systemHealthApi } from '../../api/systemHealth';
import { SystemHealthResponse } from '../../types/systemHealth';
import { Server, Database, Zap, Activity } from 'lucide-react';

export const SystemHealthWidget: React.FC = () => {
  const [health, setHealth] = useState<SystemHealthResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadHealth();
    const interval = setInterval(loadHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadHealth = async () => {
    try {
      const res = await systemHealthApi.getSystemHealth();
      setHealth(res);
    } catch (err) {
      console.error('Failed to load system health', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !health) return null;

  const gw = health.gateway || health.gatewaySummary;
  const totalRequests = gw ? (gw.totalRequests ?? gw.totalRequestsHandled ?? 0) : 0;
  const p95Latency = gw ? (gw.p95LatencyMs ?? 0) : 0;
  const mysqlStatus = health.mysql?.status || 'UNKNOWN';
  const mysqlLatency = health.mysql?.latencyMs ?? 0;
  const redisStatus = health.redis?.status || 'UNKNOWN';
  const redisLatency = health.redis?.latencyMs ?? 0;

  return (
    <div className="card" style={{ marginBottom: '1.5rem' }}>
      <div className="card-header" style={{ padding: '1rem 1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Activity style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
          <div>
            <h3 className="card-title" style={{ fontSize: '1rem' }}>Sentinel Infrastructure & Platform Health</h3>
            <p className="card-subtitle" style={{ fontSize: '0.75rem' }}>Real-time cluster status & subsystem round-trip latencies</p>
          </div>
        </div>
        <span className="badge badge-healthy" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
          Control Plane: {health.controlPlaneStatus || 'UP'}
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', padding: '1.25rem' }}>
        {/* MySQL */}
        <div className="p-3.5 bg-slate-950/60 border border-slate-800/80 rounded-xl flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-sky-500/10 rounded-lg text-sky-400">
              <Database className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-semibold text-white">MySQL 8.4</div>
              <div className="text-[11px] text-slate-400">Persistence Store</div>
            </div>
          </div>
          <div className="text-right">
            <span className={`text-xs font-bold ${mysqlStatus === 'UP' ? 'text-emerald-400' : 'text-rose-400'}`}>
              ● {mysqlStatus}
            </span>
            <div className="text-[10px] font-mono text-slate-400">{mysqlLatency}ms ping</div>
          </div>
        </div>

        {/* Redis */}
        <div className="p-3.5 bg-slate-950/60 border border-slate-800/80 rounded-xl flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-rose-500/10 rounded-lg text-rose-400">
              <Zap className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-semibold text-white">Redis 7.x</div>
              <div className="text-[11px] text-slate-400">Rate Limiter / Quota</div>
            </div>
          </div>
          <div className="text-right">
            <span className={`text-xs font-bold ${redisStatus === 'UP' ? 'text-emerald-400' : 'text-rose-400'}`}>
              ● {redisStatus}
            </span>
            <div className="text-[10px] font-mono text-slate-400">{redisLatency}ms ping</div>
          </div>
        </div>

        {/* Gateway Summary */}
        <div className="p-3.5 bg-slate-950/60 border border-slate-800/80 rounded-xl flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-indigo-500/10 rounded-lg text-indigo-400">
              <Server className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-semibold text-white">HTTP Gateway</div>
              <div className="text-[11px] text-slate-400">Throughput & P95</div>
            </div>
          </div>
          <div className="text-right">
            <span className="text-xs font-bold text-indigo-400 font-mono">
              {totalRequests.toLocaleString()} reqs
            </span>
            <div className="text-[10px] font-mono text-slate-400">P95: {p95Latency}ms</div>
          </div>
        </div>
      </div>
    </div>
  );
};
