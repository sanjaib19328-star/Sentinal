package com.sentinel.api.dto;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class PreparedUpstreamRequest {

    private final URI targetUri;
    private final Map<String, String> headers;

    public PreparedUpstreamRequest(URI targetUri, Map<String, String> headers) {
        this.targetUri = targetUri;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    public URI getTargetUri() {
        return targetUri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
