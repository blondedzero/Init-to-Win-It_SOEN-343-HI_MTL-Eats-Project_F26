package com.shopeasy.Domain.RVW;

import com.shopeasy.Foundation.entities.Rating;

import java.time.Instant;

public record RatingData(Long ratingId, Long orderId, Long storeId, int storeScore, int courierScore, Instant ratedAt) {
    public static RatingData from(Rating rating) {
        return new RatingData(rating.getRatingId(), rating.getOrder().getOrderId(), rating.getStore().getAccountId(),
                rating.getStoreScore(), rating.getCourierScore(), rating.getRatedAt());
    }
}
