package com.locationservicemaster.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locationservicemaster.dto.AddressEligibilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Redis cache operations for address eligibility checks.
 * Provides caching functionality to improve performance and reduce database load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressCacheService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String CACHE_PREFIX = "eligibility:";
    private static final String ADDRESS_LOOKUP_PREFIX = "address:lookup:";

    
    @Value("${eligibility.rules.cache-duration:3600}")
    private long cacheDurationSeconds;


    /**
     * Get cached address lookup result (raw address components)
     * @param address The address string to lookup
     * @return Optional containing cached JSON string of address components
     */
    public Optional<String> get(String address) {
        try {
            String cacheKey = ADDRESS_LOOKUP_PREFIX + address;
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedValue != null) {
                log.debug("Address lookup cache hit for: {}", address);
            } else {
                log.debug("Address lookup cache miss for: {}", address);
            }
            
            return Optional.ofNullable(cachedValue);
        } catch (Exception e) {
            log.error("Error retrieving address lookup from cache: {}", address, e);
            return Optional.empty();
        }
    }

    /**
     * Cache address lookup result (raw address components)
     * @param address The address string
     * @param value JSON string of address components
     */
    public void set(String address, String value) {
        try {
            String cacheKey = ADDRESS_LOOKUP_PREFIX + address;
            redisTemplate.opsForValue().set(
                cacheKey, 
                value, 
                cacheDurationSeconds, 
                TimeUnit.SECONDS
            );
            log.debug("Cached address lookup for: {}", address);
        } catch (Exception e) {
            log.error("Error caching address lookup: {}", address, e);
        }
    }
    
    /**
     * Retrieve cached eligibility response if available
     * @param key The cache key (typically built from address components)
     * @return Optional containing the cached response, or empty if not found or error
     */
    public Optional<AddressEligibilityResponse> getCachedEligibility(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedValue != null) {
                log.debug("Cache hit for key: {}", cacheKey);
                AddressEligibilityResponse response = objectMapper.readValue(
                    cachedValue, 
                    AddressEligibilityResponse.class
                );
                return Optional.of(response);
            }
            
            log.debug("Cache miss for key: {}", cacheKey);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached response for key: {}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving from cache for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    /**
     * Cache an eligibility response with configured TTL
     * @param key The cache key
     * @param response The response to cache
     */
    public void cacheEligibility(String key, AddressEligibilityResponse response) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String value = objectMapper.writeValueAsString(response);
            
            redisTemplate.opsForValue().set(
                cacheKey, 
                value, 
                cacheDurationSeconds, 
                TimeUnit.SECONDS
            );
            
            log.debug("Cached eligibility result for key: {} with TTL: {}s", 
                cacheKey, cacheDurationSeconds);
        } catch (JsonProcessingException e) {
            log.error("Error serializing response for cache: {}", key, e);
        } catch (Exception e) {
            log.error("Error caching eligibility for key: {}", key, e);
        }
    }
    
    /**
     * Evict a specific entry from the cache
     * @param key The cache key to evict
     */
    public void evictCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.debug("Cache evicted for key: {}, Success: {}", cacheKey, deleted);
        } catch (Exception e) {
            log.error("Error evicting cache for key: {}", key, e);
        }
    }
    
    /**
     * Clear all eligibility cache entries
     */
    public void clearAllCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("Cleared {} eligibility cache entries", deleted);
            } else {
                log.info("No eligibility cache entries to clear");
            }
        } catch (Exception e) {
            log.error("Error clearing all cache", e);
        }
    }
    
    /**
     * Check if a cache entry exists
     * @param key The cache key to check
     * @return true if the key exists in cache, false otherwise
     */
    public boolean isCached(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("Error checking cache existence for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get the remaining TTL for a cached entry
     * @param key The cache key
     * @return TTL in seconds, -1 if key doesn't exist, -2 if key exists but has no TTL
     */
    public Long getCacheTTL(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            return -1L;
        }
    }
    
    /**
     * Update the TTL for an existing cache entry
     * @param key The cache key
     * @param seconds New TTL in seconds
     * @return true if successful, false otherwise
     */
    public boolean updateCacheTTL(String key, long seconds) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return Boolean.TRUE.equals(
                redisTemplate.expire(cacheKey, seconds, TimeUnit.SECONDS)
            );
        } catch (Exception e) {
            log.error("Error updating TTL for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get cache statistics
     * @return CacheStats object with cache statistics
     */
    public CacheStats getCacheStats() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            int totalEntries = keys != null ? keys.size() : 0;
            
            // Get Redis server info if needed
            var connectionFactory = redisTemplate.getConnectionFactory();
            Long dbSize = null;
            if (connectionFactory != null) {
                var connection = connectionFactory.getConnection();
                if (connection != null) {
                    dbSize = connection.serverCommands().dbSize();
                    connection.close();
                }
            }
            
            return new CacheStats(totalEntries, dbSize);
        } catch (Exception e) {
            log.error("Error getting cache stats", e);
            return new CacheStats(0, null);
        }
    }
    
    /**
     * Inner class for cache statistics
     */
    public static class CacheStats {
        private final int eligibilityEntries;
        private final Long totalDbSize;
        
        public CacheStats(int eligibilityEntries, Long totalDbSize) {
            this.eligibilityEntries = eligibilityEntries;
            this.totalDbSize = totalDbSize;
        }
        
        public int getEligibilityEntries() {
            return eligibilityEntries;
        }
        
        public Long getTotalDbSize() {
            return totalDbSize;
        }
    }
}