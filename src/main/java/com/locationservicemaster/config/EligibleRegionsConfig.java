package com.locationservicemaster.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Configuration for eligible regions loaded from YAML files
 */
@Configuration
@ConfigurationProperties(prefix = "eligible-regions")
@Data
@Slf4j
public class EligibleRegionsConfig {
    
    private Map<String, StateConfig> states;
    
    @Data
    public static class StateConfig {
        private Map<String, CountyConfig> counties;
    }
    
    @Data
    public static class CountyConfig {
        private List<String> cities;
    }
    
    @PostConstruct
    public void init() {
        if (states == null || states.isEmpty()) {
            log.warn("No eligible regions configured! Check application-eligible-regions.yml");
        } else {
            log.info("Loaded eligible regions for {} states", states.size());
            states.forEach((state, config) -> {
                if (config.getCounties() != null) {
                    int totalCities = config.getCounties().values().stream()
                        .filter(c -> c.getCities() != null)
                        .mapToInt(c -> c.getCities().size())
                        .sum();
                    log.debug("State '{}': {} counties, {} cities", 
                        state, config.getCounties().size(), totalCities);
                }
            });
        }
    }
    
    /**
     * Check if a city is eligible in a given state and county
     */
    public boolean isCityEligible(String state, String county, String city) {
        if (states == null || state == null || county == null || city == null) {
            log.trace("Null check failed - state: {}, county: {}, city: {}", state, county, city);
            return false;
        }
        
        String normalizedState = normalizeKey(state);
        StateConfig stateConfig = states.get(normalizedState);
        
        if (stateConfig == null || stateConfig.getCounties() == null) {
            log.trace("State '{}' not found in configuration", normalizedState);
            return false;
        }
        
        String normalizedCounty = normalizeKey(county);
        CountyConfig countyConfig = stateConfig.getCounties().get(normalizedCounty);
        
        if (countyConfig == null || countyConfig.getCities() == null) {
            log.trace("County '{}' not found in state '{}'", normalizedCounty, normalizedState);
            return false;
        }
        
        String normalizedCity = normalizeCity(city);
        boolean eligible = countyConfig.getCities().stream()
            .anyMatch(eligibleCity -> normalizeCity(eligibleCity).equals(normalizedCity));
            
        log.trace("Eligibility check: city={}, county={}, state={}, result={}", 
            city, county, state, eligible);
        
        return eligible;
    }
    
    /**
     * Check if a city is eligible in any county within a state
     */
    public boolean isCityEligibleInState(String state, String city) {
        if (states == null || state == null || city == null) {
            log.trace("Null check failed - state: {}, city: {}", state, city);
            return false;
        }
        
        String normalizedState = normalizeKey(state);
        StateConfig stateConfig = states.get(normalizedState);
        
        if (stateConfig == null || stateConfig.getCounties() == null) {
            log.trace("State '{}' not found in configuration", normalizedState);
            return false;
        }
        
        String normalizedCity = normalizeCity(city);
        boolean eligible = stateConfig.getCounties().values().stream()
            .filter(county -> county.getCities() != null)
            .flatMap(county -> county.getCities().stream())
            .anyMatch(eligibleCity -> normalizeCity(eligibleCity).equals(normalizedCity));
            
        log.trace("State-wide eligibility check: city={}, state={}, result={}", 
            city, state, eligible);
        
        return eligible;
    }
    
    /**
     * Get all eligible cities in a county
     */
    public List<String> getEligibleCitiesInCounty(String state, String county) {
        if (states == null || state == null || county == null) {
            return List.of();
        }
        
        String normalizedState = normalizeKey(state);
        StateConfig stateConfig = states.get(normalizedState);
        
        if (stateConfig == null || stateConfig.getCounties() == null) {
            return List.of();
        }
        
        String normalizedCounty = normalizeKey(county);
        CountyConfig countyConfig = stateConfig.getCounties().get(normalizedCounty);
        
        if (countyConfig == null || countyConfig.getCities() == null) {
            return List.of();
        }
        
        return countyConfig.getCities();
    }
    
    /**
     * Get all eligible states
     */
    public List<String> getEligibleStates() {
        if (states == null) {
            return List.of();
        }
        return List.copyOf(states.keySet());
    }
    
    /**
     * Get all eligible counties in a state
     */
    public List<String> getEligibleCountiesInState(String state) {
        if (states == null || state == null) {
            return List.of();
        }
        
        String normalizedState = normalizeKey(state);
        StateConfig stateConfig = states.get(normalizedState);
        
        if (stateConfig == null || stateConfig.getCounties() == null) {
            return List.of();
        }
        
        return List.copyOf(stateConfig.getCounties().keySet());
    }
    
    /**
     * Normalize state/county key for case-insensitive lookup
     */
    private String normalizeKey(String key) {
        if (key == null) return "";
        
        String trimmed = key.trim();
        
        // Handle state abbreviations (2 letters)
        if (trimmed.length() == 2) {
            return trimmed.toUpperCase();
        }
        
        // Full names - lowercase
        return trimmed.toLowerCase();
    }
    
    /**
     * Normalize city name for comparison
     */
    private String normalizeCity(String city) {
        if (city == null) return "";
        return city.toLowerCase().trim();
    }
}