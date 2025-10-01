package com.locationservicemaster.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for address eligibility check results.
 * Contains eligibility status, reasons, and additional metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressEligibilityResponse {
    
    /**
     * Whether the address is eligible for service
     */
    @JsonProperty("eligible")
    private Boolean eligible;
    
    /**
     * Reason for eligibility decision
     */
    @JsonProperty("reason")
    private String reason;
    
    /**
     * Detailed address information
     */
    @JsonProperty("address")
    private AddressDetails address;
    
    /**
     * List of zone names that matched the address
     */
    @JsonProperty("matched_zones")
    private List<String> matchedZones;
    
    /**
     * Confidence score of the eligibility decision (0.0 to 1.0)
     */
    @JsonProperty("confidence_score")
    private Double confidenceScore;
    
    /**
     * Timestamp when the check was performed
     */
    @JsonProperty("checked_at")
    private LocalDateTime checkedAt;
    
    /**
     * Whether the result was retrieved from cache
     */
    @JsonProperty("cache_hit")
    private Boolean cacheHit;
    
    /**
     * Processing time in milliseconds
     */
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    /**
     * Nested class for address details in the response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDetails {
        
        @JsonProperty("street_address")
        private String streetAddress;
        
        @JsonProperty("street_address_2")
        private String streetAddress2;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("state")
        private String state;
        
        @JsonProperty("zip_code")
        private String zipCode;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("formatted_address")
        private String formattedAddress;
        
        @JsonProperty("latitude")
        private Double latitude;
        
        @JsonProperty("longitude")
        private Double longitude;
    }
}