export type MessageSender = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface ConversationMessage {
  id: number;
  conversationId: number;
  sender: MessageSender;
  content: string;
  metadataJson?: string | null;
  createdAt: string;
}

export interface Conversation {
  id: number;
  userId: number;
  applicationId?: number | null;
  applicationName?: string | null;
  title: string;
  metadataJson?: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  lastMessagePreview?: string | null;
}

export interface ConversationDetail {
  id: number;
  userId: number;
  applicationId?: number | null;
  applicationName?: string | null;
  applicationBaseUrl?: string | null;
  title: string;
  metadataJson?: string | null;
  createdAt: string;
  updatedAt: string;
  messages: ConversationMessage[];
}

export interface CreateConversationRequest {
  applicationId?: number | null;
  title?: string;
  initialPrompt?: string;
  metadataJson?: string;
}

export interface UpdateConversationRequest {
  title?: string;
  metadataJson?: string;
}

export interface SendMessageRequest {
  content: string;
  apiKeyId?: number | null;
  metadataJson?: string;
  triggerAiTesting?: boolean;
  fileBase64?: string;
  fileName?: string;
  fileContentType?: string;
}

export interface AiTestMissingInput {
  inputKey: string;
  inputType: 'FILE' | 'API_KEY' | 'VARIABLE' | 'CONFIRMATION' | 'HEADER';
  targetEndpoint: string;
  targetMethod: string;
  prompt: string;
  received?: boolean;
  valuePreview?: string;
}

export interface AiTestStep {
  stepId: string;
  name: string;
  method: string;
  path: string;
  description?: string;
  destructive: boolean;
  requiresApproval: boolean;
  dependsOnStepIds: string[];
  requiredVariables?: string[];
  producedVariables?: string[];
  extractedVariables: Record<string, string>;
  parameterMappings: Record<string, string>;
  requestBodyTemplate?: string;
  requestContentType?: string;
  multipart: boolean;
  multipartFieldName?: string;
  requiresInput?: boolean;
  missingInputType?: string;
  missingInputPrompt?: string;
  level?: number;
}

export interface AiTestPlan {
  planId: string;
  applicationId: number;
  applicationName: string;
  title: string;
  summary: string;
  totalEndpointsDiscovered: number;
  totalStepsPlanned: number;
  status: 'READY' | 'WAITING_FOR_INPUT' | 'REQUIRES_CONFIRMATION';
  missingInputs?: AiTestMissingInput[];
  steps: AiTestStep[];
}

export interface AiTestStepResult {
  stepId: string;
  name: string;
  method: string;
  endpoint: string;
  resolvedPath?: string;
  status: number;
  latencyMs: number;
  requestId?: string;
  passed: boolean;
  skipped: boolean;
  blocked: boolean;
  requiresApproval: boolean;
  executionStatus?: 'PASSED' | 'FAILED' | 'BLOCKED' | 'SKIPPED_DUE_TO_DEPENDENCY' | 'REQUIRES_CONFIRMATION';
  blockedReason?: string;
  error?: string;
  responseSummary?: string;
  inputsUsed: Record<string, string>;
  outputsExtracted: Record<string, string>;
}

export interface AiTestRunReport {
  runId: string;
  applicationId: number;
  applicationName: string;
  totalSteps: number;
  passedSteps: number;
  failedSteps: number;
  blockedSteps: number;
  pendingApprovalSteps: number;
  totalDurationMs: number;
  avgLatencyMs: number;
  overallStatus: 'PASSED' | 'FAILED' | 'PARTIAL' | 'NEEDS_APPROVAL';
  executiveSummary: string;
  failureAnalysis?: string;
  rememberedContext: Record<string, string>;
  stepResults: AiTestStepResult[];
}

export interface AiTestSession {
  sessionId: string;
  applicationId: number;
  applicationName: string;
  status: 'PLANNING' | 'WAITING_FOR_INPUT' | 'READY' | 'RUNNING' | 'PASSED' | 'FAILED' | 'PARTIAL' | 'NEEDS_APPROVAL' | 'CANCELLED';
  statusMessage: string;
  plan?: AiTestPlan;
  missingInputs: AiTestMissingInput[];
  providedInputs: Record<string, string>;
  fileBase64?: string;
  fileName?: string;
  fileContentType?: string;
  apiKeyId?: number;
  approveDestructiveOperations?: boolean;
  lastReport?: AiTestRunReport;
  createdAt: number;
  updatedAt: number;
}

export interface RunAiTestRequest {
  applicationId?: number;
  apiKeyId?: number;
  approveDestructiveOperations?: boolean;
  initialContext?: Record<string, string>;
  fileBase64?: string;
  fileName?: string;
  fileContentType?: string;
  focusPrompt?: string;
}
