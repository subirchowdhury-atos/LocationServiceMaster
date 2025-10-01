package com.locationservicemaster.repository;

import com.locationservicemaster.domain.entity.EligibilityZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EligibilityZone entity.
 * Provides database access methods for eligibility zone operations.
 */
@Repository
public interface EligibilityZoneRepository extends JpaRepository<EligibilityZone, Long> {
    
    /**
     * Find all active eligibility zones
     * @return List of active zones
     */
    List<EligibilityZone> findByIsActiveTrue();
    
    /**
     * Find an eligibility zone by its unique name
     * @param zoneName The zone name
     * @return Optional containing the zone if found
     */
    Optional<EligibilityZone> findByZoneName(String zoneName);
    
    /**
     * Find active zones that contain a specific ZIP code
     * @param zipCode The ZIP code to search for
     * @return List of matching active zones
     */
    @Query("SELECT DISTINCT z FROM EligibilityZone z " +
           "LEFT JOIN z.zipCodes zc " +
           "WHERE z.isActive = true AND zc = :zipCode")
    List<EligibilityZone> findActiveZonesByZipCode(@Param("zipCode") String zipCode);
    
    /**
     * Find active zones that contain a specific city and state combination
     * @param city The city name
     * @param state The state name
     * @return List of matching active zones
     */
    @Query("SELECT DISTINCT z FROM EligibilityZone z " +
           "LEFT JOIN z.cities c " +
           "LEFT JOIN z.states s " +
           "WHERE z.isActive = true AND c = :city AND s = :state")
    List<EligibilityZone> findActiveZonesByCityAndState(
            @Param("city") String city,
            @Param("state") String state);
    
    /**
     * Find active zones that contain specific coordinates
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return List of matching active zones
     */
    @Query("SELECT z FROM EligibilityZone z WHERE z.isActive = true AND " +
           "z.minLatitude <= :lat AND z.maxLatitude >= :lat AND " +
           "z.minLongitude <= :lon AND z.maxLongitude >= :lon")
    List<EligibilityZone> findActiveZonesByCoordinates(
            @Param("lat") Double latitude,
            @Param("lon") Double longitude);
    
    /**
     * Find all active zones ordered by priority (highest first)
     * @return List of zones ordered by priority descending
     */
    @Query("SELECT z FROM EligibilityZone z WHERE z.isActive = true " +
           "ORDER BY z.priority DESC")
    List<EligibilityZone> findAllActiveOrderByPriority();
    
    /**
     * Find zones by specific zone type
     * @param zoneType The type of zone to search for
     * @return List of zones with the specified type
     */
    List<EligibilityZone> findByZoneType(EligibilityZone.ZoneType zoneType);
    
    /**
     * Find active zones by specific zone type
     * @param zoneType The type of zone to search for
     * @param isActive Whether the zone is active
     * @return List of zones matching the criteria
     */
    List<EligibilityZone> findByZoneTypeAndIsActive(
            EligibilityZone.ZoneType zoneType, 
            Boolean isActive);
    
    /**
     * Count active zones
     * @return Number of active zones
     */
    Long countByIsActiveTrue();
    
    /**
     * Check if a zone exists with the given name
     * @param zoneName The zone name to check
     * @return true if exists, false otherwise
     */
    boolean existsByZoneName(String zoneName);
    
    /**
     * Find zones containing a specific state
     * @param state The state to search for
     * @return List of zones containing the state
     */
    @Query("SELECT DISTINCT z FROM EligibilityZone z " +
           "LEFT JOIN z.states s " +
           "WHERE z.isActive = true AND s = :state")
    List<EligibilityZone> findActiveZonesByState(@Param("state") String state);
    
    /**
     * Find zones containing a specific city
     * @param city The city to search for
     * @return List of zones containing the city
     */
    @Query("SELECT DISTINCT z FROM EligibilityZone z " +
           "LEFT JOIN z.cities c " +
           "WHERE z.isActive = true AND c = :city")
    List<EligibilityZone> findActiveZonesByCity(@Param("city") String city);
    
    /**
     * Delete zones by active status
     * @param isActive The active status
     * @return Number of deleted zones
     */
    Long deleteByIsActive(Boolean isActive);
}