package com.sentinel.api.dto;

import com.sentinel.api.model.AuditAction;

import java.time.Instant;

public class AuditLogDto {

    private Long id;
    private Long userId;
    private Long applicationId;
    private AuditAction action;
    private String resourceType;
    private String resourceId;
    private String metadata;
    private String ipAddress;
    private Instant createdAt;

    public AuditLogDto() {}

    public AuditLogDto(Long id, Long userId, Long applicationId, AuditAction action, String resourceType, String resourceId, String metadata, String ipAddress, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.applicationId = applicationId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

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

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
