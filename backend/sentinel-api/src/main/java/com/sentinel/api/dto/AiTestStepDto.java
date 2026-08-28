package com.sentinel.api.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiTestStepDto {

    private String stepId;
    private String name;
    private String method;
    private String path;
    private String description;
    private boolean destructive;
    private boolean requiresApproval;
    private List<String> dependsOnStepIds = new ArrayList<>();
    private List<String> requiredVariables = new ArrayList<>();
    private List<String> producedVariables = new ArrayList<>();
    private Map<String, String> extractedVariables = new HashMap<>(); // e.g. "image_id" -> "response.body.image_id"
    private Map<String, String> parameterMappings = new HashMap<>(); // e.g. "image_id" -> "{image_id}"
    private String requestBodyTemplate;
    private String requestContentType;
    private boolean isMultipart;
    private String multipartFieldName;
    private boolean requiresInput;
    private String missingInputType;
    private String missingInputPrompt;
    private int level;

    public AiTestStepDto() {}

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDestructive() {
        return destructive;
    }

    public void setDestructive(boolean destructive) {
        this.destructive = destructive;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public List<String> getDependsOnStepIds() {
        return dependsOnStepIds;
    }

    public void setDependsOnStepIds(List<String> dependsOnStepIds) {
        this.dependsOnStepIds = dependsOnStepIds;
    }

    public Map<String, String> getExtractedVariables() {
        return extractedVariables;
    }

    public void setExtractedVariables(Map<String, String> extractedVariables) {
        this.extractedVariables = extractedVariables;
    }

    public Map<String, String> getParameterMappings() {
        return parameterMappings;
    }

    public void setParameterMappings(Map<String, String> parameterMappings) {
        this.parameterMappings = parameterMappings;
    }

    public String getRequestBodyTemplate() {
        return requestBodyTemplate;
    }

    public void setRequestBodyTemplate(String requestBodyTemplate) {
        this.requestBodyTemplate = requestBodyTemplate;
    }

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(String requestContentType) {
        this.requestContentType = requestContentType;
    }

    public boolean isMultipart() {
        return isMultipart;
    }

    public void setMultipart(boolean multipart) {
        isMultipart = multipart;
    }

    public String getMultipartFieldName() {
        return multipartFieldName;
    }

    public void setMultipartFieldName(String multipartFieldName) {
        this.multipartFieldName = multipartFieldName;
    }

    public List<String> getRequiredVariables() {
        return requiredVariables;
    }

    public void setRequiredVariables(List<String> requiredVariables) {
        this.requiredVariables = requiredVariables;
    }

    public List<String> getProducedVariables() {
        return producedVariables;
    }

    public void setProducedVariables(List<String> producedVariables) {
        this.producedVariables = producedVariables;
    }

    public boolean isRequiresInput() {
        return requiresInput;
    }

    public void setRequiresInput(boolean requiresInput) {
        this.requiresInput = requiresInput;
    }

    public String getMissingInputType() {
        return missingInputType;
    }

    public void setMissingInputType(String missingInputType) {
        this.missingInputType = missingInputType;
    }

    public String getMissingInputPrompt() {
        return missingInputPrompt;
    }

    public void setMissingInputPrompt(String missingInputPrompt) {
        this.missingInputPrompt = missingInputPrompt;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
