package com.sentinel.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.ErrorResponse;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Sentinel-API-Key";
    public static final String API_KEY_ATTRIBUTE = "SENTINEL_API_KEY";

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/gateway");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            rawKey = request.getHeader("X-API-Key");
        }

        if (rawKey == null || rawKey.isBlank()) {
            sendUnauthorizedResponse(response);
            return;
        }

        Optional<ApiKey> apiKeyOpt = apiKeyService.validateKey(rawKey.trim());
        if (apiKeyOpt.isEmpty()) {
            sendUnauthorizedResponse(response);
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();
        request.setAttribute(API_KEY_ATTRIBUTE, apiKey);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            apiKey,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "Valid Sentinel API key required");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
