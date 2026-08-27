package com.sentinel.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

@Component
public class SsrfProtectionValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionValidator.class);

    private final boolean defaultAllowLocal;

    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "localhost",
        "127.0.0.1",
        "::1",
        "0.0.0.0",
        "internal",
        "local",
        "lan",
        "corp"
    );

    public SsrfProtectionValidator(@Value("${sentinel.security.ssrf.allow-local:false}") boolean defaultAllowLocal) {
        this.defaultAllowLocal = defaultAllowLocal;
    }

    public void validateUrl(String urlString) {
        validateUrl(urlString, defaultAllowLocal);
    }

    public void validateUrl(String urlString, boolean allowLocal) {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SecurityException("SSRF_BLOCKED: Only HTTP and HTTPS schemes are supported");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is missing");
        }

        if (allowLocal) {
            return; // Permitted in local dev / test environments
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_DOMAINS.contains(lowerHost) || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal") || lowerHost.endsWith(".lan")) {
            log.warn("SSRF blocked attempt to connect to internal domain: {}", lowerHost);
            throw new SecurityException("SSRF_BLOCKED: Internal and loopback hostnames are not allowed");
        }

        // Resolve and inspect IP addresses
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrLocalAddress(addr)) {
                    log.warn("SSRF blocked attempt to resolve host {} to private/local IP {}", host, addr.getHostAddress());
                    throw new SecurityException("SSRF_BLOCKED: Host resolves to a private or loopback IP (" + addr.getHostAddress() + ")");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    private boolean isPrivateOrLocalAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            return true;
        }

        byte[] raw = addr.getAddress();
        if (raw.length == 4) { // IPv4
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12 (172.16 - 172.31)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16 (Link-local)
            if (b0 == 169 && b1 == 254) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
            // 0.0.0.0/8
            if (b0 == 0) return true;
        } else if (raw.length == 16) { // IPv6
            // Unique local fc00::/7
            int b0 = raw[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) return true;
            // Link-local fe80::/10
            if (b0 == 0xFE && (raw[1] & 0xC0) == 0x80) return true;
        }

        return false;
    }
}
