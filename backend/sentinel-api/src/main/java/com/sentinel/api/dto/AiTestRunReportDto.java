package com.sentinel.api.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiTestRunReportDto {

    private String runId;
    private Long applicationId;
    private String applicationName;
    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int blockedSteps;
    private int pendingApprovalSteps;
    private long totalDurationMs;
    private double avgLatencyMs;
    private String overallStatus; // PASSED, FAILED, PARTIAL, NEEDS_APPROVAL
    private String executiveSummary;
    private String failureAnalysis;
    private Map<String, String> rememberedContext = new HashMap<>();
    private List<AiTestStepResultDto> stepResults = new ArrayList<>();

    public AiTestRunReportDto() {}

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
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

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getPassedSteps() {
        return passedSteps;
    }

    public void setPassedSteps(int passedSteps) {
        this.passedSteps = passedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public int getBlockedSteps() {
        return blockedSteps;
    }

    public void setBlockedSteps(int blockedSteps) {
        this.blockedSteps = blockedSteps;
    }

    public int getPendingApprovalSteps() {
        return pendingApprovalSteps;
    }

    public void setPendingApprovalSteps(int pendingApprovalSteps) {
        this.pendingApprovalSteps = pendingApprovalSteps;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public String getFailureAnalysis() {
        return failureAnalysis;
    }

    public void setFailureAnalysis(String failureAnalysis) {
        this.failureAnalysis = failureAnalysis;
    }

    public Map<String, String> getRememberedContext() {
        return rememberedContext;
    }

    public void setRememberedContext(Map<String, String> rememberedContext) {
        this.rememberedContext = rememberedContext;
    }

    public List<AiTestStepResultDto> getStepResults() {
        return stepResults;
    }

    public void setStepResults(List<AiTestStepResultDto> stepResults) {
        this.stepResults = stepResults;
    }
}
