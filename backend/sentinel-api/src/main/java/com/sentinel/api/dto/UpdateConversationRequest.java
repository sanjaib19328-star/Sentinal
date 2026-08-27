package com.sentinel.api.dto;

import jakarta.validation.constraints.Size;

public class UpdateConversationRequest {

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String metadataJson;

    public UpdateConversationRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
