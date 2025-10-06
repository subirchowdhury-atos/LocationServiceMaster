package com.locationservicemaster.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Filter for API Token Authentication
 * Validates API tokens from request headers
 */
@Slf4j
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Set<String> validTokens;
    private final boolean securityEnabled;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/v1/address",
        "/api/actuator",
        "/api/swagger-ui",
        "/api/v3/api-docs"
    );

    public ApiTokenAuthenticationFilter(List<String> validTokens, boolean securityEnabled) {
        this.validTokens = new HashSet<>(validTokens);
        this.securityEnabled = securityEnabled;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // If security is disabled, allow all requests
        if (!securityEnabled) {
            log.debug("Security disabled - allowing request");
            setAuthentication();
            filterChain.doFilter(request, response);
            return;
        }

        // Try to get token from headers
        String token = extractToken(request);

        if (token == null || token.trim().isEmpty()) {
            log.warn("Missing API token for request: {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Must provide API token");
            return;
        }

        if (!validTokens.contains(token)) {
            log.warn("Invalid API token attempted for request: {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Invalid API token");
            return;
        }

        // Token is valid - set authentication and continue
        log.debug("Valid API token for request: {}", request.getRequestURI());
        setAuthentication();
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // If security is disabled, don't filter any requests
        if (!securityEnabled) {
            return true;
        }
        
        String path = request.getRequestURI();
        
        // Check if path matches any public endpoint
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Extract API token from request headers
     * Tries API-TOKEN header first, then HTTP_API_TOKEN
     */
    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("API-TOKEN");
        
        if (token == null || token.trim().isEmpty()) {
            token = request.getHeader("HTTP_API_TOKEN");
        }
        
        return token;
    }

    /**
     * Set authentication in SecurityContext
     */
    private void setAuthentication() {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken("api-user", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Send JSON error response
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", String.valueOf(status));
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}