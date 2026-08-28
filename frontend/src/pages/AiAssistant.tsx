import React, { useState, useEffect, useRef } from 'react';
import {
  Bot,
  Plus,
  Search,
  Send,
  Trash2,
  Edit2,
  Check,
  X,
  UploadCloud,
  FileCode,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Sparkles,
  Zap,
} from 'lucide-react';
import { conversationsApi } from '../api/conversations';
import { applicationsApi } from '../api/applications';
import { apiKeysApi } from '../api/apiKeys';
import { Conversation, ConversationDetail, AiTestRunReport } from '../types/conversation';
import { Application } from '../types/application';
import { ApiKey } from '../types/apiKey';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { MarkdownViewer } from '../components/common/MarkdownViewer';

export const AiAssistant: React.FC = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [activeConversation, setActiveConversation] = useState<ConversationDetail | null>(null);
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedAppId, setSelectedAppId] = useState<number | null>(null);
  const [, setApiKeys] = useState<ApiKey[]>([]);
  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [prompt, setPrompt] = useState('');
  const [loadingList, setLoadingList] = useState(false);
  const [loadingChat, setLoadingChat] = useState(false);
  const [sending, setSending] = useState(false);
  const [runningTest, setRunningTest] = useState(false);

  // Rename state
  const [editingChatId, setEditingChatId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState('');

  // Delete state
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // File upload state in chat
  const [attachedFile, setAttachedFile] = useState<{
    base64: string;
    name: string;
    size: number;
    type: string;
  } | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadApplications();
    loadConversations();
  }, []);

  useEffect(() => {
    if (activeConversationId) {
      loadActiveConversation(activeConversationId);
    }
  }, [activeConversationId]);

  useEffect(() => {
    if (selectedAppId) {
      loadApiKeys(selectedAppId);
    } else {
      setApiKeys([]);
      setSelectedKeyId(null);
    }
  }, [selectedAppId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConversation?.messages, sending, runningTest]);

  const loadApplications = async () => {
    try {
      const res = await applicationsApi.list();
      setApplications(res);
      if (res.length > 0 && !selectedAppId) {
        setSelectedAppId(res[0].id);
      }
    } catch (e) {
      console.error('Failed to load applications', e);
    }
  };

  const loadApiKeys = async (appId: number) => {
    try {
      const keys = await apiKeysApi.list(appId);
      const active = keys.filter((k) => k.active);
      setApiKeys(active);
      if (active.length > 0) {
        setSelectedKeyId(active[0].id);
      } else {
        setSelectedKeyId(null);
      }
    } catch (e) {
      console.error('Failed to load API keys', e);
    }
  };

  const loadConversations = async (search?: string) => {
    setLoadingList(true);
    try {
      const list = await conversationsApi.list({ search });
      setConversations(list);
      if (list.length > 0 && !activeConversationId) {
        setActiveConversationId(list[0].id);
      }
    } catch (e) {
      console.error('Failed to load conversations', e);
    } finally {
      setLoadingList(false);
    }
  };

  const loadActiveConversation = async (id: number) => {
    setLoadingChat(true);
    try {
      const detail = await conversationsApi.getById(id);
      setActiveConversation(detail);
      if (detail.applicationId) {
        setSelectedAppId(detail.applicationId);
      }
    } catch (e) {
      console.error('Failed to load conversation details', e);
    } finally {
      setLoadingChat(false);
    }
  };

  const handleCreateNewChat = async () => {
    try {
      const created = await conversationsApi.create({
        applicationId: selectedAppId,
        title: 'New Chat',
      });
      setConversations((prev) => [
        {
          id: created.id,
          userId: created.userId,
          applicationId: created.applicationId,
          applicationName: created.applicationName,
          title: created.title,
          createdAt: created.createdAt,
          updatedAt: created.updatedAt,
          messageCount: 0,
        },
        ...prev,
      ]);
      setActiveConversationId(created.id);
      setActiveConversation(created);
    } catch (e) {
      console.error('Failed to create conversation', e);
    }
  };

  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if ((!prompt.trim() && !attachedFile) || !activeConversationId || sending) return;

    const userText = prompt.trim() || `Uploaded file: ${attachedFile?.name}`;
    setSending(true);
    setPrompt('');

    try {
      const updated = await conversationsApi.sendMessage(activeConversationId, {
        content: userText,
        apiKeyId: selectedKeyId,
        fileBase64: attachedFile?.base64,
        fileName: attachedFile?.name,
        fileContentType: attachedFile?.type,
      });

      setActiveConversation(updated);
      setAttachedFile(null);
      loadConversations();
    } catch (e) {
      console.error('Failed to send message', e);
    } finally {
      setSending(false);
    }
  };

  const handleRunAiTestSuite = async () => {
    if (!activeConversationId || !selectedAppId || runningTest) return;

    setRunningTest(true);
    try {
      await conversationsApi.runAiTestForConversation(activeConversationId, {
        applicationId: selectedAppId,
        apiKeyId: selectedKeyId || undefined,
        fileBase64: attachedFile?.base64,
        fileName: attachedFile?.name,
        fileContentType: attachedFile?.type,
      });

      await loadActiveConversation(activeConversationId);
      setAttachedFile(null);
      loadConversations();
    } catch (e) {
      console.error('Failed to run AI test suite', e);
    } finally {
      setRunningTest(false);
    }
  };

  const handleRename = async (id: number) => {
    if (!editTitle.trim()) {
      setEditingChatId(null);
      return;
    }
    try {
      const updated = await conversationsApi.update(id, { title: editTitle.trim() });
      setConversations((prev) =>
        prev.map((c) => (c.id === id ? { ...c, title: updated.title } : c))
      );
      if (activeConversation?.id === id) {
        setActiveConversation((prev) => (prev ? { ...prev, title: updated.title } : null));
      }
    } catch (e) {
      console.error('Failed to rename conversation', e);
    } finally {
      setEditingChatId(null);
      setEditTitle('');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await conversationsApi.delete(id);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      if (activeConversationId === id) {
        const remaining = conversations.filter((c) => c.id !== id);
        if (remaining.length > 0) {
          setActiveConversationId(remaining[0].id);
        } else {
          setActiveConversationId(null);
          setActiveConversation(null);
        }
      }
    } catch (e) {
      console.error('Failed to delete conversation', e);
    } finally {
      setDeleteConfirmId(null);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      const base64 = result.split(',')[1] || result;
      setAttachedFile({
        base64,
        name: file.name,
        size: file.size,
        type: file.type || 'application/octet-stream',
      });
    };
    reader.readAsDataURL(file);
  };

  const renderStructuredReport = (metadataJson?: string | null) => {
    if (!metadataJson) return null;
    try {
      const report: AiTestRunReport = JSON.parse(metadataJson);
      if (!report.stepResults || report.stepResults.length === 0) return null;

      return (
        <div className="ai-test-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Sparkles style={{ width: '1.125rem', height: '1.125rem', color: 'var(--primary)' }} />
              <span style={{ fontWeight: 700, fontSize: '0.875rem' }}>Structured Test Execution Report</span>
            </div>
            <span
              className={`pill-badge ${
                report.overallStatus === 'PASSED'
                  ? 'pill-badge-green'
                  : report.overallStatus === 'NEEDS_APPROVAL'
                  ? 'pill-badge-amber'
                  : 'pill-badge-blue'
              }`}
            >
              {report.overallStatus} ({report.passedSteps}/{report.totalSteps} Passed)
            </span>
          </div>

          <table className="ai-step-table">
            <thead>
              <tr>
                <th>Method</th>
                <th>Endpoint</th>
                <th>Status</th>
                <th>Latency</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {report.stepResults.map((step, idx) => (
                <tr key={idx}>
                  <td>
                    <span className={`pill-badge ${step.method === 'GET' ? 'pill-badge-green' : step.method === 'POST' ? 'pill-badge-blue' : 'pill-badge-amber'}`}>
                      {step.method}
                    </span>
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{step.resolvedPath || step.endpoint}</td>
                  <td>{step.status > 0 ? step.status : '-'}</td>
                  <td>{step.latencyMs}ms</td>
                  <td>
                    {step.passed ? (
                      <span style={{ color: '#047857', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <CheckCircle2 style={{ width: '1rem', height: '1rem' }} /> OK
                      </span>
                    ) : step.requiresApproval ? (
                      <span style={{ color: '#b45309', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <AlertTriangle style={{ width: '1rem', height: '1rem' }} /> Approval Needed
                      </span>
                    ) : step.blocked ? (
                      <span style={{ color: '#64748b', fontWeight: 600 }}>Blocked</span>
                    ) : (
                      <span style={{ color: '#b91c1c', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                        <XCircle style={{ width: '1rem', height: '1rem' }} /> Failed
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {report.rememberedContext && Object.keys(report.rememberedContext).filter((k) => !k.includes('base64')).length > 0 && (
            <div style={{ marginTop: '0.75rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              <strong>Reused Variables:</strong>{' '}
              {Object.entries(report.rememberedContext)
                .filter(([k]) => !k.includes('base64'))
                .map(([k, v]) => (
                  <span key={k} className="pill-badge pill-badge-purple" style={{ marginLeft: '0.375rem' }}>
                    {k}: {v}
                  </span>
                ))}
            </div>
          )}
        </div>
      );
    } catch {
      return null;
    }
  };

  return (
    <div className="ai-assistant-page">
      {/* Left Sidebar: Conversation History & Search */}
      <aside className="ai-sidebar">
        <div className="ai-sidebar-header">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Bot style={{ width: '1.25rem', height: '1.25rem', color: 'var(--primary)' }} />
              <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>AI Assistant</span>
            </div>
            <button className="btn btn-primary btn-sm" onClick={handleCreateNewChat} style={{ gap: '0.25rem' }}>
              <Plus style={{ width: '0.875rem', height: '0.875rem' }} /> New Chat
            </button>
          </div>

          <div style={{ position: 'relative' }}>
            <Search style={{ width: '0.875rem', height: '0.875rem', position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '2.125rem', fontSize: '0.8125rem' }}
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                loadConversations(e.target.value);
              }}
            />
          </div>
        </div>

        <div className="ai-conversation-list">
          {loadingList ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}>
              <LoadingSpinner message="Loading chats..." />
            </div>
          ) : conversations.length === 0 ? (
            <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
              No conversations found. Click <strong>+ New Chat</strong> to start testing!
            </div>
          ) : (
            conversations.map((chat) => (
              <div
                key={chat.id}
                className={`ai-conversation-item ${activeConversationId === chat.id ? 'active' : ''}`}
                onClick={() => setActiveConversationId(chat.id)}
              >
                <div style={{ flex: 1, minWidth: 0, paddingRight: '0.5rem' }}>
                  {editingChatId === chat.id ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={(e) => e.stopPropagation()}>
                      <input
                        type="text"
                        className="form-input"
                        style={{ fontSize: '0.8125rem', padding: '0.25rem 0.5rem' }}
                        value={editTitle}
                        onChange={(e) => setEditTitle(e.target.value)}
                        autoFocus
                      />
                      <button className="btn btn-primary btn-sm" style={{ padding: '0.25rem' }} onClick={() => handleRename(chat.id)}>
                        <Check style={{ width: '0.75rem', height: '0.75rem' }} />
                      </button>
                      <button className="btn btn-secondary btn-sm" style={{ padding: '0.25rem' }} onClick={() => setEditingChatId(null)}>
                        <X style={{ width: '0.75rem', height: '0.75rem' }} />
                      </button>
                    </div>
                  ) : (
                    <>
                      <div style={{ fontWeight: 600, fontSize: '0.8125rem', color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {chat.title}
                      </div>
                      {chat.applicationName && (
                        <div style={{ marginTop: '0.25rem' }}>
                          <span className="pill-badge pill-badge-blue" style={{ fontSize: '0.6875rem', padding: '0.1rem 0.375rem' }}>
                            {chat.applicationName}
                          </span>
                        </div>
                      )}
                    </>
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }} onClick={(e) => e.stopPropagation()}>
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ padding: '0.25rem', color: 'var(--text-muted)' }}
                    title="Rename"
                    onClick={() => {
                      setEditingChatId(chat.id);
                      setEditTitle(chat.title);
                    }}
                  >
                    <Edit2 style={{ width: '0.75rem', height: '0.75rem' }} />
                  </button>
                  <button
                    className="btn btn-ghost btn-sm"
                    style={{ padding: '0.25rem', color: '#ef4444' }}
                    title="Delete"
                    onClick={() => setDeleteConfirmId(chat.id)}
                  >
                    <Trash2 style={{ width: '0.75rem', height: '0.75rem' }} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </aside>

      {/* Main Chat Interface */}
      <main className="ai-chat-area">
        {/* Header */}
        <header className="ai-chat-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ width: '2rem', height: '2rem', borderRadius: 'var(--radius-md)', backgroundColor: '#eff6ff', color: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Bot style={{ width: '1.25rem', height: '1.25rem' }} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                {activeConversation?.title || 'Sentinel AI Test Assistant'}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Autonomous API Discovery & Dependency Chaining via Sentinel Gateway
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <select
              className="form-select"
              style={{ fontSize: '0.8125rem', width: '180px' }}
              value={selectedAppId || ''}
              onChange={(e) => setSelectedAppId(Number(e.target.value))}
            >
              {applications.map((app) => (
                <option key={app.id} value={app.id}>
                  {app.name}
                </option>
              ))}
            </select>

            <button
              className="btn btn-primary btn-sm"
              onClick={handleRunAiTestSuite}
              disabled={runningTest || !selectedAppId}
              style={{ gap: '0.375rem' }}
            >
              <Zap style={{ width: '0.875rem', height: '0.875rem' }} />
              {runningTest ? 'Running AI Tests...' : 'Run AI Test Suite'}
            </button>
          </div>
        </header>

        {/* Messages */}
        <div className="ai-chat-messages">
          {loadingChat ? (
            <div style={{ padding: '3rem', textAlign: 'center' }}>
              <LoadingSpinner message="Loading messages..." />
            </div>
          ) : !activeConversation || activeConversation.messages.length === 0 ? (
            <div style={{ padding: '4rem 2rem', textAlign: 'center', maxWidth: '500px', margin: '0 auto' }}>
              <div style={{ width: '3.5rem', height: '3.5rem', borderRadius: '50%', backgroundColor: '#eff6ff', color: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.25rem' }}>
                <Sparkles style={{ width: '1.75rem', height: '1.75rem' }} />
              </div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
                Sentinel AI Observability Copilot
              </h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '1.5rem' }}>
                Interact with Sentinel's live database, API catalog, telemetry, and automated gateway test engine.
              </p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPrompt('give list of apis')}
                  style={{ textAlign: 'left', justifyContent: 'flex-start', fontSize: '0.8125rem' }}
                >
                  ⚡ "give list of apis"
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPrompt('show the health status of all applications')}
                  style={{ textAlign: 'left', justifyContent: 'flex-start', fontSize: '0.8125rem' }}
                >
                  🩺 "show the health status of all applications"
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPrompt('give me the system overview')}
                  style={{ textAlign: 'left', justifyContent: 'flex-start', fontSize: '0.8125rem' }}
                >
                  🌐 "give me the system overview"
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPrompt('are there any unhealthy applications?')}
                  style={{ textAlign: 'left', justifyContent: 'flex-start', fontSize: '0.8125rem' }}
                >
                  ⚠️ "are there any unhealthy applications?"
                </button>
              </div>
            </div>
          ) : (
            activeConversation.messages.map((msg) => (
              <div key={msg.id} className={`ai-message-bubble ${msg.sender.toLowerCase()}`}>
                <div className="ai-message-content">
                  {msg.sender === 'USER' ? (
                    <div style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</div>
                  ) : (
                    <MarkdownViewer content={msg.content} />
                  )}
                  {renderStructuredReport(msg.metadataJson)}
                </div>
              </div>
            ))
          )}

          {(sending || runningTest) && (
            <div className="ai-message-bubble assistant">
              <div className="ai-message-content" style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <div className="status-dot status-dot-healthy animate-spin" />
                <span style={{ color: 'var(--text-secondary)' }}>
                  {runningTest ? 'Executing automated AI test suite via Sentinel Gateway...' : 'Sentinel AI is thinking...'}
                </span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div className="ai-chat-input-area">
          {attachedFile && (
            <div style={{ marginBottom: '0.5rem' }}>
              <div className="console-file-chip">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileCode style={{ width: '1rem', height: '1rem', color: 'var(--primary)' }} />
                  <span style={{ fontWeight: 600 }}>{attachedFile.name}</span>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                    ({(attachedFile.size / 1024).toFixed(1)} KB)
                  </span>
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  style={{ padding: '0.15rem', color: '#ef4444' }}
                  onClick={() => setAttachedFile(null)}
                >
                  <X style={{ width: '0.875rem', height: '0.875rem' }} />
                </button>
              </div>
            </div>
          )}

          <form onSubmit={handleSendMessage} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <input
              type="file"
              ref={fileInputRef}
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              title="Attach File/Image for AI Testing"
              onClick={() => fileInputRef.current?.click()}
              style={{ padding: '0.625rem 0.75rem' }}
            >
              <UploadCloud style={{ width: '1.125rem', height: '1.125rem' }} />
            </button>

            <input
              type="text"
              className="form-input"
              placeholder="Ask Sentinel AI to test APIs, upload files, or analyze failures..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              disabled={sending || runningTest}
              style={{ flex: 1 }}
            />

            <button
              type="submit"
              className="btn btn-primary"
              disabled={(!prompt.trim() && !attachedFile) || sending || runningTest}
              style={{ gap: '0.375rem' }}
            >
              <Send style={{ width: '1rem', height: '1rem' }} /> Send
            </button>
          </form>
        </div>
      </main>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={deleteConfirmId !== null}
        title="Delete Conversation"
        message="Are you sure you want to delete this conversation? All messages and test context will be permanently removed."
        confirmLabel="Delete"
        isDangerous={true}
        onConfirm={() => {
          if (deleteConfirmId) handleDelete(deleteConfirmId);
        }}
        onClose={() => setDeleteConfirmId(null)}
      />
    </div>
  );
};
