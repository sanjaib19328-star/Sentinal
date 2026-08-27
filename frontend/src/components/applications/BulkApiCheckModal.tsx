import React, { useState, useEffect, useRef } from 'react';
import { Modal } from '../common/Modal';
import { applicationsApi } from '../../api/applications';
import { BulkApiEndpointResult } from '../../types/application';
import { getErrorMessage } from '../../api/client';
import {
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  RefreshCw,
  Play,
  Pause,
} from 'lucide-react';

interface BulkApiCheckModalProps {
  isOpen: boolean;
  onClose: () => void;
  applicationId: number;
  applicationName?: string;
  totalEndpointsCount?: number;
}

export const BulkApiCheckModal: React.FC<BulkApiCheckModalProps> = ({
  isOpen,
  onClose,
  applicationId,
  applicationName = 'Application',
  totalEndpointsCount = 0,
}) => {
  const [running, setRunning] = useState(false);
  const [paused, setPaused] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [currentBatch, setCurrentBatch] = useState(0);
  const [totalBatches, setTotalBatches] = useState(1);
  const [totalEndpoints, setTotalEndpoints] = useState(totalEndpointsCount);
  const [results, setResults] = useState<BulkApiEndpointResult[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'VALID' | 'WARNING' | 'ERROR'>('ALL');

  const isCancelledRef = useRef(false);
  const isPausedRef = useRef(false);

  useEffect(() => {
    if (isOpen) {
      setRunning(false);
      setPaused(false);
      setCompleted(false);
      setCurrentBatch(0);
      setTotalBatches(Math.max(1, Math.ceil((totalEndpointsCount || 1) / 20)));
      setTotalEndpoints(totalEndpointsCount);
      setResults([]);
      setError(null);
      isCancelledRef.current = false;
      isPausedRef.current = false;
    }
  }, [isOpen, totalEndpointsCount]);

  const runBatchLoop = async (startIndex: number) => {
    setRunning(true);
    setPaused(false);
    setError(null);
    isCancelledRef.current = false;
    isPausedRef.current = false;

    let batchIdx = startIndex;
    let accumulatedResults = [...results];

    while (!isCancelledRef.current && !isPausedRef.current) {
      try {
        const resp = await applicationsApi.bulkApiCheck(applicationId, {
          applicationId,
          batchIndex: batchIdx,
          batchSize: 20,
        });

        if (isCancelledRef.current) break;

        setTotalBatches(resp.totalBatches);
        setTotalEndpoints(resp.totalEndpoints);
        setCurrentBatch(batchIdx + 1);

        // Deduplicate and append batch results
        const existingIds = new Set(accumulatedResults.map((r) => r.endpointId));
        const newResults = resp.results.filter((r) => !existingIds.has(r.endpointId));
        accumulatedResults = [...accumulatedResults, ...newResults];
        setResults(accumulatedResults);

        if (resp.lastBatch || batchIdx >= resp.totalBatches - 1) {
          setCompleted(true);
          setRunning(false);
          break;
        }

        batchIdx++;
        // Small yielding delay to keep UI responsive
        await new Promise((r) => setTimeout(r, 200));
      } catch (err: any) {
        setError(getErrorMessage(err) || 'Failed during bulk API analysis');
        setRunning(false);
        break;
      }
    }
  };

  const handleStart = () => {
    setResults([]);
    runBatchLoop(0);
  };

  const handlePause = () => {
    isPausedRef.current = true;
    setPaused(true);
    setRunning(false);
  };

  const handleResume = () => {
    runBatchLoop(currentBatch);
  };

  const handleModalClose = () => {
    isCancelledRef.current = true;
    onClose();
  };

  const validCount = results.filter((r) => r.status === 'VALID').length;
  const warningCount = results.filter((r) => r.status === 'WARNING' || r.status === 'REQUIRES_INPUT').length;
  const errorCount = results.filter((r) => r.status === 'ERROR').length;

  const filteredResults = results.filter((r) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'VALID') return r.status === 'VALID';
    if (statusFilter === 'WARNING') return r.status === 'WARNING' || r.status === 'REQUIRES_INPUT';
    if (statusFilter === 'ERROR') return r.status === 'ERROR';
    return true;
  });

  const progressPercent = totalEndpoints > 0 ? Math.min(100, Math.round((results.length / totalEndpoints) * 100)) : 0;

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleModalClose}
      title={`AI Bulk API Validation — ${applicationName}`}
      maxWidth="860px"
    >
      <div>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: '0 0 1rem' }}>
          Efficiently validate large API collections (up to 1,000+ endpoints) in controlled, safe batches with automated diagnostics.
        </p>

        {error && (
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--danger-light)',
              color: 'var(--danger-text)',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.8125rem',
              marginBottom: '1rem',
            }}
          >
            {error}
          </div>
        )}

        {/* Progress & Metrics Bar */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: '0.75rem',
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-color)',
            marginBottom: '1rem',
            textAlign: 'center',
          }}
        >
          <div>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Analyzed</div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {results.length} / {totalEndpoints || totalEndpointsCount}
            </div>
          </div>
          <div>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Valid</div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--success)' }}>
              {validCount} ✓
            </div>
          </div>
          <div>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Warnings / Input Required</div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--warning)' }}>
              {warningCount} ⚠️
            </div>
          </div>
          <div>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Errors</div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--danger)' }}>
              {errorCount} ✕
            </div>
          </div>
        </div>

        {/* Visual Progress Bar */}
        {(running || paused || completed) && (
          <div style={{ marginBottom: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '0.25rem', color: 'var(--text-secondary)' }}>
              <span>
                {running ? `Analyzing Batch ${currentBatch} of ${totalBatches}...` : paused ? 'Paused' : 'Analysis Complete'}
              </span>
              <span>{progressPercent}%</span>
            </div>
            <div
              style={{
                width: '100%',
                height: '6px',
                backgroundColor: 'var(--border-color)',
                borderRadius: '3px',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${progressPercent}%`,
                  height: '100%',
                  backgroundColor: completed ? 'var(--success)' : 'var(--primary)',
                  transition: 'width 0.3s ease',
                }}
              />
            </div>
          </div>
        )}

        {/* Action Controls & Filter */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {!running && !paused && !completed && (
              <button onClick={handleStart} className="btn btn-primary btn-sm flex items-center gap-1.5">
                <Sparkles style={{ width: '0.875rem', height: '0.875rem' }} />
                Start Bulk Check
              </button>
            )}
            {running && (
              <button onClick={handlePause} className="btn btn-secondary btn-sm flex items-center gap-1.5">
                <Pause style={{ width: '0.875rem', height: '0.875rem' }} />
                Pause
              </button>
            )}
            {paused && (
              <button onClick={handleResume} className="btn btn-primary btn-sm flex items-center gap-1.5">
                <Play style={{ width: '0.875rem', height: '0.875rem' }} />
                Resume
              </button>
            )}
            {completed && (
              <button onClick={handleStart} className="btn btn-secondary btn-sm flex items-center gap-1.5">
                <RefreshCw style={{ width: '0.875rem', height: '0.875rem' }} />
                Re-Run Check
              </button>
            )}
          </div>

          {/* Filter Pills */}
          {results.length > 0 && (
            <div style={{ display: 'flex', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
              {(['ALL', 'VALID', 'WARNING', 'ERROR'] as const).map((filter) => (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setStatusFilter(filter)}
                  style={{
                    padding: '0.25rem 0.625rem',
                    fontSize: '0.6875rem',
                    fontWeight: statusFilter === filter ? 700 : 500,
                    backgroundColor: statusFilter === filter ? 'var(--primary)' : 'var(--bg-surface)',
                    color: statusFilter === filter ? '#fff' : 'var(--text-secondary)',
                    border: 'none',
                    cursor: 'pointer',
                    borderRight: '1px solid var(--border-color)',
                  }}
                >
                  {filter}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Results Table */}
        {results.length > 0 ? (
          <div
            style={{
              maxHeight: '320px',
              overflowY: 'auto',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--bg-card)',
            }}
          >
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.75rem' }}>
              <thead>
                <tr style={{ backgroundColor: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                  <th style={{ padding: '0.5rem 0.75rem', width: '70px' }}>Method</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Endpoint</th>
                  <th style={{ padding: '0.5rem 0.75rem', width: '110px' }}>Status</th>
                  <th style={{ padding: '0.5rem 0.75rem', width: '80px' }}>Latency</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Diagnostics / Recommendation</th>
                </tr>
              </thead>
              <tbody>
                {filteredResults.map((r, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <span
                        style={{
                          padding: '0.125rem 0.375rem',
                          borderRadius: 'var(--radius-sm)',
                          fontWeight: 700,
                          fontSize: '0.6875rem',
                          backgroundColor: r.method === 'GET' ? 'var(--success-light)' : r.method === 'POST' ? 'var(--primary-light)' : 'var(--warning-light)',
                          color: r.method === 'GET' ? 'var(--success-text)' : r.method === 'POST' ? 'var(--primary)' : 'var(--warning-text)',
                        }}
                      >
                        {r.method}
                      </span>
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>
                      {r.path}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                          padding: '0.125rem 0.375rem',
                          borderRadius: 'var(--radius-sm)',
                          fontSize: '0.6875rem',
                          fontWeight: 600,
                          backgroundColor:
                            r.status === 'VALID'
                              ? 'var(--success-light)'
                              : r.status === 'ERROR'
                              ? 'var(--danger-light)'
                              : 'var(--warning-light)',
                          color:
                            r.status === 'VALID'
                              ? 'var(--success-text)'
                              : r.status === 'ERROR'
                              ? 'var(--danger-text)'
                              : 'var(--warning-text)',
                        }}
                      >
                        {r.status === 'VALID' && <CheckCircle2 style={{ width: '0.6875rem', height: '0.6875rem' }} />}
                        {r.status === 'WARNING' && <AlertTriangle style={{ width: '0.6875rem', height: '0.6875rem' }} />}
                        {r.status === 'REQUIRES_INPUT' && <AlertTriangle style={{ width: '0.6875rem', height: '0.6875rem' }} />}
                        {r.status === 'ERROR' && <XCircle style={{ width: '0.6875rem', height: '0.6875rem' }} />}
                        {r.status === 'REQUIRES_INPUT' ? 'INPUT REQ' : r.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', color: 'var(--text-muted)' }}>
                      {r.latencyMs != null ? `${r.latencyMs}ms` : '—'}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{r.responseValidity || 'OK'}</div>
                      {r.detectedProblems && (
                        <div style={{ color: 'var(--danger)', fontSize: '0.6875rem', marginTop: '0.125rem' }}>
                          {r.detectedProblems}
                        </div>
                      )}
                      {r.recommendation && (
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.6875rem', marginTop: '0.125rem' }}>
                          💡 {r.recommendation}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div
            style={{
              padding: '2.5rem 1rem',
              textAlign: 'center',
              backgroundColor: 'var(--bg-surface)',
              border: '1px dashed var(--border-color)',
              borderRadius: 'var(--radius-md)',
              color: 'var(--text-muted)',
              fontSize: '0.8125rem',
            }}
          >
            Click <strong>Start Bulk Check</strong> to analyze endpoints in safe, controlled chunks.
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
          <button type="button" onClick={handleModalClose} className="btn btn-secondary btn-sm">
            Close
          </button>
        </div>
      </div>
    </Modal>
  );
};
