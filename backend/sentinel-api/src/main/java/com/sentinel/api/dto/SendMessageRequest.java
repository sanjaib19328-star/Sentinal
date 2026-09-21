package com.sentinel.api.dto;

public class SendMessageRequest {

    private String content;

    private Long apiKeyId;

    private String metadataJson;

    private boolean triggerAiTesting;

    // Uploaded file base64 data for multimodal vision and AI test input memory
    private String fileBase64;
    private String fileName;
    private String fileContentType;

    public SendMessageRequest() {}

    public boolean hasImage() {
        return fileBase64 != null && !fileBase64.isBlank();
    }

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    public String getEffectiveContent() {
        if (hasContent()) {
            return content.trim();
        }
        if (hasImage()) {
            return "Please analyze this image.";
        }
        return "";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public boolean isTriggerAiTesting() {
        return triggerAiTesting;
    }

    public void setTriggerAiTesting(boolean triggerAiTesting) {
        this.triggerAiTesting = triggerAiTesting;
    }

    public String getFileBase64() {
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }
}
