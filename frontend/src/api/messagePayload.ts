import type { SendMessageRequest } from '../types/conversation';

export function buildSendMessagePayload(
  content: string,
  apiKeyId?: number | null,
  file?: { base64: string; name: string; type?: string } | null
): SendMessageRequest {
  const payload: SendMessageRequest = {
    content,
    apiKeyId,
  };

  if (file && file.base64) {
    payload.fileBase64 = file.base64;
    payload.fileName = file.name;
    payload.fileContentType = file.type || 'image/png';
  }

  return payload;
}
