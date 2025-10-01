package com.locationservicemaster.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locationservicemaster.dto.AddressEligibilityRequest;
import com.locationservicemaster.service.AddressCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AddressEligibilityControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @MockBean
    private AddressCacheService addressCacheService;
    
    @BeforeEach
    void setUp() {
        // Mock cache to return empty (cache miss)
        when(addressCacheService.getCachedEligibility(anyString()))
            .thenReturn(Optional.empty());
    }
    
    private static final String VALID_API_TOKEN = "00000-000-00000";
    private static final String INVALID_API_TOKEN = "invalid";
    private static final String API_ENDPOINT = "/api/v1/address/eligibility_check";
    
    @Nested
    @DisplayName("Address Eligibility Checks")
    class EligibilityChecks {
        
        @Test
        @DisplayName("Should return 'address missing' when address is empty")
        void shouldReturnAddressMissingResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("")
                .city("")
                .state("")
                .zipCode("")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("API-TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("required")));
        }
        
        @Test
        @DisplayName("Should return eligible response for valid address")
        void shouldReturnAddressEligibleResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("212 encounter bay")
                .city("Alameda")
                .state("California")
                .zipCode("90255")
                .country("USA")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("API-TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eligible").exists())
                    .andExpect(jsonPath("$.address.city").value("Alameda"))
                    .andExpect(jsonPath("$.address.state").value("California"))
                    .andExpect(jsonPath("$.address.zip_code").value("90255"));
        }
        
        @Test
        @DisplayName("Should return not eligible response for non-eligible address")
        void shouldReturnAddressNotEligibleResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("123 Test")
                .city("Hunting Park")
                .state("PA")
                .zipCode("19140")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("API-TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eligible").value(false))
                    .andExpect(jsonPath("$.reason").exists());
        }
        
        @Test
        @DisplayName("Should handle unknown addresses gracefully")
        void shouldReturnAddressNotFoundResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("fake address")
                .city("Unknown City")
                .state("XX")
                .zipCode("00000")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("API-TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eligible").value(false))
                    .andExpect(jsonPath("$.reason").value(containsString("not in any eligible")));
        }
    }
    
    @Nested
    @DisplayName("API Token Authentication")
    class ApiTokenAuthentication {
        
        @Test
        @DisplayName("Should reject request with invalid API token")
        void shouldReturnInvalidTokenResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("212 encounter bay")
                .city("Alameda")
                .state("CA")
                .zipCode("90255")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("API-TOKEN", INVALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid API token"));
        }
        
        @Test
        @DisplayName("Should reject request without API token")
        void shouldReturnMissingTokenResponse() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("212 encounter bay")
                .city("Alameda")
                .state("CA")
                .zipCode("90255")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Must provide API token"));
        }
        
        @Test
        @DisplayName("Should accept request with valid API token in HTTP_API_TOKEN header")
        void shouldAcceptHttpApiTokenHeader() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                .streetAddress("212 encounter bay")
                .city("Alameda")
                .state("CA")
                .zipCode("90255")
                .build();
            
            mockMvc.perform(post(API_ENDPOINT)
                    .header("HTTP_API_TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }
    
    @Nested
    @DisplayName("Post Endpoint Tests")
    class PostEndpointTests {
        
        @Test
        @DisplayName("Should work with POST request and structured request body")
        void shouldWorkWithPostRequest() throws Exception {
            AddressEligibilityRequest request = AddressEligibilityRequest.builder()
                    .streetAddress("212 encounter bay")
                    .city("Alameda")
                    .state("CA")
                    .zipCode("90255")
                    .build();
            
            mockMvc.perform(post("/api/v1/address/check")
                    .header("API-TOKEN", VALID_API_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address").exists());
        }
    }
}
