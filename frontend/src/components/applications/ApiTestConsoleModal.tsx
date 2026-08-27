import React, { useState, useEffect, useRef, useMemo } from 'react';
import { testConsoleApi } from '../../api/testConsole';
import { apiKeysApi } from '../../api/apiKeys';
import { applicationsApi } from '../../api/applications';
import { ApiKey } from '../../types/apiKey';
import { ApiEndpoint, Application } from '../../types/application';
import { ApiTestConsoleResult } from '../../types/testConsole';
import { CreateKeyModal } from '../apiKeys/CreateKeyModal';
import { ApiKeyModal } from '../apiKeys/ApiKeyModal';
import { conversationsApi } from '../../api/conversations';
import { AiTestPlan, AiTestRunReport } from '../../types/conversation';
import { useNavigate } from 'react-router-dom';
import {
  Copy,
  Check,
  RotateCcw,
  Code,
  Play,
  KeyRound,
  ShieldCheck,
  Globe,
  Server,
  AlertTriangle,
  UploadCloud,
  X,
  Plus,
  Clock,
  Sparkles,
  RefreshCw,
  FileText,
  Terminal,
  Layers,
  Info,
  CheckCircle2,
  XCircle,
  Hash,
  FileCode,
  Lock,
  Trash2,
  Send,
  Zap,
} from 'lucide-react';

interface ApiTestConsoleModalProps {
  isOpen: boolean;
  onClose: () => void;
  applicationId: number;
  initialEndpoint?: ApiEndpoint | null;
  onExecuted?: () => void;
}

interface RequestHistoryItem {
  id: string;
  timestamp: string;
  method: string;
  path: string;
  statusCode: number;
  latencyMs: number;
  requestId: string;
  result: ApiTestConsoleResult;
}

