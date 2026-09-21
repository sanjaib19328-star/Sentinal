package com.sentinel.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunAiTestRequest {

    private Long applicationId;
    private Long apiKeyId;
    private boolean approveDestructiveOperations;
    private Map<String, String> initialContext = new HashMap<>(); // provided variables e.g. "image_id", "file"

    @JsonProperty("fileBase64")
    @JsonAlias({"file_base64", "file", "binaryBodyBase64", "base64", "image"})
    private String fileBase64;

    @JsonProperty("fileName")
    @JsonAlias({"file_name", "filename", "name"})
    private String fileName;

    @JsonProperty("fileContentType")
    @JsonAlias({"file_content_type", "contentType", "content_type", "mimeType", "mime_type", "type"})
    private String fileContentType;

    private String focusPrompt; // e.g. "Test all APIs in this application" or "Test image workflow"

    public RunAiTestRequest() {}

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public boolean isApproveDestructiveOperations() {
        return approveDestructiveOperations;
    }

    public void setApproveDestructiveOperations(boolean approveDestructiveOperations) {
        this.approveDestructiveOperations = approveDestructiveOperations;
    }

    public Map<String, String> getInitialContext() {
        if (initialContext == null) {
            initialContext = new HashMap<>();
        }
        return initialContext;
    }

    public void setInitialContext(Map<String, String> initialContext) {
        this.initialContext = initialContext;
    }

    public String getFileBase64() {
        if (fileBase64 != null && !fileBase64.isBlank()) {
            return fileBase64;
        }
        if (initialContext != null) {
            String val = initialContext.get("file_base64");
            if (val == null) val = initialContext.get("file");
            if (val == null) val = initialContext.get("binaryBodyBase64");
            if (val == null) val = initialContext.get("base64");
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    public String getFileName() {
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        if (initialContext != null) {
            String val = initialContext.get("file_name");
            if (val == null) val = initialContext.get("filename");
            if (val == null) val = initialContext.get("name");
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContentType() {
        if (fileContentType != null && !fileContentType.isBlank()) {
            return fileContentType;
        }
        if (initialContext != null) {
            String val = initialContext.get("file_content_type");
            if (val == null) val = initialContext.get("contentType");
            if (val == null) val = initialContext.get("content_type");
            if (val == null) val = initialContext.get("mimeType");
            if (val == null) val = initialContext.get("type");
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public String getFocusPrompt() {
        return focusPrompt;
    }

    public void setFocusPrompt(String focusPrompt) {
        this.focusPrompt = focusPrompt;
    }
}

