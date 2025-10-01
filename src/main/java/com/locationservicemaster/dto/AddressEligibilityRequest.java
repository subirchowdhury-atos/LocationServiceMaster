package com.locationservicemaster.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressEligibilityRequest {
    
    @NotBlank(message = "Street address is required")
    @JsonProperty("street_address")
    private String streetAddress;
    
    @JsonProperty("street_address_2")
    private String streetAddress2;
    
    @NotBlank(message = "City is required")
    @JsonProperty("city")
    private String city;
    
    @NotBlank(message = "State is required")
    @JsonProperty("state")
    private String state;
    
    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Invalid zip code format")
    @JsonProperty("zip_code")
    private String zipCode;
    
    @JsonProperty("country")
    @Builder.Default
    private String country = "USA";
    
    @JsonProperty("latitude")
    private Double latitude;
    
    @JsonProperty("longitude")
    private Double longitude;
    
    @JsonProperty("check_coordinates")
    @Builder.Default
    private Boolean checkCoordinates = false;
    
    @JsonProperty("include_reason")
    @Builder.Default
    private Boolean includeReason = true;
}