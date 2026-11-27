package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Component;

@Component
class CourierOrderLookup {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;

    CourierOrderLookup(CourierRepository courierRepository, OrderRepository orderRepository) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
    }

    Courier findCourier(Long courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("Courier with id " + courierId + " not found"));
    }

    Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
    }

    Order findOfferedOrder(Long orderId, Courier courier) {
        Order order = findOrder(orderId);
        if (order.getCourier() == null || !order.getCourier().getAccountId().equals(courier.getAccountId())) {
            throw new PermissionDeniedException("Order #" + orderId + " was not offered to you");
        }
        if (order.getStatus() != OrderStatus.READY || courier.getStatus() != CourierStatus.IDLE
                || order.isCourierAccepted()) {
            throw new OrderRuleViolationException("Order #" + orderId + " has no pending offer (order " + order.getStatus()
                    + ", courier " + courier.getStatus() + ")");
        }
        return order;
    }

    Order findAssignedOrder(Long orderId, Courier courier) {
        Order order = findOrder(orderId);
        if (order.getCourier() == null || !order.getCourier().getAccountId().equals(courier.getAccountId())) {
            throw new PermissionDeniedException("Order #" + orderId + " is not assigned to you");
        }
        return order;
    }

    void requireCourierState(Courier courier, Order order, String failureMessage) {
        if (courier.getStatus() != CourierStatus.ASSIGNED || order.getStatus() != OrderStatus.READY
                || !order.isCourierAccepted()) {
            throw new OrderRuleViolationException(failureMessage);
        }
    }
}
