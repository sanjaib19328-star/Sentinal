import React, { useState, useEffect } from 'react';
import { circuitBreakerApi } from '../../api/circuitBreaker';
import { CircuitBreakerStatus } from '../../types/circuitBreaker';

interface CircuitBreakerWidgetProps {
  applicationId: number;
}

export const CircuitBreakerWidget: React.FC<CircuitBreakerWidgetProps> = ({ applicationId }) => {
  const [status, setStatus] = useState<CircuitBreakerStatus | null>(null);

  useEffect(() => {
    loadStatus();
    const interval = setInterval(loadStatus, 5000);
    return () => clearInterval(interval);
  }, [applicationId]);

  const loadStatus = async () => {
    try {
      const res = await circuitBreakerApi.getStatus(applicationId);
      setStatus(res);
    } catch (err) {
      console.error('Failed to load circuit breaker status', err);
    }
  };

  if (!status) return null;

  const getStateColor = (state: string) => {
    switch (state) {
      case 'CLOSED':
        return {
          bg: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
          dot: 'bg-emerald-400',
          title: 'CLOSED (Healthy Traffic Allowed)',
        };
      case 'OPEN':
        return {
          bg: 'bg-rose-500/10 border-rose-500/20 text-rose-400',
          dot: 'bg-rose-400 animate-ping',
          title: 'OPEN (Fast Failing Requests - Downstream Down)',
        };
      case 'HALF_OPEN':
        return {
          bg: 'bg-amber-500/10 border-amber-500/20 text-amber-400',
          dot: 'bg-amber-400',
          title: 'HALF_OPEN (Trial Probing Active)',
        };
      default:
        return {
          bg: 'bg-slate-500/10 border-slate-500/20 text-slate-400',
          dot: 'bg-slate-400',
          title: 'UNKNOWN',
        };
    }
  };

  const style = getStateColor(status.state);

  return (
    <div className={`p-4 rounded-xl border ${style.bg} flex flex-col md:flex-row items-start md:items-center justify-between gap-3`}>
      <div className="flex items-center space-x-3">
        <div className="relative flex h-3 w-3">
          <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${style.dot}`}></span>
          <span className={`relative inline-flex rounded-full h-3 w-3 ${style.dot}`}></span>
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <span className="font-bold text-sm text-white">Circuit Breaker:</span>
            <span className="font-semibold text-xs uppercase tracking-wide">{status.state}</span>
            <span className="text-xs text-slate-400">({status.enabled ? 'Enabled' : 'Disabled'})</span>
          </div>
          <p className="text-xs text-slate-300 mt-0.5">{style.title}</p>
        </div>
      </div>

      <div className="flex items-center space-x-4 text-xs">
        <div>
          <span className="text-slate-400">Consecutive Failures: </span>
          <strong className="text-white font-mono">{status.consecutiveFailures} / {status.failureThreshold}</strong>
        </div>
        {status.state === 'OPEN' && (
          <div>
            <span className="text-slate-400">Recovery in: </span>
            <strong className="text-rose-400 font-mono">{status.timeUntilRecoverySeconds}s</strong>
          </div>
        )}
      </div>
    </div>
  );
};
