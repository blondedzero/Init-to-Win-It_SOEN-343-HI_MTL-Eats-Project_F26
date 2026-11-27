package com.shopeasy.Domain.RVW;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rvw")
public record RatingRules(
        int minScore,
        int maxScore
) {
}
