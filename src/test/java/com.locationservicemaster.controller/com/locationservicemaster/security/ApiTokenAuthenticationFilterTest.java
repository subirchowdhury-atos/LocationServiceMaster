package com.locationservicemaster.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ApiTokenAuthenticationFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private PrintWriter printWriter;
    
    private ApiTokenAuthenticationFilter filter;
    
    private static final List<String> VALID_TOKENS = Arrays.asList(
        "00000-000-00000",
        "11111-111-11111"
    );
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new ApiTokenAuthenticationFilter(VALID_TOKENS, true);
    }
    
    @Nested
    @DisplayName("Valid API Token")
    class ValidApiToken {
        
        @Test
        @DisplayName("Should authenticate with first valid token")
        void shouldAuthenticateWithFirstValidToken() throws ServletException, IOException {
            // Given
            when(request.getHeader("API-TOKEN")).thenReturn("00000-000-00000");
            when(request.getRequestURI()).thenReturn("/api/v1/address/eligibility_check");
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
        
        @Test
        @DisplayName("Should authenticate with last valid token")
        void shouldAuthenticateWithLastValidToken() throws ServletException, IOException {
            // Given
            when(request.getHeader("API-TOKEN")).thenReturn("11111-111-11111");
            when(request.getRequestURI()).thenReturn("/api/v1/address/eligibility_check");
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
        
        @Test
        @DisplayName("Should accept token in HTTP_API_TOKEN header")
        void shouldAcceptHttpApiTokenHeader() throws ServletException, IOException {
            // Given
            when(request.getHeader("API-TOKEN")).thenReturn(null);
            when(request.getHeader("HTTP_API_TOKEN")).thenReturn("00000-000-00000");
            when(request.getRequestURI()).thenReturn("/api/v1/address/eligibility_check");
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(filterChain).doFilter(request, response);
        }
    }
    
    @Nested
    @DisplayName("Invalid API Token")
    class InvalidApiToken {
        
        @Test
        @DisplayName("Should reject invalid token")
        void shouldRejectInvalidToken() throws ServletException, IOException {
            // Given
            when(request.getHeader("API-TOKEN")).thenReturn("XXXXXXXX-1111-XXXX-1111-XXXXXXXXXXXX");
            when(request.getRequestURI()).thenReturn("/api/v1/address/eligibility_check");
            when(response.getWriter()).thenReturn(printWriter);
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(response).setStatus(403); // Should be 403, not 401
            verify(printWriter).write(contains("Invalid API token")); // Add message verification
            verify(filterChain, never()).doFilter(any(), any());
        }
    }
    
    @Nested
    @DisplayName("Missing API Token")
    class MissingApiToken {
        
        @Test
        @DisplayName("Should reject request without token")
        void shouldRejectRequestWithoutToken() throws ServletException, IOException {
            // Given
            when(request.getHeader(anyString())).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/v1/address/eligibility_check");
            when(response.getWriter()).thenReturn(printWriter);
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(response).setStatus(401);
            verify(printWriter).write(contains("Must provide API token"));
            verify(filterChain, never()).doFilter(any(), any());
        }
    }
    
    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {
        
        @Test
        @DisplayName("Should allow health check without token")
        void shouldAllowHealthCheckWithoutToken() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/api/v1/address/health");
            
            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);
            
            // Then
            assertThat(shouldNotFilter).isTrue();
        }
        
        @Test
        @DisplayName("Should allow actuator endpoints without token")
        void shouldAllowActuatorEndpointsWithoutToken() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/api/actuator/health");
            
            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);
            
            // Then
            assertThat(shouldNotFilter).isTrue();
        }
        
        @Test
        @DisplayName("Should allow Swagger UI without token")
        void shouldAllowSwaggerUIWithoutToken() throws ServletException, IOException {
            // Given
            when(request.getRequestURI()).thenReturn("/api/swagger-ui/index.html");
            
            // When
            boolean shouldNotFilter = filter.shouldNotFilter(request);
            
            // Then
            assertThat(shouldNotFilter).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Security Disabled")
    class SecurityDisabled {
        
        @BeforeEach
        void setUp() {
            filter = new ApiTokenAuthenticationFilter(VALID_TOKENS, false);
        }
        
        @Test
        @DisplayName("Should allow requests without token when security is disabled")
        void shouldAllowRequestsWithoutTokenWhenDisabled() throws ServletException, IOException {
            
            // When
            filter.doFilterInternal(request, response, filterChain);
            
            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(401);
        }
    }
}