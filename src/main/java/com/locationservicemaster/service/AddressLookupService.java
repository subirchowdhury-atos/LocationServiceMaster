package com.locationservicemaster.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AddressLookupService {
    
    @Autowired
    @Qualifier("redisTemplateObject")
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ADDRESS_CACHE_PREFIX = "address:";
    private static final long CACHE_TTL_HOURS = 24;
    
    // In-memory cache (always used, with or without Redis)
    private final Map<String, Map<String, String>> memoryCache = new HashMap<>();
    
    // Flag to track if Redis is available
    private volatile boolean redisAvailable = false;

    @PostConstruct
    public void loadAddressesFromYaml() {
        log.info("Loading addresses from addresses.yml...");
        
        // Test Redis availability
        redisAvailable = testRedisConnection();
        
        try {
            ClassPathResource resource = new ClassPathResource("data/addresses.yml");
            InputStream inputStream = resource.getInputStream();
            
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            AddressData addressData = yamlMapper.readValue(inputStream, AddressData.class);
            
            int loadedCount = 0;
            int redisCount = 0;
            
            for (AddressEntry entry : addressData.getAddresses()) {
                String key = entry.getInput().toLowerCase().trim();
                
                Map<String, String> formattedAddress = new HashMap<>();
                formattedAddress.put("street", entry.getStreet());
                formattedAddress.put("city", entry.getCity());
                formattedAddress.put("zip", entry.getZip());
                formattedAddress.put("state", entry.getState());
                formattedAddress.put("county", entry.getCounty());
                formattedAddress.put("country", entry.getCountry());
                formattedAddress.put("eligible", String.valueOf(entry.isEligible()));
                
                // Always store in memory
                memoryCache.put(key, formattedAddress);
                loadedCount++;
                
                // Also try Redis if available
                if (redisAvailable) {
                    try {
                        String redisKey = ADDRESS_CACHE_PREFIX + key;
                        redisTemplate.opsForValue().set(redisKey, formattedAddress, CACHE_TTL_HOURS, TimeUnit.HOURS);
                        redisCount++;
                    } catch (Exception e) {
                        log.warn("Failed to cache address in Redis: {}", key);
                        redisAvailable = false;
                    }
                }
                
                log.debug("Loaded address: {} -> {}, {}", entry.getInput(), entry.getCity(), entry.getState());
            }
            
            if (redisAvailable) {
                log.info("Successfully loaded {} addresses (Memory: {}, Redis: {})", 
                    loadedCount, loadedCount, redisCount);
            } else {
                log.info("Successfully loaded {} addresses into memory cache only (Redis unavailable)", 
                    loadedCount);
            }
            
        } catch (Exception e) {
            log.error("Failed to load addresses from YAML file", e);
        }
    }
    
    private boolean testRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection successful");
            return true;
        } catch (Exception e) {
            log.warn("Redis connection failed: {}. Using memory cache only.", e.getMessage());
            return false;
        }
    }

    /**
     * Lookup an address
     * Priority: Memory Cache -> Redis Cache
     */
    public Optional<Map<String, String>> lookup(String address) {
        if (address == null || address.trim().isEmpty()) {
            return Optional.empty();
        }

        String key = address.toLowerCase().trim();
        
        log.debug("Looking up address: '{}'", key);
        
        // 1. Check in-memory cache first (fastest)
        if (memoryCache.containsKey(key)) {
            log.debug("Address found in memory cache: {}", address);
            return Optional.of(memoryCache.get(key));
        }
        
        log.debug("Address not found in memory cache");

        // 2. Try Redis if available
        if (redisAvailable) {
            String redisKey = ADDRESS_CACHE_PREFIX + key;
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> result = (Map<String, String>) redisTemplate.opsForValue().get(redisKey);
                
                if (result != null) {
                    log.debug("Address found in Redis cache: {}", address);
                    // Cache in memory for faster future lookups
                    memoryCache.put(key, result);
                    return Optional.of(result);
                }
                
                log.debug("Address not found in Redis cache");
            } catch (Exception e) {
                log.warn("Redis lookup failed: {}. Marking Redis as unavailable.", e.getMessage());
                redisAvailable = false;
            }
        }

        // Not found
        log.debug("Address not found: {}", address);
        return Optional.empty();
    }

    // Inner classes for YAML parsing
    @Data
    private static class AddressData {
        private List<AddressEntry> addresses;
    }

    @Data
    private static class AddressEntry {
        private String input;
        private String street;
        private String city;
        private String zip;
        private String state;
        private String county;
        private String country;
        private boolean eligible;
    }
}