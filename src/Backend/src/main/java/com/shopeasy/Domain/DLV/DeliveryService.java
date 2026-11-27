package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeliveryService {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;
    private final CourierOrderLookup courierOrderLookup;
    private final CourierStatusPublisher courierStatusPublisher;

    DeliveryService(CourierRepository courierRepository, OrderRepository orderRepository,
                    EventManager eventManager, RoleValidator roleValidator,
                    CourierOrderLookup courierOrderLookup, CourierStatusPublisher courierStatusPublisher) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
        this.courierOrderLookup = courierOrderLookup;
        this.courierStatusPublisher = courierStatusPublisher;
    }

    @Transactional
    public OrderResult confirmPickup(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "confirm order pickups");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        Order order = courierOrderLookup.findAssignedOrder(orderId, courier);
        if (courier.getStatus() != CourierStatus.ASSIGNED || order.getStatus() != OrderStatus.READY
                || !order.isCourierAccepted()) {
            throw new OrderRuleViolationException("Order #" + orderId
                    + " can only be picked up after you accept it");
        }

        order.markPickedUp();
        courier.confirmPickup();
        orderRepository.save(order);
        courierRepository.save(courier);
        Long eventId = eventManager.logEvent("Order #" + orderId + ": READY -> PICKED_UP by courier #" + courier.getAccountId()
                + "; courier ASSIGNED -> CARRYING");
        return new OrderResult("Order #" + orderId + " marked picked up", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult confirmDelivery(User user, Long orderId) {
        // Keep completion isolated here for a future after-commit delivery event.
        roleValidator.requireRole(user, Role.COURIER, "confirm order deliveries");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        Order order = courierOrderLookup.findAssignedOrder(orderId, courier);
        if (courier.getStatus() != CourierStatus.CARRYING || order.getStatus() != OrderStatus.PICKED_UP) {
            throw new OrderRuleViolationException("Order #" + orderId
                    + " can only be marked delivered after it has been picked up");
        }

        order.markDelivered();
        courier.confirmDelivery();
        orderRepository.save(order);
        courierRepository.save(courier);
        Long eventId = eventManager.logEvent("Order #" + orderId + ": PICKED_UP -> DELIVERED by courier #" + courier.getAccountId()
                + "; courier CARRYING -> IDLE");
        courierStatusPublisher.notifyCourierIdle(courier);
        return new OrderResult("Order #" + orderId + " marked delivered", eventId, OrderData.from(order));
    }
}
