package com.sentinel.api.dto;

public class OpenApiImportRequest {

    private String specContent;
    private String specUrl;

    public OpenApiImportRequest() {}

    public OpenApiImportRequest(String specContent, String specUrl) {
        this.specContent = specContent;
        this.specUrl = specUrl;
    }

    public String getSpecContent() {
        return specContent;
    }

    public void setSpecContent(String specContent) {
        this.specContent = specContent;
    }

    public String getSpecUrl() {
        return specUrl;
    }

    public void setSpecUrl(String specUrl) {
        this.specUrl = specUrl;
    }
}
