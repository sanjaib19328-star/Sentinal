import React, { useState, useEffect } from 'react';
import { consumersApi } from '../../api/consumers';
import { ConsumerKeyAnalytics } from '../../types/consumer';

interface ConsumerAnalyticsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  applicationId: number;
  keyId: number | null;
  keyName: string;
}

export const ConsumerAnalyticsDrawer: React.FC<ConsumerAnalyticsDrawerProps> = ({
  isOpen,
  onClose,
  applicationId,
  keyId,
  keyName,
}) => {
  const [data, setData] = useState<ConsumerKeyAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && keyId) {
      loadKeyAnalytics();
    }
  }, [isOpen, applicationId, keyId]);

  const loadKeyAnalytics = async () => {
    if (!keyId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await consumersApi.getKeyAnalytics(applicationId, keyId);
      setData(res);
    } catch (err: any) {
      setError(err.message || 'Failed to load key consumer analytics');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-xl bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col">
          
          {/* Header */}
          <div className="px-6 py-5 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-indigo-500/10 border border-indigo-500/20 rounded-lg text-indigo-400">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 8v8m-4-5v5m-4-2v2m-2 4h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Consumer Key Analytics</h3>
                <p className="text-xs text-slate-400">{keyName} (Key #{keyId})</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {loading ? (
              <div className="flex items-center justify-center h-48">
                <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : error ? (
              <div className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-sm">
                {error}
              </div>
            ) : data ? (
              <>
                {/* Metric Cards Grid */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400">Total Requests</span>
                    <p className="text-2xl font-bold text-white mt-1">{data.totalRequests.toLocaleString()}</p>
                  </div>
                  <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400">Error Rate</span>
                    <p className={`text-2xl font-bold mt-1 ${data.errorRate > 5 ? 'text-rose-400' : 'text-emerald-400'}`}>
                      {data.errorRate}%
                    </p>
                  </div>
                  <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400">Avg Latency</span>
                    <p className="text-2xl font-bold text-indigo-400 mt-1 font-mono">{data.avgLatencyMs}ms</p>
                  </div>
                  <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400">Rate Limited (429)</span>
                    <p className="text-2xl font-bold text-amber-400 mt-1">{data.count429.toLocaleString()}</p>
                  </div>
                </div>

                {/* Status Code Distribution */}
                <div className="bg-slate-950 p-5 rounded-xl border border-slate-800 space-y-3">
                  <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">Status Code Breakdown</h4>
                  <div className="grid grid-cols-4 gap-2 text-center text-xs">
                    <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-lg">
                      <div className="text-emerald-400 font-bold">{data.successRequests}</div>
                      <div className="text-slate-400 text-[10px] mt-0.5">2xx Success</div>
                    </div>
                    <div className="p-2.5 bg-amber-500/10 border border-amber-500/20 rounded-lg">
                      <div className="text-amber-400 font-bold">{data.count4xx}</div>
                      <div className="text-slate-400 text-[10px] mt-0.5">4xx Client</div>
                    </div>
                    <div className="p-2.5 bg-rose-500/10 border border-rose-500/20 rounded-lg">
                      <div className="text-rose-400 font-bold">{data.count5xx}</div>
                      <div className="text-slate-400 text-[10px] mt-0.5">5xx Server</div>
                    </div>
                    <div className="p-2.5 bg-indigo-500/10 border border-indigo-500/20 rounded-lg">
                      <div className="text-indigo-400 font-bold">{data.count429}</div>
                      <div className="text-slate-400 text-[10px] mt-0.5">429 Throttled</div>
                    </div>
                  </div>
                </div>

                {/* Latency Percentiles */}
                <div className="bg-slate-950 p-5 rounded-xl border border-slate-800 space-y-3">
                  <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">Latency Percentiles</h4>
                  <div className="grid grid-cols-3 gap-3 text-center">
                    <div className="bg-slate-900/80 p-3 rounded-lg border border-slate-800/80">
                      <div className="text-slate-400 text-xs">P50</div>
                      <div className="text-lg font-bold text-white font-mono mt-1">{data.p50LatencyMs}ms</div>
                    </div>
                    <div className="bg-slate-900/80 p-3 rounded-lg border border-slate-800/80">
                      <div className="text-slate-400 text-xs">P95</div>
                      <div className="text-lg font-bold text-indigo-400 font-mono mt-1">{data.p95LatencyMs}ms</div>
                    </div>
                    <div className="bg-slate-900/80 p-3 rounded-lg border border-slate-800/80">
                      <div className="text-slate-400 text-xs">P99</div>
                      <div className="text-lg font-bold text-amber-400 font-mono mt-1">{data.p99LatencyMs}ms</div>
                    </div>
                  </div>
                </div>

                {/* Top Endpoints */}
                <div className="bg-slate-950 p-5 rounded-xl border border-slate-800 space-y-3">
                  <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">Most Active Endpoints</h4>
                  {data.topEndpoints.length === 0 ? (
                    <p className="text-xs text-slate-500">No endpoint activity recorded for this key.</p>
                  ) : (
                    <div className="space-y-2">
                      {data.topEndpoints.map((ep, idx) => (
                        <div key={idx} className="flex items-center justify-between p-2.5 bg-slate-900/60 rounded-lg border border-slate-800 text-xs">
                          <div className="flex items-center space-x-2">
                            <span className="font-semibold px-2 py-0.5 text-[10px] rounded bg-indigo-500/20 text-indigo-400">
                              {ep.method}
                            </span>
                            <span className="font-mono text-slate-200">{ep.normalizedPath}</span>
                          </div>
                          <div className="flex items-center space-x-3 text-slate-400 font-mono">
                            <span>{ep.requestCount} reqs</span>
                            <span>{ep.avgLatencyMs}ms avg</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
};
