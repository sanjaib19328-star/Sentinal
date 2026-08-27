package com.sentinel.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String code;
    private String message;
    private String requestId;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String error, String message) {
        this.timestamp = Instant.now();
        this.error = error;
        this.code = error;
        this.message = message;
    }

    public ErrorResponse(Integer status, String error, String message) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.code = error;
        this.message = message;
    }

    public ErrorResponse(Integer status, String error, String code, String message, String requestId) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.code = code != null ? code : error;
        this.message = message;
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        if (this.code == null) {
            this.code = error;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
