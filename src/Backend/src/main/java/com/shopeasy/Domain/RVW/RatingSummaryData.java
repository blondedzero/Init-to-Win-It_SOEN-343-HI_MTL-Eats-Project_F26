package com.shopeasy.Domain.RVW;

public record RatingSummaryData(Long accountId, Double averageScore, long ratingCount, int maxScore) {
}
