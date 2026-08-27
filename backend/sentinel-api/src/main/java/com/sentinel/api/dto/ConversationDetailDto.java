package com.sentinel.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ConversationDetailDto {

    private Long id;
    private Long userId;
    private Long applicationId;
    private String applicationName;
    private String applicationBaseUrl;
    private String title;
    private String metadataJson;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ConversationMessageDto> messages = new ArrayList<>();

    public ConversationDetailDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getApplicationBaseUrl() {
        return applicationBaseUrl;
    }

    public void setApplicationBaseUrl(String applicationBaseUrl) {
        this.applicationBaseUrl = applicationBaseUrl;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ConversationMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<ConversationMessageDto> messages) {
        this.messages = messages;
    }
}
