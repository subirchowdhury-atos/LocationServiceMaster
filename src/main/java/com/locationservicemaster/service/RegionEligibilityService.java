package com.locationservicemaster.service;

import com.locationservicemaster.config.EligibleRegionsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for checking address eligibility against configured regions
 */
@Service
@Slf4j
public class RegionEligibilityService {
    
    @Autowired
    private EligibleRegionsConfig regionsConfig;
    
    // State abbreviation mapping - comprehensive list
    private static final Map<String, String> STATE_ABBREVIATIONS = new HashMap<>();
    
    static {
        // States with eligible regions
        STATE_ABBREVIATIONS.put("CA", "california");
        STATE_ABBREVIATIONS.put("FL", "florida");
        
        // Other states (for test data and future expansion)
        STATE_ABBREVIATIONS.put("NY", "new york");
        STATE_ABBREVIATIONS.put("PA", "pennsylvania");
        STATE_ABBREVIATIONS.put("TX", "texas");
        STATE_ABBREVIATIONS.put("IL", "illinois");
        STATE_ABBREVIATIONS.put("OH", "ohio");
        STATE_ABBREVIATIONS.put("MI", "michigan");
        STATE_ABBREVIATIONS.put("WA", "washington");
        STATE_ABBREVIATIONS.put("OR", "oregon");
        STATE_ABBREVIATIONS.put("AZ", "arizona");
        STATE_ABBREVIATIONS.put("NV", "nevada");
        STATE_ABBREVIATIONS.put("CO", "colorado");
        STATE_ABBREVIATIONS.put("MA", "massachusetts");
        STATE_ABBREVIATIONS.put("VA", "virginia");
        STATE_ABBREVIATIONS.put("NC", "north carolina");
        STATE_ABBREVIATIONS.put("SC", "south carolina");
        STATE_ABBREVIATIONS.put("GA", "georgia");
        STATE_ABBREVIATIONS.put("TN", "tennessee");
        STATE_ABBREVIATIONS.put("AL", "alabama");
        STATE_ABBREVIATIONS.put("MS", "mississippi");
        STATE_ABBREVIATIONS.put("LA", "louisiana");
        STATE_ABBREVIATIONS.put("AR", "arkansas");
        STATE_ABBREVIATIONS.put("OK", "oklahoma");
        STATE_ABBREVIATIONS.put("KS", "kansas");
        STATE_ABBREVIATIONS.put("MO", "missouri");
        STATE_ABBREVIATIONS.put("IA", "iowa");
        STATE_ABBREVIATIONS.put("MN", "minnesota");
        STATE_ABBREVIATIONS.put("WI", "wisconsin");
        STATE_ABBREVIATIONS.put("IN", "indiana");
        STATE_ABBREVIATIONS.put("KY", "kentucky");
        STATE_ABBREVIATIONS.put("WV", "west virginia");
        STATE_ABBREVIATIONS.put("MD", "maryland");
        STATE_ABBREVIATIONS.put("DE", "delaware");
        STATE_ABBREVIATIONS.put("NJ", "new jersey");
        STATE_ABBREVIATIONS.put("CT", "connecticut");
        STATE_ABBREVIATIONS.put("RI", "rhode island");
        STATE_ABBREVIATIONS.put("VT", "vermont");
        STATE_ABBREVIATIONS.put("NH", "new hampshire");
        STATE_ABBREVIATIONS.put("ME", "maine");
        STATE_ABBREVIATIONS.put("ND", "north dakota");
        STATE_ABBREVIATIONS.put("SD", "south dakota");
        STATE_ABBREVIATIONS.put("NE", "nebraska");
        STATE_ABBREVIATIONS.put("MT", "montana");
        STATE_ABBREVIATIONS.put("WY", "wyoming");
        STATE_ABBREVIATIONS.put("ID", "idaho");
        STATE_ABBREVIATIONS.put("UT", "utah");
        STATE_ABBREVIATIONS.put("NM", "new mexico");
        STATE_ABBREVIATIONS.put("AK", "alaska");
        STATE_ABBREVIATIONS.put("HI", "hawaii");
    }
    
