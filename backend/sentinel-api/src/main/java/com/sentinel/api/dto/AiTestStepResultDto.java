package com.sentinel.api.dto;

import java.util.HashMap;
import java.util.Map;

public class AiTestStepResultDto {

    private String stepId;
    private String name;
    private String method;
    private String endpoint;
    private String resolvedPath;
    private int status;
    private long latencyMs;
    private String requestId;
    private boolean passed;
    private boolean skipped;
    private boolean blocked;
    private boolean requiresApproval;
    private String error;
    private String responseSummary;
    private Map<String, String> inputsUsed = new HashMap<>();
    private Map<String, String> outputsExtracted = new HashMap<>();

    public AiTestStepResultDto() {}

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getResolvedPath() {
        return resolvedPath;
    }

    public void setResolvedPath(String resolvedPath) {
        this.resolvedPath = resolvedPath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public Map<String, String> getInputsUsed() {
        return inputsUsed;
    }

    public void setInputsUsed(Map<String, String> inputsUsed) {
        this.inputsUsed = inputsUsed;
    }

    public Map<String, String> getOutputsExtracted() {
        return outputsExtracted;
    }

    public void setOutputsExtracted(Map<String, String> outputsExtracted) {
        this.outputsExtracted = outputsExtracted;
    }
}
