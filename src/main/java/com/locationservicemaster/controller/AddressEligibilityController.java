package com.locationservicemaster.controller;

import com.locationservicemaster.dto.AddressEligibilityRequest;
import com.locationservicemaster.dto.AddressEligibilityResponse;
import com.locationservicemaster.service.AddressEligibilityService;
import com.locationservicemaster.service.AddressLookupService;
import com.locationservicemaster.service.PropertyEligibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Address Eligibility
 * Matches Ruby app/controllers/api/v1/address_eligibilities_controller.rb
 */
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
@Slf4j
public class AddressEligibilityController {
    
    @Autowired
    private AddressLookupService addressLookupService;
    @Autowired
    private PropertyEligibilityService propertyEligibilityService;
    @Autowired
    private AddressEligibilityService addressEligibilityService;
    
    /**
     * Check if an address is eligible
     * POST /api/v1/address/eligibility_check
     * 
     * @param request Map containing "address" parameter (Ruby format)
     * @return Simple response matching Ruby format
     */
    @PostMapping("/eligibility_check")
    public ResponseEntity<Map<String, Object>> eligibilityCheck(@RequestBody Map<String, String> request) {
        String address = request.get("address");
        
        log.info("Checking eligibility for address: {}", address);
        
        // Check for missing address
        if (address == null || address.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "address missing");
            return ResponseEntity.ok(response);
        }
        
        // Create AddressEligibilityRequest from the simple address string
        AddressEligibilityRequest eligibilityRequest = AddressEligibilityRequest.builder()
                .streetAddress(address)
                .city("") // YAML lookup doesn't need these
                .state("")
                .zipCode("00000") // Dummy value to pass validation
                .build();
        
        // Use AddressEligibilityService (with YAML integration)
        AddressEligibilityResponse eligibilityResponse = addressEligibilityService.checkEligibility(eligibilityRequest);
        
        Map<String, Object> response = new HashMap<>();
        
        if (eligibilityResponse.getEligible() != null && eligibilityResponse.getEligible()) {
            response.put("message", "address_eligible");
            
            // Build formatted address from response
            Map<String, String> formattedAddress = new HashMap<>();
            formattedAddress.put("street", eligibilityResponse.getAddress().getStreetAddress());
            formattedAddress.put("city", eligibilityResponse.getAddress().getCity());
            formattedAddress.put("state", eligibilityResponse.getAddress().getState());
            formattedAddress.put("zip", eligibilityResponse.getAddress().getZipCode());
            formattedAddress.put("county", eligibilityResponse.getMatchedZones().isEmpty() ? "" : eligibilityResponse.getMatchedZones().get(0));
            formattedAddress.put("country", eligibilityResponse.getAddress().getCountry());
            
            response.put("formatted_address", formattedAddress);
        } else {
            response.put("message", "address not eligible");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check eligibility using structured DTO (new endpoint using AddressEligibilityService)
     * POST /api/v1/address/check
     * 
     * @param request Structured address eligibility request
     * @return Detailed eligibility response
     */
    @PostMapping("/check")
    public ResponseEntity<AddressEligibilityResponse> checkEligibility(
            @Valid @RequestBody AddressEligibilityRequest request) {
        
        log.info("Checking eligibility for structured request: {}, {}, {} {}", 
                request.getStreetAddress(), request.getCity(), request.getState(), request.getZipCode());
        
        // Use the AddressEligibilityService to check eligibility
        AddressEligibilityResponse response = addressEligibilityService.checkEligibility(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Alternative endpoint with request parameter (for backward compatibility)
     */
    @PostMapping("/eligible")
    public ResponseEntity<Map<String, Object>> eligibleAlternative(
            @RequestParam(value = "address", required = false) String address) {
        Map<String, String> request = new HashMap<>();
        request.put("address", address);
        return eligibilityCheck(request);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}