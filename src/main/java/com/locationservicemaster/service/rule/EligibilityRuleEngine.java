package com.locationservicemaster.service.rule;

import com.locationservicemaster.domain.entity.EligibilityZone;
import com.locationservicemaster.dto.AddressEligibilityRequest;
import com.locationservicemaster.dto.EligibilityResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EligibilityRuleEngine {
    
    @Value("${eligibility.rules.enabled:true}")
    private boolean rulesEnabled;
    
    @Value("${eligibility.rules.min-confidence-score:0.5}")
    private double minConfidenceScore;
    
    public EligibilityResult evaluate(AddressEligibilityRequest request, List<EligibilityZone> matchedZones) {
        log.debug("Evaluating eligibility for {} matched zones", matchedZones.size());
        
        if (!rulesEnabled) {
            log.debug("Rules are disabled, returning default eligible result");
            return new EligibilityResult(true, "Rules disabled - automatically eligible", matchedZones, 1.0);
        }
        
        // No zones matched - not eligible
        if (matchedZones.isEmpty()) {
            log.debug("No zones matched for address");
            return new EligibilityResult(false, "Address is not in any eligible service area", matchedZones, 0.0);
        }
        
        // Remove duplicates and sort by priority
        List<EligibilityZone> uniqueZones = matchedZones.stream()
                .distinct()
                .sorted((z1, z2) -> Integer.compare(z2.getPriority(), z1.getPriority()))
                .collect(Collectors.toList());
        
        // Calculate confidence score based on matched zones
        double confidenceScore = calculateConfidenceScore(request, uniqueZones);
        
        // Check if confidence score meets minimum threshold
        boolean isEligible = confidenceScore >= minConfidenceScore;
        
        String reason = generateReason(isEligible, uniqueZones, confidenceScore);
        
        log.debug("Eligibility result: {}, Confidence: {}, Reason: {}", isEligible, confidenceScore, reason);
        
        return new EligibilityResult(isEligible, reason, uniqueZones, confidenceScore);
    }
    
    private double calculateConfidenceScore(AddressEligibilityRequest request, List<EligibilityZone> zones) {
        if (zones.isEmpty()) {
            return 0.0;
        }
        
        double baseScore = 0.0;
        double maxScore = 0.0;
        
        for (EligibilityZone zone : zones) {
            double zoneScore = 0.0;
            
            // Score based on zone type
            switch (zone.getZoneType()) {
                case ZIP_CODE:
                    if (zone.getZipCodes().contains(request.getZipCode())) {
                        zoneScore = 1.0;
                    }
                    break;
                case CITY:
                    if (zone.getCities().contains(request.getCity())) {
                        zoneScore = 0.8;
                    }
                    break;
                case STATE:
                    if (zone.getStates().contains(request.getState())) {
                        zoneScore = 0.6;
                    }
                    break;
                case COORDINATES:
                    if (request.getLatitude() != null && request.getLongitude() != null) {
                        if (isWithinCoordinateBounds(request, zone)) {
                            zoneScore = 0.9;
                        }
                    }
                    break;
                case CUSTOM:
                    zoneScore = 0.7;
                    break;
            }
            
            // Apply priority weight
            zoneScore *= (1.0 + zone.getPriority() * 0.1);
            
            // Keep track of the maximum score
            if (zoneScore > maxScore) {
                maxScore = zoneScore;
            }
            
            baseScore += zoneScore;
        }
        
        // Average the scores and normalize
        double avgScore = baseScore / zones.size();
        double finalScore = (avgScore + maxScore) / 2.0;
        
        // Ensure score is between 0 and 1
        return Math.min(1.0, Math.max(0.0, finalScore));
    }
    
    private boolean isWithinCoordinateBounds(AddressEligibilityRequest request, EligibilityZone zone) {
        return request.getLatitude() >= zone.getMinLatitude() && 
               request.getLatitude() <= zone.getMaxLatitude() &&
               request.getLongitude() >= zone.getMinLongitude() && 
               request.getLongitude() <= zone.getMaxLongitude();
    }
    
    private String generateReason(boolean isEligible, List<EligibilityZone> zones, double confidenceScore) {
        if (zones.isEmpty()) {
            return "Address is not in any eligible service area";
        }
        
        if (isEligible) {
            EligibilityZone primaryZone = zones.get(0); // Highest priority zone
            return String.format("Address is eligible for service (Zone: %s, Confidence: %.2f%%)", 
                    primaryZone.getZoneName(), confidenceScore * 100);
        } else {
            return String.format("Address does not meet minimum eligibility requirements (Confidence: %.2f%%, Required: %.2f%%)", 
                    confidenceScore * 100, minConfidenceScore * 100);
        }
    }
}