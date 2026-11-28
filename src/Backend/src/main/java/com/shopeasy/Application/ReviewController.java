package com.shopeasy.Application;

import com.shopeasy.Domain.RVW.RVWModule;
import com.shopeasy.Domain.RVW.RatingData;
import com.shopeasy.Domain.RVW.RatingRequest;
import com.shopeasy.Domain.RVW.RatingRules;
import com.shopeasy.Domain.RVW.RatingSummaryData;
import com.shopeasy.Foundation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController extends Controller {

    private final RVWModule rvwModule;

    public ReviewController(RequestAuthenticator authenticator, RVWModule rvwModule) {
        super(authenticator);
        this.rvwModule = rvwModule;
    }

    @PostMapping("/orders/{orderId}/rating")
    public ResponseEntity<RatingData> rateOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Long orderId,
                                                @RequestBody(required = false) RatingRequest request) {
        User user = authenticator.authenticate(authorization);
        RatingRequest scores = request == null ? new RatingRequest(null, null) : request;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rvwModule.rateOrder(user, orderId, scores.storeScore(), scores.courierScore()));
    }

    @GetMapping("/stores/{storeId}/rating")
    public RatingSummaryData getStoreRating(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable Long storeId) {
        authenticator.authenticate(authorization);
        return rvwModule.getStoreRating(storeId);
    }

    @GetMapping("/courier/me/rating")
    public RatingSummaryData getCourierRating(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return rvwModule.getCourierRating(user);
    }

    @GetMapping("/couriers/{courierId}/rating")
    public RatingSummaryData getCourierRatingById(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long courierId) {
        authenticator.authenticate(authorization);
        return rvwModule.getCourierRating(courierId);
    }

    @GetMapping("/ratings/scale")
    public RatingRules getRatingScale(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.authenticate(authorization);
        return rvwModule.getRatingScale();
    }
}
