package com.locationservicemaster.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for property eligibility checks
 * Now uses RegionEligibilityService for YAML-based region lookups
 */
@Service
@Slf4j
public class PropertyEligibilityService {
    
    @Autowired
    private RegionEligibilityService regionEligibilityService;
    
    private static final String MESSAGE_ELIGIBLE = "address_eligible";
    private static final String MESSAGE_NOT_ELIGIBLE = "address not eligible";
    private static final String MESSAGE_NOT_FOUND = "address not found";
    
    /**
     * Check if a property/address is eligible based on formatted address
     * @param formattedAddress Map containing address components
     * @return Map containing eligibility result and formatted address
     */
    public Map<String, Object> checkEligibility(Map<String, String> formattedAddress) {
        Map<String, Object> result = new HashMap<>();
        
        if (formattedAddress == null || formattedAddress.isEmpty()) {
            log.debug("Address not found - null or empty formatted address");
            result.put("message", MESSAGE_NOT_FOUND);
            return result;
        }
        
        // Extract address components
        String city = formattedAddress.get("city");
        String county = formattedAddress.get("county");
        String state = formattedAddress.get("state");
        
        // Check if address is eligible using the region configuration
        boolean isEligible = regionEligibilityService.isAddressEligible(city, county, state);
        
        if (isEligible) {
            log.debug("Address is eligible: {}", formattedAddress);
            result.put("message", MESSAGE_ELIGIBLE);
            result.put("formatted_address", formattedAddress);
        } else {
            log.debug("Address is not eligible: {}", formattedAddress);
            result.put("message", MESSAGE_NOT_ELIGIBLE);
        }
        
        return result;
    }
    
    /**
     * Check eligibility with detailed reason
     */
    public Map<String, Object> checkEligibilityWithReason(Map<String, String> formattedAddress) {
        Map<String, Object> result = new HashMap<>();
        
        if (formattedAddress == null || formattedAddress.isEmpty()) {
            result.put("message", MESSAGE_NOT_FOUND);
            result.put("reason", "Address information is missing or incomplete");
            return result;
        }
        
        String city = formattedAddress.get("city");
        String county = formattedAddress.get("county");
        String state = formattedAddress.get("state");
        
        RegionEligibilityService.EligibilityCheckResult checkResult = 
            regionEligibilityService.checkEligibilityWithReason(city, county, state);
        
        if (checkResult.isEligible()) {
            result.put("message", MESSAGE_ELIGIBLE);
            result.put("formatted_address", formattedAddress);
        } else {
            result.put("message", MESSAGE_NOT_ELIGIBLE);
        }
        
        result.put("reason", checkResult.getReason());
        
        return result;
    }
}