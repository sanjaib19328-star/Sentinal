export type AuditAction =
  | 'APPLICATION_CREATED'
  | 'APPLICATION_UPDATED'
  | 'APPLICATION_DEACTIVATED'
  | 'APPLICATION_DELETED'
  | 'API_KEY_CREATED'
  | 'API_KEY_UPDATED'
  | 'API_KEY_REVOKED'
  | 'API_KEY_REGENERATED'
  | 'API_KEY_DELETED'
  | 'POLICY_CREATED'
  | 'POLICY_UPDATED'
  | 'POLICY_DELETED'
  | 'ALERT_RULE_CREATED'
  | 'ALERT_RULE_UPDATED'
  | 'ALERT_RULE_DELETED';

export interface AuditLog {
  id: number;
  ownerId: number;
  applicationId: number | null;
  actorEmail: string;
  action: AuditAction;
  targetType: string;
  targetId: string | null;
  description: string;
  metadata: string | null;
  ipAddress: string | null;
  createdAt: string;
}
