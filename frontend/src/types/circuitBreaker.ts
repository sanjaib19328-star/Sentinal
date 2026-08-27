export type CircuitState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

export interface CircuitBreakerStatus {
  applicationId: number;
  state: CircuitState;
  consecutiveFailures: number;
  failureThreshold: number;
  recoveryTimeoutSeconds: number;
  timeUntilRecoverySeconds: number;
  enabled: boolean;
}
