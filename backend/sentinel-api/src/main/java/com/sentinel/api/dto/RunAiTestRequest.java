package com.sentinel.api.dto;

import java.util.HashMap;
import java.util.Map;

public class RunAiTestRequest {

    private Long applicationId;
    private Long apiKeyId;
    private boolean approveDestructiveOperations;
    private Map<String, String> initialContext = new HashMap<>(); // provided variables e.g. "image_id", "file"
    private String fileBase64;
    private String fileName;
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
        return initialContext;
    }

    public void setInitialContext(Map<String, String> initialContext) {
        this.initialContext = initialContext;
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

    public String getFocusPrompt() {
        return focusPrompt;
    }

    public void setFocusPrompt(String focusPrompt) {
        this.focusPrompt = focusPrompt;
    }
}
