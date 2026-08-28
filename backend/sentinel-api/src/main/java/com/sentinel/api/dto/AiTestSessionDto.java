package com.sentinel.api.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiTestSessionDto {

    private String sessionId;
    private Long applicationId;
    private String applicationName;
    private String status; // "PLANNING", "WAITING_FOR_INPUT", "READY", "RUNNING", "PASSED", "FAILED", "PARTIAL", "NEEDS_APPROVAL", "CANCELLED"
    private String statusMessage;
    private AiTestPlanDto plan;
    private List<AiTestMissingInputDto> missingInputs = new ArrayList<>();
    private Map<String, String> providedInputs = new HashMap<>();
    private String fileBase64;
    private String fileName;
    private String fileContentType;
    private Long apiKeyId;
    private boolean approveDestructiveOperations;
    private AiTestRunReportDto lastReport;
    private long createdAt;
    private long updatedAt;

    public AiTestSessionDto() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public AiTestPlanDto getPlan() {
        return plan;
    }

    public void setPlan(AiTestPlanDto plan) {
        this.plan = plan;
    }

    public List<AiTestMissingInputDto> getMissingInputs() {
        return missingInputs;
    }

    public void setMissingInputs(List<AiTestMissingInputDto> missingInputs) {
        this.missingInputs = missingInputs;
    }

    public Map<String, String> getProvidedInputs() {
        return providedInputs;
    }

    public void setProvidedInputs(Map<String, String> providedInputs) {
        this.providedInputs = providedInputs;
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

    public AiTestRunReportDto getLastReport() {
        return lastReport;
    }

    public void setLastReport(AiTestRunReportDto lastReport) {
        this.lastReport = lastReport;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
