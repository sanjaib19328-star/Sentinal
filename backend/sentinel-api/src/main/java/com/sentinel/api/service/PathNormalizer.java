package com.sentinel.api.service;

import java.util.regex.Pattern;

public final class PathNormalizer {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern HEX_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{16,64}$");
    private static final Pattern PREFIXED_ID_PATTERN = Pattern.compile("^[a-zA-Z]{2,6}_[a-zA-Z0-9]{6,}$");
    private static final Pattern HYPHENATED_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+-[a-zA-Z0-9-]+$");

    private PathNormalizer() {
    }

    public static String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || rawPath.equals("/")) {
            return "/";
        }

        String cleaned = rawPath.trim();
        // Remove query parameters if present
        int queryIndex = cleaned.indexOf('?');
        if (queryIndex != -1) {
            cleaned = cleaned.substring(0, queryIndex);
        }

        // Ensure leading slash
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }

        // Split segments
        String[] segments = cleaned.split("/");
        StringBuilder normalized = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }

            normalized.append("/");
            if (isDynamicSegment(segment)) {
                normalized.append("{id}");
            } else {
                normalized.append(segment);
            }
        }

        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    private static boolean isDynamicSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }
        String base = segment;
        int dotIdx = segment.lastIndexOf('.');
        if (dotIdx > 0) {
            base = segment.substring(0, dotIdx);
        }

        if (NUMERIC_PATTERN.matcher(base).matches()) {
            return true;
        }
        if (UUID_PATTERN.matcher(base).matches()) {
            return true;
        }
        if (HEX_ID_PATTERN.matcher(base).matches()) {
            return true;
        }
        if (PREFIXED_ID_PATTERN.matcher(base).matches()) {
            return true;
        }
        if (HYPHENATED_ID_PATTERN.matcher(base).matches()) {
            return true;
        }
        return false;
    }
}
