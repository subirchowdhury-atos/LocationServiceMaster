package com.locationservicemaster.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Address Lookup Service
 * Handles address lookups with caching and Google Maps API integration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AddressLookupService {

    @Autowired
    private AddressCacheService addressCacheService;
    @Autowired
    private GoogleMapService googleMapService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${google.maps.enabled:false}")
    private boolean useGoogleMapsApi;

    @Value("${address.fixtures.path:classpath:fixtures/addresses.json}")
    private String fixturesPath;

    /**
     * Lookup an address with caching
     * 
     * @param address The address string to lookup
     * @return Optional containing address components map if found
     */
    public Optional<Map<String, String>> lookup(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        // Try to get from cache first
        Optional<String> cachedResult = getFromCache(address);
        if (cachedResult.isPresent()) {
            try {
                Map<String, String> result = objectMapper.readValue(
                    cachedResult.get(), 
                    new TypeReference<Map<String, String>>() {}
                );
                log.debug("Address found in cache: {}", address);
                return Optional.of(result);
            } catch (IOException e) {
                log.error("Error parsing cached address data for: {}", address, e);
            }
        }

        // Lookup address
        Optional<Map<String, String>> result = useGoogleMapsApi 
            ? lookupInGoogleMaps(address)
            : getMockAddressResponse(address);

        // Cache the result if found
        result.ifPresent(addressData -> putInCache(address, addressData));

        return result;
    }

    /**
     * Get address from cache
     */
    private Optional<String> getFromCache(String address) {
        return addressCacheService.get(address);
    }

    /**
     * Lookup address using Google Maps API
     */
    private Optional<Map<String, String>> lookupInGoogleMaps(String address) {
        try {
            Optional<Map<String, Object>> response = googleMapService.getAddress(address);
            
            if (response.isEmpty()) {
                log.debug("No results from Google Maps for address: {}", address);
                return Optional.empty();
            }

            Map<String, Object> responseData = response.get();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseData.get("results");
            
            if (results == null || results.isEmpty()) {
                log.debug("Empty results from Google Maps for address: {}", address);
                return Optional.empty();
            }

            return Optional.of(formatResult(results.get(0)));
            
        } catch (Exception e) {
            log.error("Error looking up address in Google Maps: {}", address, e);
            return Optional.empty();
        }
    }

    /**
     * Format Google Maps API result into standardized address components
     */
    private Map<String, String> formatResult(Map<String, Object> result) {
        Map<String, String> addressHash = new HashMap<>();
        List<String> streetComponents = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addressComponents = 
            (List<Map<String, Object>>) result.get("address_components");

        if (addressComponents == null) {
            return addressHash;
        }

        for (Map<String, Object> component : addressComponents) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) component.get("types");
            
            if (types == null || types.isEmpty()) {
                continue;
            }

            String type = types.get(0);
            String longName = (String) component.get("long_name");

            switch (type) {
                case "street_number":
                    streetComponents.add(longName);
                    break;
                case "route":
                    streetComponents.add(longName);
                    break;
                case "locality":
                    // City name
                    addressHash.put("city", longName);
                    break;
                case "administrative_area_level_2":
                    // Remove " County" suffix if present
                    addressHash.put("county", longName.replace(" County", ""));
                    break;
                case "administrative_area_level_1":
                    addressHash.put("state", longName);
                    break;
                case "country":
                    addressHash.put("country", longName);
                    break;
                case "postal_code":
                    // Use "zip" to match PropertyEligibilityService expectations
                    addressHash.put("zip", longName);
                    break;
            }
        }

        // Combine street components
        if (!streetComponents.isEmpty()) {
            addressHash.put("street", String.join(" ", streetComponents));
        }

        return addressHash;
    }

    /**
     * Store address in cache
     */
    private void putInCache(String address, Map<String, String> result) {
        try {
            String jsonResult = objectMapper.writeValueAsString(result);
            addressCacheService.set(address, jsonResult);
            log.debug("Cached address: {}", address);
        } catch (IOException e) {
            log.error("Error caching address data for: {}", address, e);
        }
    }

    /**
     * Get mock address response from fixtures file
     */
    private Optional<Map<String, String>> getMockAddressResponse(String address) {
        try {
            Map<String, Map<String, String>> sampleAddresses = loadSampleAddresses();
            Map<String, String> addressData = sampleAddresses.get(address);
            
            if (addressData != null) {
                log.debug("Found mock address for: {}", address);
                return Optional.of(addressData);
            }
            
            log.debug("No mock address found for: {}", address);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error loading mock address for: {}", address, e);
            return Optional.empty();
        }
    }

    /**
     * Load sample addresses from fixtures file
     */
    private Map<String, Map<String, String>> loadSampleAddresses() throws IOException {
        Resource resource = resourceLoader.getResource(fixturesPath);
        
        return objectMapper.readValue(
            resource.getInputStream(),
            new TypeReference<Map<String, Map<String, String>>>() {}
        );
    }
}