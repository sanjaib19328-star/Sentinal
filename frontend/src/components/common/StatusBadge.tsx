import React from 'react';
import { HealthStatus } from '../../types/application';

interface StatusBadgeProps {
  status: HealthStatus;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const getBadgeClass = (s: HealthStatus) => {
    switch (s) {
      case 'HEALTHY':
        return 'badge-healthy';
      case 'DEGRADED':
        return 'badge-degraded';
      case 'UNAVAILABLE':
        return 'badge-unavailable';
      case 'UNKNOWN':
      default:
        return 'badge-unknown';
    }
  };

  const getDotClass = (s: HealthStatus) => {
    switch (s) {
      case 'HEALTHY':
        return 'status-dot-healthy';
      case 'DEGRADED':
        return 'status-dot-degraded';
      case 'UNAVAILABLE':
        return 'status-dot-unavailable';
      case 'UNKNOWN':
      default:
        return 'status-dot-unknown';
    }
  };

  const style = size === 'sm' ? { fontSize: '0.6875rem', padding: '0.15rem 0.5rem' } : undefined;

  return (
    <span className={`badge ${getBadgeClass(status)}`} style={style}>
      <span className={`status-dot ${getDotClass(status)}`} />
      {status}
    </span>
  );
};
