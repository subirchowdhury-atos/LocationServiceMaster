package com.locationservicemaster.config;

import com.locationservicemaster.security.ApiTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Security configuration for API token authentication
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {
    
    @Value("${api.security.enabled:true}")
    private boolean apiSecurityEnabled;
    
    @Value("#{'${api.security.tokens:00000-000-00000,11111-111-11111}'.split(',')}")
    private List<String> validApiTokens;
    
    @Value("#{'${api.security.public-paths:/api/actuator/**,/api/v1/address/health,/api/swagger-ui/**,/api/v3/api-docs/**}'.split(',')}")
    private List<String> publicPaths;
    
    @Bean
    public ApiTokenAuthenticationFilter apiTokenAuthenticationFilter() {
        return new ApiTokenAuthenticationFilter(validApiTokens, apiSecurityEnabled);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, 
                                          ApiTokenAuthenticationFilter apiTokenAuthenticationFilter) throws Exception {
        if (apiSecurityEnabled) {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(apiTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            // Disable security for development/testing
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll()
                );
        }
        
        return http.build();
    }
}