    /**
     * Check if an address is eligible based on city, county, and state
     */
    public boolean isAddressEligible(String city, String county, String state) {
        if (city == null || state == null) {
            log.debug("City or state is null, address not eligible");
            return false;
        }
        
        String normalizedState = normalizeState(state);
        
        // Try with county if provided
        if (county != null && !county.trim().isEmpty()) {
            boolean eligible = regionsConfig.isCityEligible(normalizedState, county, city);
            log.debug("Eligibility check with county: city={}, county={}, state={}, eligible={}", 
                city, county, state, eligible);
            return eligible;
        }
        
        // Try without county (search all counties in state)
        boolean eligible = regionsConfig.isCityEligibleInState(normalizedState, city);
        log.debug("Eligibility check without county: city={}, state={}, eligible={}", 
            city, state, eligible);
        return eligible;
    }
    
    /**
     * Check if an address is eligible (without county info)
     */
    public boolean isAddressEligible(String city, String state) {
        return isAddressEligible(city, null, state);
    }
    
    /**
     * Get all eligible cities in a county
     */
    public List<String> getEligibleCitiesInCounty(String state, String county) {
        String normalizedState = normalizeState(state);
        return regionsConfig.getEligibleCitiesInCounty(normalizedState, county);
    }
    
    /**
     * Get all eligible states
     */
    public List<String> getEligibleStates() {
        return regionsConfig.getEligibleStates();
    }
    
    /**
     * Get all eligible counties in a state
     */
    public List<String> getEligibleCountiesInState(String state) {
        String normalizedState = normalizeState(state);
        return regionsConfig.getEligibleCountiesInState(normalizedState);
    }
    
    /**
     * Get eligibility details with reason
     */
    public EligibilityCheckResult checkEligibilityWithReason(String city, String county, String state) {
        boolean eligible = isAddressEligible(city, county, state);
        
        String reason;
        if (eligible) {
            if (county != null && !county.trim().isEmpty()) {
                reason = String.format("Address in %s, %s County, %s is in an eligible region", 
                    city, county, state);
            } else {
                reason = String.format("Address in %s, %s is in an eligible region", city, state);
            }
        } else {
            // Provide more detailed reason
            String normalizedState = normalizeState(state);
            List<String> eligibleStates = getEligibleStates();
            
            if (!eligibleStates.contains(normalizedState)) {
                reason = String.format("State '%s' does not have any eligible regions configured", state);
            } else if (county != null && !county.trim().isEmpty()) {
                reason = String.format("City '%s' in %s County, %s is not in the list of eligible cities", 
                    city, county, state);
            } else {
                reason = String.format("City '%s' in %s is not in the list of eligible cities", city, state);
            }
        }
        
        return new EligibilityCheckResult(eligible, reason);
    }
    
    /**
     * Normalize state name or abbreviation to match config keys
     */
    private String normalizeState(String state) {
        if (state == null || state.trim().isEmpty()) {
            return "";
        }
        
        String trimmed = state.trim();
        
        // Check if it's an abbreviation (2 characters)
        if (trimmed.length() == 2) {
            String uppercase = trimmed.toUpperCase();
            // Return mapped full name if exists, otherwise return as-is
            return STATE_ABBREVIATIONS.getOrDefault(uppercase, uppercase);
        }
        
        // Return lowercase full name
        return trimmed.toLowerCase();
    }
    
    /**
     * Result of eligibility check with reason
     */
    public static class EligibilityCheckResult {
        private final boolean eligible;
        private final String reason;
        
        public EligibilityCheckResult(boolean eligible, String reason) {
            this.eligible = eligible;
            this.reason = reason;
        }
        
        public boolean isEligible() {
            return eligible;
        }
        
        public String getReason() {
            return reason;
        }
    }
}