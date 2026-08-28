package com.sentinel.api.dto;

import java.util.ArrayList;
import java.util.List;

public class AiTestPlanDto {

    private String planId;
    private Long applicationId;
    private String applicationName;
    private String title;
    private String summary;
    private int totalEndpointsDiscovered;
    private int totalStepsPlanned;
    private String status = "READY"; // "READY", "WAITING_FOR_INPUT", "REQUIRES_CONFIRMATION"
    private List<AiTestMissingInputDto> missingInputs = new ArrayList<>();
    private List<AiTestStepDto> steps = new ArrayList<>();

    public AiTestPlanDto() {}

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getTotalEndpointsDiscovered() {
        return totalEndpointsDiscovered;
    }

    public void setTotalEndpointsDiscovered(int totalEndpointsDiscovered) {
        this.totalEndpointsDiscovered = totalEndpointsDiscovered;
    }

    public int getTotalStepsPlanned() {
        return totalStepsPlanned;
    }

    public void setTotalStepsPlanned(int totalStepsPlanned) {
        this.totalStepsPlanned = totalStepsPlanned;
    }

    public List<AiTestStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<AiTestStepDto> steps) {
        this.steps = steps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AiTestMissingInputDto> getMissingInputs() {
        return missingInputs;
    }

    public void setMissingInputs(List<AiTestMissingInputDto> missingInputs) {
        this.missingInputs = missingInputs;
    }
}
