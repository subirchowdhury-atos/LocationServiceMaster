package com.locationservicemaster.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locationservicemaster.domain.entity.Address;
import com.locationservicemaster.domain.entity.EligibilityZone;
import com.locationservicemaster.dto.EligibilityResult;
import com.locationservicemaster.dto.AddressEligibilityRequest;
import com.locationservicemaster.dto.AddressEligibilityResponse;
import com.locationservicemaster.repository.AddressRepository;
import com.locationservicemaster.repository.EligibilityZoneRepository;
import com.locationservicemaster.service.rule.EligibilityRuleEngine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressEligibilityService {
    
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private EligibilityZoneRepository eligibilityZoneRepository;
    @Autowired
    private EligibilityRuleEngine ruleEngine;
    @Autowired
    private AddressCacheService addressCacheService;
    @Autowired
    private AddressLookupService addressLookupService;
    
    @Transactional
    public AddressEligibilityResponse checkEligibility(AddressEligibilityRequest request) {
        log.debug("Checking eligibility for address: {}", request);
        
        long startTime = System.currentTimeMillis();
        
        // Check cache first
        String cacheKey = buildCacheKey(request);
        Optional<AddressEligibilityResponse> cachedResponse = addressCacheService.getCachedEligibility(cacheKey);
        
        if (cachedResponse.isPresent()) {
            log.debug("Cache hit for address: {}", cacheKey);
            AddressEligibilityResponse response = cachedResponse.get();
            response.setCacheHit(true);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;
        }
        
        // NEW: Check YAML preloaded addresses first (highest priority)
        Optional<AddressEligibilityResponse> yamlResponse = checkYamlAddress(request, startTime);
        if (yamlResponse.isPresent()) {
            log.debug("Found address in YAML preloaded data");
            // Cache the YAML result
            addressCacheService.cacheEligibility(cacheKey, yamlResponse.get());
            return yamlResponse.get();
        }
        
        // Check if address already exists in database
        Optional<Address> existingAddress = findExistingAddress(request);
        
        if (existingAddress.isPresent() && existingAddress.get().getIsEligible() != null) {
            log.debug("Found existing address with eligibility: {}", existingAddress.get().getIsEligible());
            AddressEligibilityResponse response = buildResponseFromAddress(existingAddress.get(), true);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;
        }
        
        // Perform eligibility check via zones and rule engine
        EligibilityResult result = performEligibilityCheck(request);
        
        // Save or update address (temporarirly disabled)
        Address address = saveOrUpdateAddress(request, result);
        
        // Build response
        AddressEligibilityResponse response = buildResponse(request, result, address);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        // Cache the response
        addressCacheService.cacheEligibility(cacheKey, response);
        
        return response;
    }
    
    /**
     * Check if address exists in YAML preloaded data
     */
    private Optional<AddressEligibilityResponse> checkYamlAddress(AddressEligibilityRequest request, long startTime) {
        // Build address lookup string - try exact match first
        String lookupAddress = request.getStreetAddress();
        
        Optional<Map<String, String>> yamlData = addressLookupService.lookup(lookupAddress);
        log.info("YAML lookup result: {}", yamlData.isPresent() ? "FOUND" : "NOT FOUND");

        if (yamlData.isPresent()) {
            Map<String, String> addressData = yamlData.get();
            log.info("Address data: {}", addressData); 
            
            // Check if this YAML entry has eligibility info
            String eligibleStr = addressData.get("eligible");
            if (eligibleStr != null) {
                boolean eligible = Boolean.parseBoolean(eligibleStr);
                
                log.info("Found YAML preloaded address: {} - Eligible: {}", lookupAddress, eligible);
                
                // Build address details from YAML data
                AddressEligibilityResponse.AddressDetails addressDetails = AddressEligibilityResponse.AddressDetails.builder()
                        .streetAddress(addressData.getOrDefault("street", request.getStreetAddress()))
                        .streetAddress2(request.getStreetAddress2())
                        .city(addressData.getOrDefault("city", request.getCity()))
                        .state(addressData.getOrDefault("state", request.getState()))
                        .zipCode(addressData.getOrDefault("zip", request.getZipCode()))
                        .country(addressData.getOrDefault("country", request.getCountry()))
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .formattedAddress(formatYamlAddress(addressData))
                        .build();
                
                String reason = eligible 
                    ? String.format("Address is in eligible region: %s County, %s", 
                        addressData.get("county"), addressData.get("state"))
                    : String.format("Address is not in an eligible region: %s County, %s", 
                        addressData.get("county"), addressData.get("state"));
                
                AddressEligibilityResponse response = AddressEligibilityResponse.builder()
                        .eligible(eligible)
                        .reason(reason)
                        .address(addressDetails)
                        .matchedZones(eligible ? List.of(addressData.get("county") + ", " + addressData.get("state")) : List.of())
                        .confidenceScore(1.0) // High confidence for preloaded data
                        .checkedAt(LocalDateTime.now())
                        .cacheHit(false)
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
                
                return Optional.of(response);
            }
        }
        
        return Optional.empty();
    }
    
    private String formatYamlAddress(Map<String, String> addressData) {
        StringBuilder formatted = new StringBuilder();
        formatted.append(addressData.getOrDefault("street", ""));
        formatted.append(", ").append(addressData.getOrDefault("city", ""));
        formatted.append(", ").append(addressData.getOrDefault("state", ""));
        formatted.append(" ").append(addressData.getOrDefault("zip", ""));
        String country = addressData.get("country");
        if (country != null && !country.isEmpty()) {
            formatted.append(", ").append(country);
        }
        return formatted.toString();
    }
    
    private Optional<Address> findExistingAddress(AddressEligibilityRequest request) {
        return addressRepository.findByStreetAddressAndCityAndStateAndZipCode(
                request.getStreetAddress(),
                request.getCity(),
                request.getState(),
                request.getZipCode()
        );
    }
    
    private EligibilityResult performEligibilityCheck(AddressEligibilityRequest request) {
        List<EligibilityZone> matchedZones = new ArrayList<>();
        
        // Check by zip code
        matchedZones.addAll(eligibilityZoneRepository.findActiveZonesByZipCode(request.getZipCode()));
        
        // Check by city and state
        matchedZones.addAll(eligibilityZoneRepository.findActiveZonesByCityAndState(
                request.getCity(), request.getState()));
        
        // Check by coordinates if provided
        if (request.getCheckCoordinates() && request.getLatitude() != null && request.getLongitude() != null) {
            matchedZones.addAll(eligibilityZoneRepository.findActiveZonesByCoordinates(
                    request.getLatitude(), request.getLongitude()));
        }
        
        // Apply business rules
        return ruleEngine.evaluate(request, matchedZones);
    }
    
    @Transactional
    private Address saveOrUpdateAddress(AddressEligibilityRequest request, EligibilityResult result) {
        Address address = findExistingAddress(request)
                .orElse(Address.builder()
                        .streetAddress(request.getStreetAddress())
                        .streetAddress2(request.getStreetAddress2())
                        .city(request.getCity())
                        .state(request.getState())
                        .zipCode(request.getZipCode())
                        .country(request.getCountry())
                        .build());
        
        address.setIsEligible(result.isEligible());
        address.setEligibilityReason(result.getReason());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        
        return addressRepository.save(address);
    }
    
    private AddressEligibilityResponse buildResponse(AddressEligibilityRequest request, 
                                                    EligibilityResult result, 
                                                    Address address) {
        AddressEligibilityResponse.AddressDetails addressDetails = AddressEligibilityResponse.AddressDetails.builder()
                .streetAddress(address.getStreetAddress())
                .streetAddress2(address.getStreetAddress2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .formattedAddress(formatAddress(address))
                .build();
        
        return AddressEligibilityResponse.builder()
                .eligible(result.isEligible())
                .reason(request.getIncludeReason() ? result.getReason() : null)
                .address(addressDetails)
                .matchedZones(result.getMatchedZoneNames())
                .confidenceScore(result.getConfidenceScore())
                .checkedAt(LocalDateTime.now())
                .cacheHit(false)
                .build();
    }
    
    private AddressEligibilityResponse buildResponseFromAddress(Address address, boolean cacheHit) {
        AddressEligibilityResponse.AddressDetails addressDetails = AddressEligibilityResponse.AddressDetails.builder()
                .streetAddress(address.getStreetAddress())
                .streetAddress2(address.getStreetAddress2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .formattedAddress(formatAddress(address))
                .build();
        
        return AddressEligibilityResponse.builder()
                .eligible(address.getIsEligible())
                .reason(address.getEligibilityReason())
                .address(addressDetails)
                .checkedAt(LocalDateTime.now())
                .cacheHit(cacheHit)
                .build();
    }
    
    private String formatAddress(Address address) {
        StringBuilder formatted = new StringBuilder();
        formatted.append(address.getStreetAddress());
        if (address.getStreetAddress2() != null && !address.getStreetAddress2().isEmpty()) {
            formatted.append(", ").append(address.getStreetAddress2());
        }
        formatted.append(", ").append(address.getCity());
        formatted.append(", ").append(address.getState());
        formatted.append(" ").append(address.getZipCode());
        if (address.getCountry() != null) {
            formatted.append(", ").append(address.getCountry());
        }
        return formatted.toString();
    }
    
    private String buildCacheKey(AddressEligibilityRequest request) {
        return String.format("%s:%s:%s:%s",
                request.getStreetAddress(),
                request.getCity(),
                request.getState(),
                request.getZipCode()).toLowerCase();
    }
}