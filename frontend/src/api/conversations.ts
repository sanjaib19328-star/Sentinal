import { apiClient } from './client';
import {
  Conversation,
  ConversationDetail,
  ConversationMessage,
  CreateConversationRequest,
  UpdateConversationRequest,
  SendMessageRequest,
  AiTestPlan,
  AiTestRunReport,
  RunAiTestRequest,
} from '../types/conversation';

export const conversationsApi = {
  list: async (params?: { applicationId?: number; search?: string }): Promise<Conversation[]> => {
    const res = await apiClient.get<Conversation[]>('/api/v1/conversations', { params });
    return res.data;
  },

  getById: async (id: number): Promise<ConversationDetail> => {
    const res = await apiClient.get<ConversationDetail>(`/api/v1/conversations/${id}`);
    return res.data;
  },

  create: async (data: CreateConversationRequest): Promise<ConversationDetail> => {
    const res = await apiClient.post<ConversationDetail>('/api/v1/conversations', data);
    return res.data;
  },

  update: async (id: number, data: UpdateConversationRequest): Promise<Conversation> => {
    const res = await apiClient.patch<Conversation>(`/api/v1/conversations/${id}`, data);
    return res.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/v1/conversations/${id}`);
  },

  sendMessage: async (id: number, data: SendMessageRequest): Promise<ConversationDetail> => {
    const res = await apiClient.post<ConversationDetail>(`/api/v1/conversations/${id}/messages`, data);
    return res.data;
  },

  getMessages: async (id: number): Promise<ConversationMessage[]> => {
    const res = await apiClient.get<ConversationMessage[]>(`/api/v1/conversations/${id}/messages`);
    return res.data;
  },

  runAiTestForConversation: async (id: number, data: RunAiTestRequest): Promise<AiTestRunReport> => {
    const res = await apiClient.post<AiTestRunReport>(`/api/v1/conversations/${id}/run-ai-test`, data);
    return res.data;
  },

  getAiTestPlan: async (applicationId: number): Promise<AiTestPlan> => {
    const res = await apiClient.get<AiTestPlan>(`/api/v1/applications/${applicationId}/ai-test-plan`);
    return res.data;
  },

  runDirectAiTest: async (applicationId: number, data: RunAiTestRequest): Promise<AiTestRunReport> => {
    const res = await apiClient.post<AiTestRunReport>(`/api/v1/applications/${applicationId}/run-ai-test`, data);
    return res.data;
  },
};