export const ApiTestConsoleModal: React.FC<ApiTestConsoleModalProps> = ({
  isOpen,
  onClose,
  applicationId,
  initialEndpoint,
  onExecuted,
}) => {
  const [app, setApp] = useState<Application | null>(null);
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [selectedKeyId, setSelectedKeyId] = useState<number | ''>('');
  const [method, setMethod] = useState<string>('GET');
  const [rawPath, setRawPath] = useState<string>('/');
  const [pathParams, setPathParams] = useState<Record<string, string>>({});
  const [queryParams, setQueryParams] = useState<{ key: string; value: string; description?: string }[]>([
    { key: '', value: '' },
  ]);
  const [headers, setHeaders] = useState<{ key: string; value: string }[]>([
    { key: 'Accept', value: 'application/json' },
  ]);
  const [bearerToken, setBearerToken] = useState<string>('');

  // Body configuration
  const [bodyType, setBodyType] = useState<'json' | 'multipart' | 'raw'>('json');
  const [bodyJson, setBodyJson] = useState<string>('');
  const [rawBodyText, setRawBodyText] = useState<string>('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileFieldName, setFileFieldName] = useState<string>('file');
  const [fileBase64, setFileBase64] = useState<string | null>(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState<string | null>(null);

  // Execution & UI state
  const [loading, setLoading] = useState<boolean>(false);
  const [loadingKeys, setLoadingKeys] = useState<boolean>(false);
  const [result, setResult] = useState<ApiTestConsoleResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copiedCurl, setCopiedCurl] = useState<boolean>(false);
  const [copiedResponse, setCopiedResponse] = useState<boolean>(false);
  const [activeRequestTab, setActiveRequestTab] = useState<'params' | 'headers' | 'body' | 'schema' | 'history'>('params');
  const [activeResponseTab, setActiveResponseTab] = useState<'response' | 'headers' | 'request' | 'curl'>('response');

  // Key creation sub-modals
  const [isCreateKeyOpen, setIsCreateKeyOpen] = useState<boolean>(false);
  const [createdRawKey, setCreatedRawKey] = useState<ApiKey | null>(null);

  // Resource extraction (e.g. captured id from response)
  const [detectedResourceId, setDetectedResourceId] = useState<string | null>(null);
  const [history, setHistory] = useState<RequestHistoryItem[]>([]);

  // AI Testing Mode state
  const [testMode, setTestMode] = useState<'manual' | 'ai'>('manual');
  const [aiPlan, setAiPlan] = useState<AiTestPlan | null>(null);
  const [aiReport, setAiReport] = useState<AiTestRunReport | null>(null);
  const [runningAiTest, setRunningAiTest] = useState<boolean>(false);
  const [loadingAiPlan, setLoadingAiPlan] = useState<boolean>(false);
  const [approveDestructive, setApproveDestructive] = useState<boolean>(false);

  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadApplication();
      loadKeys();
      initializeEndpoint(initialEndpoint);
      setResult(null);
      setError(null);
      if (testMode === 'ai') {
        loadAiPlan();
      }
    }
  }, [isOpen, applicationId, initialEndpoint, testMode]);

  const loadAiPlan = async () => {
    setLoadingAiPlan(true);
    try {
      const plan = await conversationsApi.getAiTestPlan(applicationId);
      setAiPlan(plan);
    } catch (err) {
      console.error('Failed to load AI test plan', err);
    } finally {
      setLoadingAiPlan(false);
    }
  };

  const handleExecuteAiTest = async () => {
    if (runningAiTest) return;
    setRunningAiTest(true);
    setError(null);
    try {
      const report = await conversationsApi.runDirectAiTest(applicationId, {
        apiKeyId: selectedKeyId ? Number(selectedKeyId) : undefined,
        approveDestructiveOperations: approveDestructive,
        fileBase64: fileBase64 || undefined,
        fileName: selectedFile?.name || undefined,
        fileContentType: selectedFile?.type || undefined,
      });
      setAiReport(report);
      if (onExecuted) {
        onExecuted();
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message || 'AI Test Execution failed';
      setError(msg);
    } finally {
      setRunningAiTest(false);
    }
  };

  const handleOpenInAssistant = () => {
    onClose();
    navigate('/assistant');
  };

  const loadApplication = async () => {
    try {
      const appData = await applicationsApi.getById(applicationId);
      setApp(appData);
    } catch (err) {
      console.error('Failed to load application details', err);
    }
  };

  const loadKeys = async (preferredKeyId?: number) => {
    setLoadingKeys(true);
    try {
      const data: ApiKey[] = await apiKeysApi.list(applicationId);
      const activeKeys = data.filter((k: ApiKey) => k.active);
      setKeys(activeKeys);
      if (preferredKeyId && activeKeys.some((k) => k.id === preferredKeyId)) {
        setSelectedKeyId(preferredKeyId);
      } else if (activeKeys.length > 0) {
        setSelectedKeyId((prev) => (prev && activeKeys.some((k) => k.id === prev) ? prev : activeKeys[0].id));
      } else {
        setSelectedKeyId('');
      }
    } catch (err: any) {
      console.error('Failed to load application keys', err);
    } finally {
      setLoadingKeys(false);
    }
  };

  const initializeEndpoint = (ep?: ApiEndpoint | null) => {
    if (ep) {
      const m = (ep.method || 'GET').toUpperCase();
      setMethod(m);
      const cleanPath = ep.normalizedPath || '/';
      setRawPath(cleanPath);

      // Extract template variables from path
      const paramNames = extractParamNames(cleanPath);
      const initialPathParams: Record<string, string> = {};
      paramNames.forEach((name) => {
        initialPathParams[name] = (name === 'image_id' && detectedResourceId ? detectedResourceId : '');
      });

      // Parse OpenAPI parameters if available
      const parsedQueryParams: { key: string; value: string; description?: string }[] = [];
      const parsedHeaders: { key: string; value: string }[] = [{ key: 'Accept', value: 'application/json' }];

      if (ep.parametersJson) {
        try {
          const paramsList = JSON.parse(ep.parametersJson);
          if (Array.isArray(paramsList)) {
            paramsList.forEach((p: any) => {
              if (p.in === 'path' && p.name) {
                if (initialPathParams[p.name] === undefined || initialPathParams[p.name] === '') {
                  initialPathParams[p.name] = p.schema?.default || p.example || (p.name === 'image_id' && detectedResourceId ? detectedResourceId : '');
                }
              } else if (p.in === 'query' && p.name) {
                parsedQueryParams.push({
                  key: p.name,
                  value: String(p.schema?.default || p.example || ''),
                  description: p.description || undefined,
                });
              } else if (p.in === 'header' && p.name) {
                const lower = p.name.toLowerCase();
                if (!lower.startsWith('x-sentinel-') && lower !== 'authorization') {
                  parsedHeaders.push({
                    key: p.name,
                    value: String(p.schema?.default || p.example || ''),
                  });
                }
              }
            });
          }
        } catch {
          // Non-critical parameter parse error
        }
      }

      setPathParams(initialPathParams);
      if (parsedQueryParams.length > 0) {
        setQueryParams(parsedQueryParams);
      } else {
        setQueryParams([{ key: '', value: '' }]);
      }
      setHeaders(parsedHeaders);

      // Parse OpenAPI requestBody schema if present
      let isMultipart = cleanPath.toLowerCase().includes('upload');
      let defaultJsonBody = '';
      let determinedFieldName = 'file';

      if (ep.requestBodySchemaJson) {
        try {
          const rb = JSON.parse(ep.requestBodySchemaJson);
          const content = rb.content || {};
          if (content['multipart/form-data']) {
            isMultipart = true;
            const schemaProps = content['multipart/form-data'].schema?.properties;
            if (schemaProps) {
              const keys = Object.keys(schemaProps);
              if (keys.length > 0) {
                determinedFieldName = keys[0];
              }
            }
          } else if (content['application/json']) {
            const jsonSchema = content['application/json'].schema;
            const example = content['application/json'].example;
            if (example) {
              defaultJsonBody = JSON.stringify(example, null, 2);
            } else if (jsonSchema) {
              defaultJsonBody = generateExampleFromSchema(jsonSchema);
            }
          }
        } catch {
          // Non-critical schema parse error
        }
      }

      setFileFieldName(determinedFieldName);

      if (isMultipart) {
        setBodyType('multipart');
        setActiveRequestTab('body');
      } else if (['POST', 'PUT', 'PATCH'].includes(m)) {
        setBodyType('json');
        setBodyJson(defaultJsonBody || '{\n  \n}');
        setActiveRequestTab('body');
      } else {
        setActiveRequestTab(paramNames.length > 0 || parsedQueryParams.length > 0 ? 'params' : 'headers');
      }
    } else {
      setMethod('GET');
      setRawPath('/');
      setPathParams({});
      setQueryParams([{ key: '', value: '' }]);
      setHeaders([{ key: 'Accept', value: 'application/json' }]);
      setBodyJson('');
      setRawBodyText('');
      setSelectedFile(null);
      setFileBase64(null);
      setFilePreviewUrl(null);
      setActiveRequestTab('params');
    }
  };

  const generateExampleFromSchema = (schema: any): string => {
    try {
      if (!schema) return '{\n  \n}';
      if (schema.example) return JSON.stringify(schema.example, null, 2);
      if (schema.type === 'object' && schema.properties) {
        const obj: Record<string, any> = {};
        Object.entries(schema.properties).forEach(([k, prop]: [string, any]) => {
          if (prop.default !== undefined) obj[k] = prop.default;
          else if (prop.example !== undefined) obj[k] = prop.example;
          else if (prop.type === 'string') obj[k] = 'string';
          else if (prop.type === 'number' || prop.type === 'integer') obj[k] = 0;
          else if (prop.type === 'boolean') obj[k] = true;
          else if (prop.type === 'array') obj[k] = [];
          else obj[k] = {};
        });
        return JSON.stringify(obj, null, 2);
      }
    } catch {
      // Fallback
    }
    return '{\n  \n}';
  };

  const extractParamNames = (pathStr: string): string[] => {
    const matches = pathStr.match(/\{([^}]+)\}/g);
    if (!matches) return [];
    return matches.map((m) => m.replace(/[{}]/g, ''));
  };

  const handlePathChange = (newPath: string) => {
    setRawPath(newPath);
    const paramNames = extractParamNames(newPath);
    const updated: Record<string, string> = {};
    paramNames.forEach((p) => {
      updated[p] = pathParams[p] || (p === 'image_id' && detectedResourceId ? detectedResourceId : '');
    });
    setPathParams(updated);
  };

  const getEvaluatedPath = (): string => {
    let finalPath = rawPath.trim();
    if (!finalPath.startsWith('/')) {
      finalPath = '/' + finalPath;
    }
    Object.entries(pathParams).forEach(([k, v]) => {
      finalPath = finalPath.replace(new RegExp(`\\{${k}\\}`, 'g'), v.trim() || `{${k}}`);
    });
    return finalPath;
  };

  const fullGatewayUrl = useMemo(() => {
    const evaluated = getEvaluatedPath();
    let url = `http://localhost:8080/api/v1/gateway${evaluated}`;
    const activeParams = queryParams.filter((p) => p.key.trim() !== '');
    if (activeParams.length > 0) {
      const q = new URLSearchParams();
      activeParams.forEach((p) => q.append(p.key.trim(), p.value));
      url += `?${q.toString()}`;
    }
    return url;
  }, [rawPath, pathParams, queryParams]);

  const targetUpstreamUrl = useMemo(() => {
    const evaluated = getEvaluatedPath();
    const base = (app?.baseUrl || 'https://api.example.com').replace(/\/+$/, '');
    let url = `${base}${evaluated}`;
    const activeParams = queryParams.filter((p) => p.key.trim() !== '');
    if (activeParams.length > 0) {
      const q = new URLSearchParams();
      activeParams.forEach((p) => q.append(p.key.trim(), p.value));
      url += `?${q.toString()}`;
    }
    return url;
  }, [app?.baseUrl, rawPath, pathParams, queryParams]);

  const handleAddQueryParam = () => setQueryParams([...queryParams, { key: '', value: '' }]);
  const handleRemoveQueryParam = (index: number) => {
    if (queryParams.length <= 1) {
      setQueryParams([{ key: '', value: '' }]);
    } else {
      setQueryParams(queryParams.filter((_, i) => i !== index));
    }
  };

  const handleAddHeader = () => setHeaders([...headers, { key: '', value: '' }]);
  const handleRemoveHeader = (index: number) => {
    if (headers.length <= 1) {
      setHeaders([{ key: '', value: '' }]);
    } else {
      setHeaders(headers.filter((_, i) => i !== index));
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      const reader = new FileReader();
      reader.onload = () => {
        const fullDataUrl = reader.result as string;
        const base64 = fullDataUrl.split(',')[1];
        setFileBase64(base64);
        if (file.type.startsWith('image/')) {
          setFilePreviewUrl(fullDataUrl);
        } else {
          setFilePreviewUrl(null);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRemoveFile = (e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedFile(null);
    setFileBase64(null);
    setFilePreviewUrl(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleFormatJson = () => {
    try {
      const parsed = JSON.parse(bodyJson);
      setBodyJson(JSON.stringify(parsed, null, 2));
      setError(null);
    } catch {
      setError('Invalid JSON payload in request body');
    }
  };

  const handleReset = () => {
    initializeEndpoint(initialEndpoint);
    setResult(null);
    setError(null);
  };

  const selectedKey = useMemo(() => {
    return keys.find((k) => k.id === selectedKeyId) || null;
  }, [keys, selectedKeyId]);

  const getMaskedKeyDisplay = (): string => {
    if (!selectedKey) return '••••••••••••••••';
    if (selectedKey.maskedKey) return selectedKey.maskedKey;
    return `sk_••••••••${String(selectedKey.id).slice(-4)}`;
  };

  const handleCopyCurl = () => {
    const keyPlaceholder = selectedKey?.maskedKey || 'YOUR_SENTINEL_API_KEY';
    let cmd = `curl -X ${method} "${fullGatewayUrl}" \\\n  -H "X-Sentinel-API-Key: ${keyPlaceholder}"`;

    headers.forEach((h) => {
      const k = h.key.trim();
      const lower = k.toLowerCase();
      if (k && !lower.startsWith('x-sentinel-')) {
        cmd += ` \\\n  -H "${k}: ${h.value}"`;
      }
    });

    if (bodyType === 'multipart' && selectedFile) {
      cmd += ` \\\n  -F "${fileFieldName}=@${selectedFile.name}"`;
    } else if (bodyType === 'json' && bodyJson.trim() && ['POST', 'PUT', 'PATCH'].includes(method)) {
      cmd += ` \\\n  -H "Content-Type: application/json" \\\n  -d '${bodyJson.trim().replace(/'/g, "'\\''")}'`;
    } else if (bodyType === 'raw' && rawBodyText.trim() && ['POST', 'PUT', 'PATCH'].includes(method)) {
      cmd += ` \\\n  -d '${rawBodyText.trim().replace(/'/g, "'\\''")}'`;
    }

    navigator.clipboard.writeText(cmd);
    setCopiedCurl(true);
    setTimeout(() => setCopiedCurl(false), 2000);
  };

  const handleCopyResponse = () => {
    if (result?.responseBody) {
      navigator.clipboard.writeText(result.responseBody);
      setCopiedResponse(true);
      setTimeout(() => setCopiedResponse(false), 2000);
    }
  };

  const handleExecute = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!selectedKeyId) {
      setError('Please select or create an active Sentinel API Key to execute requests.');
      return;
    }

    const finalPath = getEvaluatedPath();
    if (finalPath.includes('{') && finalPath.includes('}')) {
      setError('Please provide values for all path parameters before sending the request.');
      return;
    }

    if (bodyType === 'json' && bodyJson.trim() && ['POST', 'PUT', 'PATCH'].includes(method)) {
      try {
        JSON.parse(bodyJson);
      } catch {
        setError('Invalid JSON syntax in Request Body. Please check or format the JSON payload.');
        return;
      }
    }

    setLoading(true);
    setError(null);
    setResult(null);

    const queryMap: Record<string, string> = {};
    queryParams.forEach((p) => {
      if (p.key.trim()) queryMap[p.key.trim()] = p.value;
    });

    const headerMap: Record<string, string> = {};
    headers.forEach((h) => {
      const k = h.key.trim();
      const lower = k.toLowerCase();
      if (k && !lower.startsWith('x-sentinel-') && !lower.startsWith('x-internal-')) {
        headerMap[k] = h.value;
      }
    });

    if (bearerToken.trim() && !headerMap['Authorization'] && !headerMap['authorization']) {
      headerMap['Authorization'] = bearerToken.trim().startsWith('Bearer ') ? bearerToken.trim() : `Bearer ${bearerToken.trim()}`;
    }

    let payloadBody: string | undefined = undefined;
    let binaryBase64: string | undefined = undefined;
    let fileName: string | undefined = undefined;
    let fileField: string | undefined = undefined;
    let fileContentType: string | undefined = undefined;

    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      if (bodyType === 'multipart' && fileBase64 && selectedFile) {
        binaryBase64 = fileBase64;
        fileName = selectedFile.name;
        fileField = fileFieldName || 'file';
        fileContentType = selectedFile.type || 'image/jpeg';
      } else if (bodyType === 'json' && bodyJson.trim()) {
        payloadBody = bodyJson.trim();
        if (!headerMap['Content-Type'] && !headerMap['content-type']) {
          headerMap['Content-Type'] = 'application/json';
        }
      } else if (bodyType === 'raw' && rawBodyText.trim()) {
        payloadBody = rawBodyText.trim();
      }
    }

    try {
      const res = await testConsoleApi.executeTest(applicationId, {
        apiKeyId: Number(selectedKeyId),
        method,
        path: finalPath,
        queryParams: Object.keys(queryMap).length > 0 ? queryMap : undefined,
        headers: Object.keys(headerMap).length > 0 ? headerMap : undefined,
        body: payloadBody,
        binaryBodyBase64: binaryBase64,
        fileName,
        fileFieldName: fileField,
        fileContentType,
      });

      setResult(res);
      setActiveResponseTab('response');

      // Auto-capture resource IDs and auth tokens
      try {
        if (res.responseBody) {
          const parsed = JSON.parse(res.responseBody);
          const resId = parsed.image_id || parsed.imageId || parsed.id || parsed.data?.id;
          if (resId) {
            setDetectedResourceId(String(resId));
          }
          const extractedToken = parsed.token || parsed.accessToken || parsed.access_token || parsed.jwt;
          if (extractedToken) {
            setBearerToken(String(extractedToken));
          }
        }
      } catch {
        // Non-JSON response
      }

      // Add to session history
      const historyEntry: RequestHistoryItem = {
        id: Math.random().toString(36).substring(7),
        timestamp: new Date().toLocaleTimeString(),
        method,
        path: finalPath,
        statusCode: res.statusCode,
        latencyMs: res.latencyMs,
        requestId: res.requestId,
        result: res,
      };
      setHistory((prev) => [historyEntry, ...prev.slice(0, 9)]);

      if (onExecuted) {
        onExecuted();
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message || 'Execution failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const applyDetectedIdToEndpoint = (targetEndpointPath: string, targetMethod: string) => {
    if (!detectedResourceId) return;
    setMethod(targetMethod);
    setRawPath(targetEndpointPath);
    setPathParams({ image_id: detectedResourceId, id: detectedResourceId });
    setResult(null);
    setError(null);
    setActiveRequestTab('params');
  };

  const getStatusBadge = (status: number) => {
    if (status >= 200 && status < 300) {
      return (
        <span className="badge badge-healthy">
          <CheckCircle2 style={{ width: 12, height: 12 }} />
          {status} OK
        </span>
      );
    }
    if (status === 401) {
      return (
        <span className="badge badge-degraded">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Unauthorized
        </span>
      );
    }
    if (status === 403) {
      return (
        <span className="badge badge-unavailable">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Forbidden
        </span>
      );
    }
    if (status === 404) {
      return (
        <span className="badge badge-degraded">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Not Found
        </span>
      );
    }
    if (status === 422) {
      return (
        <span className="badge badge-degraded">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Unprocessable
        </span>
      );
    }
    if (status === 429) {
      return (
        <span className="badge badge-unavailable">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Rate Limited
        </span>
      );
    }
    if (status >= 500) {
      return (
        <span className="badge badge-unavailable">
          <XCircle style={{ width: 12, height: 12 }} />
          {status} Gateway / Upstream Error
        </span>
      );
    }
    return <span className="badge badge-unknown">{status}</span>;
  };

  const getMethodClass = (m: string) => {
    const lower = m.toLowerCase();
    if (lower === 'get') return 'get';
    if (lower === 'post') return 'post';
    if (lower === 'put') return 'put';
    if (lower === 'patch') return 'patch';
    if (lower === 'delete') return 'delete';
    return '';
  };

  if (!isOpen) return null;

  return (
    <>
      <div className="test-console-overlay" onClick={onClose}>
        <div
          className="test-console-modal"
          onClick={(e) => e.stopPropagation()}
        >
          
          {/* Header */}
          <div className="test-console-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div
                style={{
                  padding: '0.5rem',
                  backgroundColor: 'var(--primary-light)',
                  color: 'var(--primary)',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Terminal style={{ width: 20, height: 20 }} />
              </div>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                    Developer API Test Console
                  </h3>
                  <span className="pill-badge pill-badge-blue">
                    {app?.name || 'Application'}
                  </span>
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                  Send real HTTP requests through the Sentinel Gateway pipeline
                </div>
              </div>
            </div>

            {/* Mode Switcher */}
            <div className="mode-switch-container">
              <button
                type="button"
                className={`mode-switch-btn ${testMode === 'manual' ? 'active' : ''}`}
                onClick={() => setTestMode('manual')}
              >
                <Terminal style={{ width: 14, height: 14 }} />
                <span>Manual Test</span>
              </button>
              <button
                type="button"
                className={`mode-switch-btn ${testMode === 'ai' ? 'active' : ''}`}
                onClick={() => {
                  setTestMode('ai');
                  if (!aiPlan) loadAiPlan();
                }}
              >
                <Sparkles style={{ width: 14, height: 14 }} />
                <span>AI Test Mode</span>
              </button>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={handleCopyCurl}
                className="btn btn-secondary btn-sm"
                title="Copy request as working cURL command"
              >
                {copiedCurl ? (
                  <>
                    <Check style={{ width: 14, height: 14, color: 'var(--success)' }} />
                    <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied cURL</span>
                  </>
                ) : (
                  <>
                    <Copy style={{ width: 14, height: 14 }} />
                    <span>Copy cURL</span>
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={handleReset}
                className="btn btn-secondary btn-sm"
                title="Reset Request Builder to endpoint defaults"
              >
                <RotateCcw style={{ width: 14, height: 14 }} />
                <span>Reset</span>
              </button>
              <button
                onClick={onClose}
                className="btn btn-secondary btn-sm btn-icon"
                title="Close console"
                aria-label="Close"
              >
                <X style={{ width: 16, height: 16 }} />
              </button>
            </div>
          </div>

          {/* Body Content */}
          <div className="test-console-body">
            
            {/* Execution Error Banner */}
            {error && (
              <div
                style={{
                  padding: '0.875rem 1rem',
                  backgroundColor: 'var(--danger-light)',
                  border: '1px solid var(--danger-border)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger-text)',
                  fontSize: '0.875rem',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.625rem',
                }}
              >
                <AlertTriangle style={{ width: 18, height: 18, color: 'var(--danger)', flexShrink: 0, marginTop: 2 }} />
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600 }}>Execution Error</div>
                  <div style={{ fontSize: '0.8125rem', marginTop: '0.15rem' }}>{error}</div>
                </div>
              </div>
            )}

            {/* 1. Application Information Card */}
            <div className="console-card">
              <div className="console-card-header">
                <div className="console-section-title">
                  <Server style={{ width: 15, height: 15, color: 'var(--primary)' }} />
                  Application & Gateway Target
                </div>
                <span className="pill-badge pill-badge-green">
                  <CheckCircle2 style={{ width: 12, height: 12 }} />
                  Sentinel Protected
                </span>
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.75rem',
                  fontSize: '0.8125rem',
                }}
              >
                <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.2rem' }}>
                    Application
                  </div>
                  <div style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                    {app?.name || 'Loading...'}
                  </div>
                </div>

                <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.2rem' }}>
                    Upstream Base URL
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)', fontSize: '0.75rem', wordBreak: 'break-all' }}>
                    {app?.baseUrl || '—'}
                  </div>
                </div>

                <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.2rem' }}>
                    Gateway Endpoint
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', color: 'var(--primary)', fontSize: '0.75rem', fontWeight: 600 }}>
                    http://localhost:8080/api/v1/gateway
                  </div>
                </div>

                <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.2rem' }}>
                    Authentication
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: 'var(--success-text)', fontWeight: 600 }}>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: 'var(--success)' }} />
                    Sentinel Developer Key
                  </div>
                </div>
              </div>
            </div>

            {/* 2. Developer API Key Section */}
            <div className="console-card">
              <div className="console-card-header">
                <div className="console-section-title">
                  <KeyRound style={{ width: 15, height: 15, color: 'var(--primary)' }} />
                  Developer API Key
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => loadKeys(selectedKeyId ? Number(selectedKeyId) : undefined)}
                    disabled={loadingKeys}
                    className="btn btn-secondary btn-sm"
                    style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                    title="Refresh keys for this application"
                  >
                    <RefreshCw style={{ width: 12, height: 12 }} className={loadingKeys ? 'animate-spin' : ''} />
                    <span>Refresh Keys</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsCreateKeyOpen(true)}
                    className="btn btn-primary btn-sm"
                    style={{ padding: '0.25rem 0.625rem', fontSize: '0.75rem' }}
                    title="Create a new Developer API Key"
                  >
                    <Plus style={{ width: 12, height: 12 }} />
                    <span>Create Key</span>
                  </button>
                </div>
              </div>

              {keys.length === 0 && !loadingKeys ? (
                <div
                  style={{
                    padding: '1rem',
                    backgroundColor: 'var(--warning-light)',
                    border: '1px solid var(--warning-border)',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.75rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem' }}>
                    <AlertTriangle style={{ width: 18, height: 18, color: 'var(--warning)', flexShrink: 0, marginTop: 1 }} />
                    <div>
                      <div style={{ fontWeight: 600, color: 'var(--warning-text)', fontSize: '0.875rem' }}>
                        No Developer API Key
                      </div>
                      <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                        Create a key to test this API through Sentinel.
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      type="button"
                      onClick={() => setIsCreateKeyOpen(true)}
                      className="btn btn-primary btn-sm"
                    >
                      <Plus style={{ width: 14, height: 14 }} />
                      Create Developer API Key
                    </button>
                    <button
                      type="button"
                      onClick={() => loadKeys()}
                      className="btn btn-secondary btn-sm"
                    >
                      <RefreshCw style={{ width: 14, height: 14 }} />
                      Refresh Keys
                    </button>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ flex: 1, minWidth: '240px' }}>
                    <select
                      value={selectedKeyId}
                      onChange={(e) => setSelectedKeyId(e.target.value ? Number(e.target.value) : '')}
                      className="form-select"
                      style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', padding: '0.5rem 0.75rem' }}
                      disabled={loadingKeys || keys.length === 0}
                    >
                      {keys.map((k) => (
                        <option key={k.id} value={k.id}>
                          {k.name} ({k.maskedKey || `sk_••••••••${k.id}`} — {k.rateLimitPerMinute} req/min)
                        </option>
                      ))}
                    </select>
                  </div>

                  <div
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      padding: '0.45rem 0.75rem',
                      backgroundColor: 'var(--bg-subtle)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.75rem',
                      color: 'var(--text-secondary)',
                    }}
                  >
                    <ShieldCheck style={{ width: 14, height: 14, color: 'var(--success)' }} />
                    <span>Transmitted as <code style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--primary)' }}>X-Sentinel-API-Key</code></span>
                  </div>
                </div>
              )}
            </div>

            {testMode === 'manual' ? (
              <>
                {/* 3. Request Builder */}
                <div className="console-card">
                  <div className="console-card-header">
                <div className="console-section-title">
                  <Globe style={{ width: 15, height: 15, color: 'var(--primary)' }} />
                  Request
                </div>
                {initialEndpoint?.summary && (
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    {initialEndpoint.summary}
                  </span>
                )}
              </div>

              {/* Method + Target Endpoint row */}
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'stretch' }}>
                <select
                  value={method}
                  onChange={(e) => setMethod(e.target.value)}
                  className={`method-select ${getMethodClass(method)}`}
                >
                  {['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>

                <div style={{ flex: 1, position: 'relative' }}>
                  <input
                    type="text"
                    value={rawPath}
                    onChange={(e) => handlePathChange(e.target.value)}
                    placeholder="/api/v1/health"
                    className="form-input"
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: '0.875rem',
                      fontWeight: 600,
                      color: 'var(--text-primary)',
                      padding: '0.5rem 0.75rem',
                    }}
                    required
                  />
                </div>
              </div>

              {/* Target & Gateway URL breakdown */}
              <div
                style={{
                  marginTop: '0.75rem',
                  padding: '0.625rem 0.875rem',
                  backgroundColor: 'var(--bg-subtle)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.35rem',
                  fontSize: '0.75rem',
                  fontFamily: 'var(--font-mono)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                  <span>Target Endpoint: <strong style={{ color: 'var(--text-primary)' }}>{method} {getEvaluatedPath()}</strong></span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
                  <span>Sentinel Gateway URL: <strong style={{ color: 'var(--primary)' }}>{fullGatewayUrl}</strong></span>
                </div>
              </div>

              {/* Path Parameters Section */}
              {Object.keys(pathParams).length > 0 && (
                <div
                  style={{
                    marginTop: '0.875rem',
                    padding: '0.875rem',
                    backgroundColor: '#eff6ff',
                    border: '1px solid #bfdbfe',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.625rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontWeight: 600, fontSize: '0.8125rem', color: '#1e40af' }}>
                      <Sparkles style={{ width: 14, height: 14 }} />
                      <span>Path Parameters</span>
                      <span className="pill-badge pill-badge-amber" style={{ fontSize: '0.7rem', padding: '0.1rem 0.4rem' }}>
                        Required
                      </span>
                    </div>

                    {detectedResourceId && (
                      <button
                        type="button"
                        onClick={() => {
                          const updated = { ...pathParams };
                          Object.keys(updated).forEach((k) => (updated[k] = detectedResourceId));
                          setPathParams(updated);
                        }}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', height: 'auto' }}
                      >
                        Fill with captured ID: {detectedResourceId}
                      </button>
                    )}
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.5rem' }}>
                    {Object.keys(pathParams).map((paramKey) => (
                      <div key={paramKey} style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                        <span
                          style={{
                            fontFamily: 'var(--font-mono)',
                            fontSize: '0.75rem',
                            padding: '0.4rem 0.6rem',
                            backgroundColor: '#dbeafe',
                            borderRadius: 'var(--radius-sm)',
                            color: '#1e40af',
                            fontWeight: 700,
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {`{${paramKey}}`}
                        </span>
                        <input
                          type="text"
                          value={pathParams[paramKey]}
                          onChange={(e) => setPathParams({ ...pathParams, [paramKey]: e.target.value })}
                          placeholder={`Enter ${paramKey}`}
                          className="form-input"
                          style={{
                            fontFamily: 'var(--font-mono)',
                            fontSize: '0.8125rem',
                            padding: '0.4rem 0.6rem',
                            flex: 1,
                          }}
                          required
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Captured Resource ID Fast Actions */}
              {detectedResourceId && (
                <div
                  style={{
                    marginTop: '0.75rem',
                    padding: '0.625rem 0.875rem',
                    backgroundColor: 'var(--success-light)',
                    border: '1px solid var(--success-border)',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    flexWrap: 'wrap',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '0.5rem',
                    fontSize: '0.8125rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--success-text)', fontWeight: 600 }}>
                    <CheckCircle2 style={{ width: 16, height: 16 }} />
                    <span>Captured Resource ID: <code style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-primary)', backgroundColor: 'var(--bg-surface)', padding: '0.15rem 0.4rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--success-border)' }}>{detectedResourceId}</code></span>
                  </div>

                  <div style={{ display: 'flex', gap: '0.375rem' }}>
                    <button
                      type="button"
                      onClick={() => applyDetectedIdToEndpoint('/api/v1/images/{image_id}/analyze', 'POST')}
                      className="btn btn-secondary btn-sm"
                      style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                    >
                      Analyze
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDetectedIdToEndpoint('/api/v1/images/{image_id}/clean', 'POST')}
                      className="btn btn-secondary btn-sm"
                      style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                    >
                      Clean
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDetectedIdToEndpoint('/api/v1/images/{image_id}/report', 'GET')}
                      className="btn btn-secondary btn-sm"
                      style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                    >
                      Report
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDetectedIdToEndpoint('/api/v1/images/{image_id}/download', 'GET')}
                      className="btn btn-secondary btn-sm"
                      style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}
                    >
                      Download
                    </button>
                  </div>
                </div>
              )}

              {/* Tabs Section: Parameters, Headers, Body, OpenAPI, History */}
              <div style={{ marginTop: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                <div className="console-tabs-nav">
                  <button
                    type="button"
                    onClick={() => setActiveRequestTab('params')}
                    className={`console-tab-btn ${activeRequestTab === 'params' ? 'active' : ''}`}
                  >
                    <Hash style={{ width: 14, height: 14 }} />
                    <span>Parameters</span>
                    {queryParams.filter((p) => p.key.trim()).length > 0 && (
                      <span className="pill-badge pill-badge-gray" style={{ fontSize: '0.7rem', padding: '0.05rem 0.35rem' }}>
                        {queryParams.filter((p) => p.key.trim()).length}
                      </span>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => setActiveRequestTab('headers')}
                    className={`console-tab-btn ${activeRequestTab === 'headers' ? 'active' : ''}`}
                  >
                    <Layers style={{ width: 14, height: 14 }} />
                    <span>Headers</span>
                    <span className="pill-badge pill-badge-gray" style={{ fontSize: '0.7rem', padding: '0.05rem 0.35rem' }}>
                      {headers.filter((h) => h.key.trim()).length + 1}
                    </span>
                  </button>

                  {['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && (
                    <button
                      type="button"
                      onClick={() => setActiveRequestTab('body')}
                      className={`console-tab-btn ${activeRequestTab === 'body' ? 'active' : ''}`}
                    >
                      <FileCode style={{ width: 14, height: 14 }} />
                      <span>Body ({bodyType})</span>
                    </button>
                  )}

                  {initialEndpoint?.requestBodySchemaJson && (
                    <button
                      type="button"
                      onClick={() => setActiveRequestTab('schema')}
                      className={`console-tab-btn ${activeRequestTab === 'schema' ? 'active' : ''}`}
                    >
                      <Info style={{ width: 14, height: 14 }} />
                      <span>OpenAPI Spec</span>
                    </button>
                  )}

                  {history.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setActiveRequestTab('history')}
                      className={`console-tab-btn ${activeRequestTab === 'history' ? 'active' : ''}`}
                    >
                      <Clock style={{ width: 14, height: 14 }} />
                      <span>Recent ({history.length})</span>
                    </button>
                  )}
                </div>

                <div style={{ padding: '1rem', backgroundColor: 'var(--bg-surface)' }}>
                  
                  {/* Tab 1: Query Parameters */}
                  {activeRequestTab === 'params' && (
                    <div className="kv-container">
                      <div className="kv-header">
                        <div>Key</div>
                        <div>Value</div>
                        <div>Action</div>
                      </div>

                      {queryParams.map((param, index) => (
                        <div key={index} className="kv-row">
                          <input
                            type="text"
                            value={param.key}
                            onChange={(e) => {
                              const updated = [...queryParams];
                              updated[index].key = e.target.value;
                              setQueryParams(updated);
                            }}
                            placeholder="e.g. limit"
                            className="kv-input"
                          />
                          <input
                            type="text"
                            value={param.value}
                            onChange={(e) => {
                              const updated = [...queryParams];
                              updated[index].value = e.target.value;
                              setQueryParams(updated);
                            }}
                            placeholder="value"
                            className="kv-input"
                          />
                          <button
                            type="button"
                            onClick={() => handleRemoveQueryParam(index)}
                            className="btn btn-secondary btn-sm btn-icon"
                            style={{ width: 32, height: 32, padding: 0 }}
                            title="Remove parameter"
                          >
                            <Trash2 style={{ width: 14, height: 14, color: 'var(--danger)' }} />
                          </button>
                        </div>
                      ))}

                      <div style={{ marginTop: '0.5rem' }}>
                        <button
                          type="button"
                          onClick={handleAddQueryParam}
                          className="btn btn-secondary btn-sm"
                        >
                          <Plus style={{ width: 14, height: 14 }} />
                          Add Parameter
                        </button>
                      </div>
                    </div>
                  )}

                  {/* Tab 2: Headers */}
                  {activeRequestTab === 'headers' && (
                    <div className="kv-container">
                      <div className="kv-header">
                        <div>Header</div>
                        <div>Value</div>
                        <div>Status</div>
                      </div>

                      {/* Locked System Sentinel Header */}
                      <div className="kv-row">
                        <input
                          type="text"
                          value="X-Sentinel-API-Key"
                          readOnly
                          className="kv-input kv-input-readonly"
                          style={{ fontWeight: 700, color: 'var(--primary)' }}
                          title="System Header (Read-Only)"
                        />
                        <input
                          type="text"
                          value={getMaskedKeyDisplay()}
                          readOnly
                          className="kv-input kv-input-readonly"
                          title="Masked Developer API Key"
                        />
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 32,
                            height: 32,
                          }}
                          title="Locked & Managed by Sentinel"
                        >
                          <Lock style={{ width: 14, height: 14, color: 'var(--text-muted)' }} />
                        </div>
                      </div>

                      {/* Customer Custom Headers */}
                      {headers.map((header, index) => (
                        <div key={index} className="kv-row">
                          <input
                            type="text"
                            value={header.key}
                            onChange={(e) => {
                              const updated = [...headers];
                              updated[index].key = e.target.value;
                              setHeaders(updated);
                            }}
                            placeholder="Header Name"
                            className="kv-input"
                          />
                          <input
                            type="text"
                            value={header.value}
                            onChange={(e) => {
                              const updated = [...headers];
                              updated[index].value = e.target.value;
                              setHeaders(updated);
                            }}
                            placeholder="Header Value"
                            className="kv-input"
                          />
                          <button
                            type="button"
                            onClick={() => handleRemoveHeader(index)}
                            className="btn btn-secondary btn-sm btn-icon"
                            style={{ width: 32, height: 32, padding: 0 }}
                            title="Remove header"
                          >
                            <Trash2 style={{ width: 14, height: 14, color: 'var(--danger)' }} />
                          </button>
                        </div>
                      ))}

                      <div style={{ marginTop: '0.5rem' }}>
                        <button
                          type="button"
                          onClick={handleAddHeader}
                          className="btn btn-secondary btn-sm"
                        >
                          <Plus style={{ width: 14, height: 14 }} />
                          Add Header
                        </button>
                      </div>
                    </div>
                  )}

                  {/* Tab 3: Request Body */}
                  {activeRequestTab === 'body' && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <div style={{ display: 'flex', gap: '0.375rem' }}>
                          <button
                            type="button"
                            onClick={() => setBodyType('json')}
                            className={`btn btn-sm ${bodyType === 'json' ? 'btn-primary' : 'btn-secondary'}`}
                            style={{ padding: '0.25rem 0.625rem', fontSize: '0.75rem' }}
                          >
                            application/json
                          </button>
                          <button
                            type="button"
                            onClick={() => setBodyType('multipart')}
                            className={`btn btn-sm ${bodyType === 'multipart' ? 'btn-primary' : 'btn-secondary'}`}
                            style={{ padding: '0.25rem 0.625rem', fontSize: '0.75rem' }}
                          >
                            multipart/form-data
                          </button>
                          <button
                            type="button"
                            onClick={() => setBodyType('raw')}
                            className={`btn btn-sm ${bodyType === 'raw' ? 'btn-primary' : 'btn-secondary'}`}
                            style={{ padding: '0.25rem 0.625rem', fontSize: '0.75rem' }}
                          >
                            raw text
                          </button>
                        </div>

                        {bodyType === 'json' && (
                          <button
                            type="button"
                            onClick={handleFormatJson}
                            className="btn btn-secondary btn-sm"
                            style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                          >
                            <Code style={{ width: 12, height: 12 }} />
                            Format JSON
                          </button>
                        )}
                      </div>

                      {bodyType === 'json' && (
                        <textarea
                          value={bodyJson}
                          onChange={(e) => setBodyJson(e.target.value)}
                          rows={8}
                          placeholder='{\n  "key": "value"\n}'
                          className="console-code-editor"
                        />
                      )}

                      {bodyType === 'multipart' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                          <div
                            className="console-dropzone"
                            onClick={() => fileInputRef.current?.click()}
                          >
                            <UploadCloud style={{ width: 36, height: 36, color: 'var(--primary)', margin: '0 auto 0.5rem' }} />
                            <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                              {selectedFile ? 'Change Selected File' : 'Upload Image'}
                            </div>
                            <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                              Drag & drop your image here or <span style={{ color: 'var(--primary)', fontWeight: 600 }}>Choose File</span>
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                              JPG • PNG • WEBP • Binary
                            </div>
                            <input
                              ref={fileInputRef}
                              type="file"
                              onChange={handleFileSelect}
                              style={{ display: 'none' }}
                            />
                          </div>

                          {selectedFile && (
                            <div className="console-file-chip">
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <CheckCircle2 style={{ width: 16, height: 16, color: 'var(--success)' }} />
                                <div>
                                  <div style={{ fontWeight: 600 }}>{selectedFile.name}</div>
                                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                    {(selectedFile.size / 1024 / 1024).toFixed(2)} MB • {selectedFile.type || 'file'}
                                  </div>
                                </div>
                              </div>

                              <button
                                type="button"
                                onClick={handleRemoveFile}
                                className="btn btn-secondary btn-sm btn-icon"
                                style={{ width: 28, height: 28, padding: 0 }}
                                title="Remove file"
                              >
                                <X style={{ width: 14, height: 14 }} />
                              </button>
                            </div>
                          )}

                          {filePreviewUrl && (
                            <div style={{ textAlign: 'center', padding: '0.5rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                              <img
                                src={filePreviewUrl}
                                alt="Selected preview"
                                style={{ maxHeight: 110, borderRadius: 'var(--radius-sm)', objectFit: 'contain', margin: '0 auto' }}
                              />
                            </div>
                          )}

                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem' }}>
                            <label style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Form Field Name:</label>
                            <input
                              type="text"
                              value={fileFieldName}
                              onChange={(e) => setFileFieldName(e.target.value)}
                              placeholder="file"
                              className="form-input"
                              style={{ width: 140, padding: '0.35rem 0.5rem', fontFamily: 'var(--font-mono)' }}
                            />
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                              (Matches OpenAPI parameter name)
                            </span>
                          </div>
                        </div>
                      )}

                      {bodyType === 'raw' && (
                        <textarea
                          value={rawBodyText}
                          onChange={(e) => setRawBodyText(e.target.value)}
                          rows={8}
                          placeholder="Raw request payload"
                          className="console-code-editor"
                        />
                      )}
                    </div>
                  )}

                  {/* Tab 4: OpenAPI Spec Info */}
                  {activeRequestTab === 'schema' && (
                    <div style={{ fontSize: '0.8125rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                      {initialEndpoint?.description && (
                        <div style={{ padding: '0.75rem', backgroundColor: 'var(--bg-subtle)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                          <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>Description</div>
                          <div style={{ color: 'var(--text-secondary)' }}>{initialEndpoint.description}</div>
                        </div>
                      )}
                      {initialEndpoint?.requestBodySchemaJson && (
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Request Body Schema</div>
                          <pre className="console-code-viewer" style={{ maxHeight: 200 }}>
                            {JSON.stringify(JSON.parse(initialEndpoint.requestBodySchemaJson), null, 2)}
                          </pre>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Tab 5: Recent History */}
                  {activeRequestTab === 'history' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: 240, overflowY: 'auto' }}>
                      {history.map((item) => (
                        <div
                          key={item.id}
                          onClick={() => {
                            setResult(item.result);
                            setMethod(item.method);
                            setRawPath(item.path);
                            setActiveResponseTab('response');
                          }}
                          style={{
                            padding: '0.625rem 0.875rem',
                            backgroundColor: 'var(--bg-subtle)',
                            border: '1px solid var(--border-color)',
                            borderRadius: 'var(--radius-sm)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span className={`method-select ${getMethodClass(item.method)}`} style={{ padding: '0.15rem 0.4rem', fontSize: '0.7rem', minWidth: 'auto' }}>
                              {item.method}
                            </span>
                            <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                              {item.path}
                            </span>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.75rem' }}>
                            {getStatusBadge(item.statusCode)}
                            <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>{item.latencyMs}ms</span>
                            <span style={{ color: 'var(--text-muted)' }}>{item.timestamp}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* 4. Response Section */}
            <div className="console-card">
              <div className="console-card-header">
                <div className="console-section-title">
                  <Terminal style={{ width: 15, height: 15, color: 'var(--primary)' }} />
                  Response
                </div>

                {result && (
                  <button
                    type="button"
                    onClick={handleCopyResponse}
                    className="btn btn-secondary btn-sm"
                    style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                  >
                    {copiedResponse ? (
                      <>
                        <Check style={{ width: 12, height: 12, color: 'var(--success)' }} />
                        <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy style={{ width: 12, height: 12 }} />
                        <span>Copy Response</span>
                      </>
                    )}
                  </button>
                )}
              </div>

              {!result ? (
                <div
                  style={{
                    padding: '2.5rem 1.5rem',
                    textAlign: 'center',
                    backgroundColor: 'var(--bg-subtle)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px dashed var(--border-color)',
                  }}
                >
                  <Send style={{ width: 32, height: 32, color: 'var(--neutral)', margin: '0 auto 0.5rem' }} />
                  <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                    No response yet
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    Configure your request and click <strong>Send Request</strong> to test live execution.
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  
                  {/* Status & Metadata Row */}
                  <div
                    style={{
                      padding: '0.625rem 0.875rem',
                      backgroundColor: 'var(--bg-subtle)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: '0.75rem',
                      fontSize: '0.8125rem',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>Status:</span>
                      {getStatusBadge(result.statusCode)}
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '1rem', color: 'var(--text-muted)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                        <Clock style={{ width: 14, height: 14, color: 'var(--primary)' }} />
                        <span>Latency: <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{result.latencyMs} ms</strong></span>
                      </div>

                      {result.rateLimitLimit !== undefined && (
                        <div>
                          <span>Rate Limit: <strong style={{ color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{result.rateLimitRemaining}/{result.rateLimitLimit}</strong></span>
                        </div>
                      )}

                      {result.requestId && (
                        <div style={{ fontFamily: 'var(--font-mono)' }} title={result.requestId}>
                          Request ID: <strong style={{ color: 'var(--text-primary)' }}>{result.requestId.slice(0, 8)}...</strong>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Contextual Diagnosis Alert for 401 / 403 / 429 / 5xx */}
                  {result.statusCode === 401 && (
                    <div
                      style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: 'var(--warning-light)',
                        border: '1px solid var(--warning-border)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--warning-text)',
                        fontSize: '0.8125rem',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '0.375rem',
                      }}
                    >
                      <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                        <AlertTriangle style={{ width: 16, height: 16 }} />
                        Sentinel Authentication Failed (401 Unauthorized)
                      </div>
                      <div>Invalid or missing Sentinel Developer API Key. Ensure an active key is selected.</div>
                    </div>
                  )}

                  {result.statusCode === 403 && (
                    <div
                      style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: 'var(--danger-light)',
                        border: '1px solid var(--danger-border)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--danger-text)',
                        fontSize: '0.8125rem',
                      }}
                    >
                      <strong>Request Blocked by Sentinel Security Policy (403 Forbidden):</strong> Endpoint or HTTP method not permitted.
                    </div>
                  )}

                  {result.statusCode === 429 && (
                    <div
                      style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: 'var(--warning-light)',
                        border: '1px solid var(--warning-border)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--warning-text)',
                        fontSize: '0.8125rem',
                      }}
                    >
                      <strong>Rate Limit Exceeded (429):</strong> Enforced atomically by Sentinel Redis limiter.
                    </div>
                  )}

                  {result.statusCode >= 502 && (
                    <div
                      style={{
                        padding: '0.75rem 1rem',
                        backgroundColor: 'var(--danger-light)',
                        border: '1px solid var(--danger-border)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--danger-text)',
                        fontSize: '0.8125rem',
                      }}
                    >
                      <strong>Upstream Target Unavailable ({result.statusCode}):</strong> Upstream server failed to respond.
                    </div>
                  )}

                  {/* Response Tabs: Response | Headers | Request | cURL */}
                  <div style={{ border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                    <div className="console-tabs-nav">
                      <button
                        type="button"
                        onClick={() => setActiveResponseTab('response')}
                        className={`console-tab-btn ${activeResponseTab === 'response' ? 'active' : ''}`}
                      >
                        <FileText style={{ width: 14, height: 14 }} />
                        <span>Response Body</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setActiveResponseTab('headers')}
                        className={`console-tab-btn ${activeResponseTab === 'headers' ? 'active' : ''}`}
                      >
                        <Layers style={{ width: 14, height: 14 }} />
                        <span>Headers ({result.responseHeaders ? Object.keys(result.responseHeaders).length : 0})</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setActiveResponseTab('request')}
                        className={`console-tab-btn ${activeResponseTab === 'request' ? 'active' : ''}`}
                      >
                        <Terminal style={{ width: 14, height: 14 }} />
                        <span>Request Sent</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setActiveResponseTab('curl')}
                        className={`console-tab-btn ${activeResponseTab === 'curl' ? 'active' : ''}`}
                      >
                        <Code style={{ width: 14, height: 14 }} />
                        <span>cURL</span>
                      </button>
                    </div>

                    <div style={{ padding: '0.875rem', backgroundColor: 'var(--bg-surface)' }}>
                      {activeResponseTab === 'response' && (
                        <pre className="console-code-viewer">
                          {result.responseBody || '<empty response body>'}
                        </pre>
                      )}

                      {activeResponseTab === 'headers' && (
                        <div className="console-code-viewer" style={{ fontSize: '0.75rem' }}>
                          {result.responseHeaders && Object.keys(result.responseHeaders).length > 0 ? (
                            Object.entries(result.responseHeaders).map(([k, v]) => (
                              <div key={k} style={{ display: 'flex', marginBottom: '0.25rem' }}>
                                <span style={{ color: '#93c5fd', fontWeight: 600, marginRight: '0.5rem' }}>{k}:</span>
                                <span style={{ color: '#f8fafc', wordBreak: 'break-all' }}>{v}</span>
                              </div>
                            ))
                          ) : (
                            <div style={{ color: 'var(--text-muted)' }}>No headers returned</div>
                          )}
                        </div>
                      )}

                      {activeResponseTab === 'request' && (
                        <div className="console-code-viewer" style={{ fontSize: '0.75rem' }}>
                          <div style={{ marginBottom: '0.35rem' }}><span style={{ color: '#93c5fd', fontWeight: 600 }}>Method:</span> {method}</div>
                          <div style={{ marginBottom: '0.35rem' }}><span style={{ color: '#93c5fd', fontWeight: 600 }}>Target Path:</span> {getEvaluatedPath()}</div>
                          <div style={{ marginBottom: '0.35rem' }}><span style={{ color: '#93c5fd', fontWeight: 600 }}>Upstream URL:</span> {targetUpstreamUrl}</div>
                          <div style={{ marginBottom: '0.35rem' }}><span style={{ color: '#93c5fd', fontWeight: 600 }}>Gateway URL:</span> {fullGatewayUrl}</div>
                          <div><span style={{ color: '#93c5fd', fontWeight: 600 }}>Trace Request ID:</span> {result.requestId}</div>
                        </div>
                      )}

                      {activeResponseTab === 'curl' && (
                        <pre className="console-code-viewer" style={{ color: '#86efac' }}>
                          {`# Working cURL command for this endpoint:\ncurl -X ${method} "${fullGatewayUrl}" \\\n  -H "X-Sentinel-API-Key: ${selectedKey?.maskedKey || 'YOUR_SENTINEL_API_KEY'}"${
                            headers
                              .filter((h) => h.key.trim() && !h.key.toLowerCase().startsWith('x-sentinel-'))
                              .map((h) => ` \\\n  -H "${h.key.trim()}: ${h.value}"`)
                              .join('')
                          }${
                            bodyType === 'multipart' && selectedFile
                              ? ` \\\n  -F "${fileFieldName}=@${selectedFile.name}"`
                              : bodyType === 'json' && bodyJson.trim() && ['POST', 'PUT', 'PATCH'].includes(method)
                              ? ` \\\n  -H "Content-Type: application/json" \\\n  -d '${bodyJson.trim().replace(/'/g, "'\\''")}'`
                              : ''
                          }`}
                        </pre>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </>
        ) : (
          /* AI Test Mode Content */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* AI Test Plan Overview Card */}
            <div className="console-card">
              <div className="console-card-header">
                <div className="console-section-title">
                  <Sparkles style={{ width: 15, height: 15, color: 'var(--primary)' }} />
                  Autonomous AI Test Plan & Discovery Graph
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={loadAiPlan}
                    disabled={loadingAiPlan}
                    style={{ gap: '0.25rem' }}
                  >
                    <RefreshCw style={{ width: 12, height: 12 }} />
                    Refresh Plan
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={handleExecuteAiTest}
                    disabled={runningAiTest || keys.length === 0}
                    style={{ gap: '0.375rem' }}
                  >
                    <Zap style={{ width: 14, height: 14 }} />
                    {runningAiTest ? 'Executing AI Tests...' : 'Run AI Test Suite'}
                  </button>
                </div>
              </div>

              {loadingAiPlan ? (
                <div style={{ padding: '2rem', textAlign: 'center' }}>
                  <div className="status-dot status-dot-healthy animate-spin" style={{ margin: '0 auto 0.5rem' }} />
                  <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>Discovering endpoints & building dependency graph...</span>
                </div>
              ) : aiPlan ? (
                <div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                    {aiPlan.summary}
                  </div>

                  <table className="ai-step-table">
                    <thead>
                      <tr>
                        <th>Step</th>
                        <th>Method</th>
                        <th>Endpoint</th>
                        <th>Type</th>
                        <th>Dependencies & Mapping</th>
                      </tr>
                    </thead>
                    <tbody>
                      {aiPlan.steps.map((step, idx) => (
                        <tr key={idx}>
                          <td style={{ fontWeight: 600 }}>#{idx + 1}</td>
                          <td>
                            <span className={`pill-badge ${step.method === 'GET' ? 'pill-badge-green' : step.method === 'POST' ? 'pill-badge-blue' : 'pill-badge-amber'}`}>
                              {step.method}
                            </span>
                          </td>
                          <td style={{ fontFamily: 'var(--font-mono)' }}>{step.path}</td>
                          <td>
                            {step.destructive ? (
                              <span className="pill-badge pill-badge-amber">Destructive</span>
                            ) : step.multipart ? (
                              <span className="pill-badge pill-badge-purple">Multipart Upload</span>
                            ) : (
                              <span className="pill-badge pill-badge-gray">Standard</span>
                            )}
                          </td>
                          <td style={{ fontSize: '0.75rem' }}>
                            {Object.keys(step.extractedVariables).length > 0 && (
                              <span style={{ color: '#6b21a8' }}>
                                Produces: {Object.keys(step.extractedVariables).join(', ')}
                              </span>
                            )}
                            {Object.keys(step.parameterMappings).length > 0 && (
                              <span style={{ color: '#1d4ed8', marginLeft: '0.5rem' }}>
                                Requires: {Object.keys(step.parameterMappings).join(', ')}
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </div>

            {/* Destructive Approval Checkpoint */}
            <div style={{ padding: '0.75rem 1rem', backgroundColor: '#fffbeb', border: '1px solid #fde68a', borderRadius: 'var(--radius-md)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', color: '#92400e' }}>
                <AlertTriangle style={{ width: 16, height: 16 }} />
                <span>Destructive safety guardrail is active. Confirmation is required for DELETE or state-altering endpoints.</span>
              </div>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8125rem', fontWeight: 600, color: '#92400e', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={approveDestructive}
                  onChange={(e) => setApproveDestructive(e.target.checked)}
                />
                Approve Destructive Operations
              </label>
            </div>

            {/* AI Test Run Results */}
            {aiReport && (
              <div className="console-card">
                <div className="console-card-header">
                  <div className="console-section-title">
                    <CheckCircle2 style={{ width: 15, height: 15, color: 'var(--success)' }} />
                    AI Execution Report ({aiReport.passedSteps}/{aiReport.totalSteps} Passed)
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleOpenInAssistant}
                    style={{ gap: '0.25rem' }}
                  >
                    <Sparkles style={{ width: 14, height: 14 }} />
                    Open in AI Assistant
                  </button>
                </div>

                <table className="ai-step-table">
                  <thead>
                    <tr>
                      <th>Method</th>
                      <th>Resolved Endpoint</th>
                      <th>Status</th>
                      <th>Latency</th>
                      <th>Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {aiReport.stepResults.map((step, idx) => (
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
                              <CheckCircle2 style={{ width: 12, height: 12 }} /> 200 OK
                            </span>
                          ) : step.requiresApproval ? (
                            <span style={{ color: '#b45309', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                              <AlertTriangle style={{ width: 12, height: 12 }} /> Approval Needed
                            </span>
                          ) : step.blocked ? (
                            <span style={{ color: '#64748b', fontWeight: 600 }}>🚫 Blocked</span>
                          ) : (
                            <span style={{ color: '#b91c1c', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                              <XCircle style={{ width: 12, height: 12 }} /> {step.error || step.status}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {aiReport.rememberedContext && Object.keys(aiReport.rememberedContext).filter((k) => !k.includes('base64')).length > 0 && (
                  <div style={{ marginTop: '0.75rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    <strong>Reused Variables:</strong>{' '}
                    {Object.entries(aiReport.rememberedContext)
                      .filter(([k]) => !k.includes('base64'))
                      .map(([k, v]) => (
                        <span key={k} className="pill-badge pill-badge-purple" style={{ marginLeft: '0.375rem' }}>
                          {k}: {v}
                        </span>
                      ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="test-console-footer">
        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: 'var(--success)' }} />
          <span>Pipeline: <strong style={{ color: 'var(--text-secondary)' }}>Client → Sentinel Gateway → Upstream Target</strong></span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            type="button"
            onClick={onClose}
            className="btn btn-secondary"
          >
            Close
          </button>
          {testMode === 'manual' ? (
            <button
              type="button"
              onClick={() => handleExecute()}
              disabled={loading || keys.length === 0}
              className="btn btn-primary"
              style={{ minWidth: 140, fontWeight: 600 }}
            >
              {loading ? (
                <>
                  <div className="status-dot status-dot-healthy animate-spin" />
                  <span>Sending...</span>
                </>
              ) : (
                <>
                  <Play style={{ width: 16, height: 16 }} />
                  <span>Send Request</span>
                </>
              )}
            </button>
          ) : (
            <button
              type="button"
              onClick={handleExecuteAiTest}
              disabled={runningAiTest || keys.length === 0}
              className="btn btn-primary"
              style={{ minWidth: 160, fontWeight: 600, gap: '0.375rem' }}
            >
              {runningAiTest ? (
                <>
                  <div className="status-dot status-dot-healthy animate-spin" />
                  <span>Running AI Tests...</span>
                </>
              ) : (
                <>
                  <Zap style={{ width: 16, height: 16 }} />
                  <span>Run AI Test Suite</span>
                </>
              )}
            </button>
          )}
        </div>
      </div>

        </div>
      </div>

      {/* Inline Create API Key Modal */}
      <CreateKeyModal
        isOpen={isCreateKeyOpen}
        applicationId={applicationId}
        onClose={() => setIsCreateKeyOpen(false)}
        onSuccess={(newKey) => {
          setIsCreateKeyOpen(false);
          setCreatedRawKey(newKey);
          loadKeys(newKey.id);
        }}
      />

      {/* One-Time Raw Key Secret Display Modal */}
      {createdRawKey && (
        <ApiKeyModal
          isOpen={!!createdRawKey}
          apiKey={createdRawKey}
          onClose={() => {
            setCreatedRawKey(null);
            loadKeys(createdRawKey.id);
          }}
        />
      )}
    </>
  );
};

