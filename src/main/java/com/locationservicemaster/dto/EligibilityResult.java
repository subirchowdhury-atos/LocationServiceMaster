package com.locationservicemaster.dto;

import com.locationservicemaster.domain.entity.EligibilityZone;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Result class for eligibility evaluation
 */
@Getter
public class EligibilityResult {
    private final boolean eligible;
    private final String reason;
    private final List<String> matchedZoneNames;
    private final Double confidenceScore;
    
    public EligibilityResult(boolean eligible, String reason, List<EligibilityZone> matchedZones, Double confidenceScore) {
        this.eligible = eligible;
        this.reason = reason;
        this.matchedZoneNames = matchedZones.stream()
                .map(EligibilityZone::getZoneName)
                .collect(Collectors.toList());
        this.confidenceScore = confidenceScore;
    }
}