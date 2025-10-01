package com.locationservicemaster.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entity representing an eligibility zone for address service coverage.
 * Zones can be defined by ZIP codes, cities, states, or geographic coordinates.
 */
@Entity
@Table(name = "eligibility_zones", indexes = {
    @Index(name = "idx_zone_name", columnList = "zone_name"),
    @Index(name = "idx_zone_type", columnList = "zone_type"),
    @Index(name = "idx_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EligibilityZone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "zone_name", nullable = false, unique = true)
    private String zoneName;
    
    @Column(name = "zone_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ZoneType zoneType;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "zone_zip_codes", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "zip_code")
    private Set<String> zipCodes;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "zone_cities", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "city")
    private Set<String> cities;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "zone_states", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "state")
    private Set<String> states;
    
    @Column(name = "min_latitude")
    private Double minLatitude;
    
    @Column(name = "max_latitude")
    private Double maxLatitude;
    
    @Column(name = "min_longitude")
    private Double minLongitude;
    
    @Column(name = "max_longitude")
    private Double maxLongitude;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Enum defining the type of eligibility zone
     */
    public enum ZoneType {
        ZIP_CODE,    // Zone defined by ZIP codes
        CITY,        // Zone defined by city names
        STATE,       // Zone defined by state names
        COORDINATES, // Zone defined by latitude/longitude bounds
        CUSTOM       // Custom zone with mixed criteria
    }
}
