package com.sentinel.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "api_endpoints",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_method_path", columnNames = {"application_id", "method", "normalized_path"})
    },
    indexes = {
        @Index(name = "idx_api_endpoints_app_id", columnList = "application_id")
    }
)
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(name = "normalized_path", nullable = false, length = 255)
    private String normalizedPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "documentation_status", nullable = false, length = 40)
    private DocumentationStatus documentationStatus = DocumentationStatus.DISCOVERED;

    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "parameters_json", columnDefinition = "LONGTEXT")
    private String parametersJson;

    @Column(name = "request_body_schema_json", columnDefinition = "LONGTEXT")
    private String requestBodySchemaJson;

    @Column(name = "responses_json", columnDefinition = "LONGTEXT")
    private String responsesJson;

    @Column(name = "is_deprecated", nullable = false)
    private boolean deprecated = false;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public ApiEndpoint() {
    }

    public ApiEndpoint(Long applicationId, String method, String normalizedPath) {
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.documentationStatus = DocumentationStatus.DISCOVERED;
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    public ApiEndpoint(Long applicationId, String method, String normalizedPath, DocumentationStatus status) {
        this.applicationId = applicationId;
        this.method = method;
        this.normalizedPath = normalizedPath;
        this.documentationStatus = status != null ? status : DocumentationStatus.DISCOVERED;
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.firstSeenAt == null) {
            this.firstSeenAt = now;
        }
        if (this.lastSeenAt == null) {
            this.lastSeenAt = now;
        }
        if (this.documentationStatus == null) {
            this.documentationStatus = DocumentationStatus.DISCOVERED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastSeenAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
    }

    public DocumentationStatus getDocumentationStatus() {
        return documentationStatus;
    }

    public void setDocumentationStatus(DocumentationStatus documentationStatus) {
        this.documentationStatus = documentationStatus;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public String getRequestBodySchemaJson() {
        return requestBodySchemaJson;
    }

    public void setRequestBodySchemaJson(String requestBodySchemaJson) {
        this.requestBodySchemaJson = requestBodySchemaJson;
    }

    public String getResponsesJson() {
        return responsesJson;
    }

    public void setResponsesJson(String responsesJson) {
        this.responsesJson = responsesJson;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
