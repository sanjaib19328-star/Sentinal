import test from 'node:test';
import assert from 'node:assert/strict';
import { buildSendMessagePayload } from './messagePayload.ts';

test('text-only message contains content and apiKeyId without file fields', () => {
  const result = buildSendMessagePayload('give me the system overview', 10);
  assert.deepEqual(result, {
    content: 'give me the system overview',
    apiKeyId: 10,
  });
  assert.equal('fileBase64' in result, false);
  assert.equal('fileName' in result, false);
  assert.equal('fileContentType' in result, false);
});

test('text-only message with null/empty file does not include file fields', () => {
  const result = buildSendMessagePayload('give me the system overview', 10, null);
  assert.deepEqual(result, {
    content: 'give me the system overview',
    apiKeyId: 10,
  });
  assert.equal('fileBase64' in result, false);
  assert.equal('fileName' in result, false);
  assert.equal('fileContentType' in result, false);
});

test('image message includes fileBase64, fileName, and fileContentType', () => {
  const file = {
    base64: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    name: 'architecture_diagram.png',
    type: 'image/png',
  };
  const result = buildSendMessagePayload('analyze this architecture', 10, file);
  assert.deepEqual(result, {
    content: 'analyze this architecture',
    apiKeyId: 10,
    fileBase64: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    fileName: 'architecture_diagram.png',
    fileContentType: 'image/png',
  });
  assert.equal(result.fileBase64, file.base64);
  assert.equal(result.fileName, file.name);
  assert.equal(result.fileContentType, file.type);
});

test('image message defaults fileContentType to image/png when omitted', () => {
  const file = {
    base64: 'someBase64Data',
    name: 'photo.jpg',
  };
  const result = buildSendMessagePayload('check this photo', 5, file);
  assert.equal(result.fileContentType, 'image/png');
  assert.equal(result.fileName, 'photo.jpg');
  assert.equal(result.fileBase64, 'someBase64Data');
});
