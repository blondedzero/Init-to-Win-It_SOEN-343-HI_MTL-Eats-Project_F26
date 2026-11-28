package com.shopeasy.Domain.RVW;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.entities.Rating;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RVWModule {
    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final GroceryStoreRepository groceryStoreRepository;
    private final CourierRepository courierRepository;
    private final ORDModule ordModule;
    private final RoleValidator roleValidator;
    private final EventManager eventManager;
    private final RatingRules ratingRules;

    public RVWModule(RatingRepository ratingRepository, OrderRepository orderRepository,
                     GroceryStoreRepository groceryStoreRepository, CourierRepository courierRepository,
                     ORDModule ordModule,
                     RoleValidator roleValidator, EventManager eventManager, RatingRules ratingRules) {
        this.ratingRepository = ratingRepository;
        this.orderRepository = orderRepository;
        this.groceryStoreRepository = groceryStoreRepository;
        this.courierRepository = courierRepository;
        this.ordModule = ordModule;
        this.roleValidator = roleValidator;
        this.eventManager = eventManager;
        this.ratingRules = ratingRules;
    }

    @Transactional
    public RatingData rateOrder(User user, Long orderId, Integer storeScore, Integer courierScore) {
        roleValidator.requireRole(user, Role.CUSTOMER, "rate orders");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " does not exist"));
        ordModule.requireOwnership(user, order, "rate");

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new OrderRuleViolationException("Order #" + orderId + " is " + order.getStatus()
                    + " and cannot be rated");
        }
        if (ratingRepository.existsByOrder_OrderId(orderId)) {
            throw new OrderRuleViolationException("Order #" + orderId + " has already been rated");
        }
        requireValidScore(storeScore);
        requireValidScore(courierScore);

        Rating rating = ratingRepository.save(
                new Rating(order, order.getStore(), order.getCourier(), storeScore, courierScore));
        eventManager.logEvent("Order #" + orderId + " was rated by customer " + user.getAccountId()
                + ": store " + storeScore + "/" + ratingRules.maxScore()
                + ", courier " + courierScore + "/" + ratingRules.maxScore());
        return RatingData.from(rating);
    }

    @Transactional(readOnly = true)
    public RatingSummaryData getStoreRating(Long storeId) {
        if (!groceryStoreRepository.existsById(storeId)) {
            throw new NotFoundException("Store " + storeId + " does not exist");
        }
        return summarize(storeId, ratingRepository.averageStoreScore(storeId),
                ratingRepository.countByStore_AccountId(storeId));
    }

    @Transactional(readOnly = true)
    public RatingSummaryData getCourierRating(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view a courier rating");
        return getCourierRating(user.getAccountId());
    }

    @Transactional(readOnly = true)
    public RatingSummaryData getCourierRating(Long courierId) {
        if (!courierRepository.existsById(courierId)) {
            throw new NotFoundException("Courier " + courierId + " does not exist");
        }
        return summarize(courierId, ratingRepository.averageCourierScore(courierId),
                ratingRepository.countByCourier_AccountId(courierId));
    }

    public RatingRules getRatingScale() {
        return ratingRules;
    }

    public long resetSimulationRatings() {
        long removed = ratingRepository.count();
        ratingRepository.deleteAllInBatch();
        return removed;
    }

    private RatingSummaryData summarize(Long accountId, Double average, long count) {
        Double rounded = average == null ? null : Math.round(average * 10) / 10.0;
        return new RatingSummaryData(accountId, rounded, count, ratingRules.maxScore());
    }

    private void requireValidScore(Integer score) {
        if (score == null || score < ratingRules.minScore() || score > ratingRules.maxScore()) {
            throw new IllegalArgumentException("Scores must be between " + ratingRules.minScore()
                    + " and " + ratingRules.maxScore());
        }
    }
}
