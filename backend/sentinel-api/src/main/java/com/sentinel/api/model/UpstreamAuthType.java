package com.sentinel.api.model;

public enum UpstreamAuthType {
    NONE,
    API_KEY_HEADER,
    API_KEY_QUERY,
    BEARER_TOKEN,
    BASIC_AUTH,
    CUSTOM_HEADER
}
