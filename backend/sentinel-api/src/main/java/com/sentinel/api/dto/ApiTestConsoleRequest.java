package com.sentinel.api.dto;

import java.util.Map;

public class ApiTestConsoleRequest {

    private Long apiKeyId;
    private String method;
    private String path;
    private Map<String, String> queryParams;
    private Map<String, String> headers;
    private String body;
    private String binaryBodyBase64;
    private String fileName;
    private String fileFieldName;
    private String fileContentType;

    public ApiTestConsoleRequest() {}

    public ApiTestConsoleRequest(Long apiKeyId, String method, String path, Map<String, String> queryParams, Map<String, String> headers, String body) {
        this.apiKeyId = apiKeyId;
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
    }

    public ApiTestConsoleRequest(Long apiKeyId, String method, String path, Map<String, String> queryParams, Map<String, String> headers, String body, String binaryBodyBase64) {
        this.apiKeyId = apiKeyId;
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
        this.binaryBodyBase64 = binaryBodyBase64;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
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

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBinaryBodyBase64() {
        return binaryBodyBase64;
    }

    public void setBinaryBodyBase64(String binaryBodyBase64) {
        this.binaryBodyBase64 = binaryBodyBase64;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileFieldName() {
        return fileFieldName;
    }

    public void setFileFieldName(String fileFieldName) {
        this.fileFieldName = fileFieldName;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }
}
