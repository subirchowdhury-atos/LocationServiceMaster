package com.locationservicemaster.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Google Maps Geocoding API Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleMapService {

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    @Value("${google.maps.api.key:}")
    private String googleApiKey;

    /**
     * Get address details from Google Maps Geocoding API
     * 
     * @param address The address string to geocode
     * @return Optional containing the parsed JSON response as a Map
     */
    public Optional<Map<String, Object>> getAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("Cannot geocode null or empty address");
            return Optional.empty();
        }

        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            log.error("Google Maps API key is not configured");
            return Optional.empty();
        }

        try {
            String url = buildUrl(address);
            log.debug("Calling Google Maps API for address: {}", address);
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                log.warn("Received null response from Google Maps API");
                return Optional.empty();
            }

            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(
                response, 
                new TypeReference<Map<String, Object>>() {}
            );

            // Check status
            String status = (String) responseMap.get("status");
            if (!"OK".equals(status)) {
                log.warn("Google Maps API returned status: {} for address: {}", status, address);
                return Optional.empty();
            }

            log.debug("Successfully geocoded address: {}", address);
            return Optional.of(responseMap);

        } catch (Exception e) {
            log.error("Error calling Google Maps API for address: {}", address, e);
            return Optional.empty();
        }
    }

    /**
     * Build the Google Maps API URL with query parameters
     */
    private String buildUrl(String address) {
        return UriComponentsBuilder
            .fromHttpUrl(BASE_URL)
            .queryParam("address", address)
            .queryParam("key", googleApiKey)
            .encode()
            .toUriString();
    }

    /**
     * Get the base URL (for testing/debugging)
     */
    public String getBaseUrl() {
        return BASE_URL;
    }
}