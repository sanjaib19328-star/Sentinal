package com.sentinel.api.dto;

public class AiTestMissingInputDto {

    private String inputKey;
    private String inputType; // "FILE", "API_KEY", "VARIABLE", "CONFIRMATION", "HEADER"
    private String targetEndpoint;
    private String targetMethod;
    private String prompt;
    private boolean received;
    private String valuePreview;

    public AiTestMissingInputDto() {}

    public AiTestMissingInputDto(String inputKey, String inputType, String targetEndpoint, String targetMethod, String prompt) {
        this.inputKey = inputKey;
        this.inputType = inputType;
        this.targetEndpoint = targetEndpoint;
        this.targetMethod = targetMethod;
        this.prompt = prompt;
        this.received = false;
    }

    public String getInputKey() {
        return inputKey;
    }

    public void setInputKey(String inputKey) {
        this.inputKey = inputKey;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public String getValuePreview() {
        return valuePreview;
    }

    public void setValuePreview(String valuePreview) {
        this.valuePreview = valuePreview;
    }
}
