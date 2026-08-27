package com.sentinel.api.dto;

import java.util.List;

public class OpenApiImportResponse {

    private int endpointsImported;
    private int endpointsUpdated;
    private int totalDocumentedEndpoints;
    private int schemasCount;
    private int parametersCount;
    private int requestBodiesCount;
    private int transitionsCount;
    private List<ApiEndpointDto> endpoints;

    public OpenApiImportResponse() {}

    public OpenApiImportResponse(
        int endpointsImported,
        int endpointsUpdated,
        int totalDocumentedEndpoints,
        int schemasCount,
        int parametersCount,
        int requestBodiesCount,
        int transitionsCount,
        List<ApiEndpointDto> endpoints
    ) {
        this.endpointsImported = endpointsImported;
        this.endpointsUpdated = endpointsUpdated;
        this.totalDocumentedEndpoints = totalDocumentedEndpoints;
        this.schemasCount = schemasCount;
        this.parametersCount = parametersCount;
        this.requestBodiesCount = requestBodiesCount;
        this.transitionsCount = transitionsCount;
        this.endpoints = endpoints;
    }

    public int getEndpointsImported() {
        return endpointsImported;
    }

    public void setEndpointsImported(int endpointsImported) {
        this.endpointsImported = endpointsImported;
    }

    public int getEndpointsUpdated() {
        return endpointsUpdated;
    }

    public void setEndpointsUpdated(int endpointsUpdated) {
        this.endpointsUpdated = endpointsUpdated;
    }

    public int getTotalDocumentedEndpoints() {
        return totalDocumentedEndpoints;
    }

    public void setTotalDocumentedEndpoints(int totalDocumentedEndpoints) {
        this.totalDocumentedEndpoints = totalDocumentedEndpoints;
    }

    public int getSchemasCount() {
        return schemasCount;
    }

    public void setSchemasCount(int schemasCount) {
        this.schemasCount = schemasCount;
    }

    public int getParametersCount() {
        return parametersCount;
    }

    public void setParametersCount(int parametersCount) {
        this.parametersCount = parametersCount;
    }

    public int getRequestBodiesCount() {
        return requestBodiesCount;
    }

    public void setRequestBodiesCount(int requestBodiesCount) {
        this.requestBodiesCount = requestBodiesCount;
    }

    public int getTransitionsCount() {
        return transitionsCount;
    }

    public void setTransitionsCount(int transitionsCount) {
        this.transitionsCount = transitionsCount;
    }

    public List<ApiEndpointDto> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ApiEndpointDto> endpoints) {
        this.endpoints = endpoints;
    }
}
