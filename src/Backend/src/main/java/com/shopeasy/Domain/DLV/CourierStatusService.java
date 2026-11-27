package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class CourierStatusService {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;
    private final CourierOrderLookup courierOrderLookup;
    private final CourierStatusPublisher courierStatusPublisher;
    private final CourierDispatchService courierDispatchService;

    CourierStatusService(CourierRepository courierRepository, OrderRepository orderRepository,
                         EventManager eventManager, RoleValidator roleValidator,
                         CourierOrderLookup courierOrderLookup, CourierStatusPublisher courierStatusPublisher,
                         CourierDispatchService courierDispatchService) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
        this.courierOrderLookup = courierOrderLookup;
        this.courierStatusPublisher = courierStatusPublisher;
        this.courierDispatchService = courierDispatchService;
    }

    @Transactional(readOnly = true)
    public CourierDashboardData getDashboard(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view their delivery dashboard");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        List<OrderStatus> visibleStatuses = switch (courier.getStatus()) {
            case IDLE, ASSIGNED -> List.of(OrderStatus.READY);
            case CARRYING -> List.of(OrderStatus.PICKED_UP);
            case OFFLINE -> List.of();
        };
        List<OrderData> orders = visibleStatuses.stream()
                .flatMap(status -> orderRepository.findByCourier_AccountIdAndStatus(courier.getAccountId(), status).stream())
                .map(OrderData::from)
                .toList();
        return new CourierDashboardData(courier.getStatus(), orders);
    }

    @Transactional
    public void goOnDuty(User user) {
        roleValidator.requireRole(user, Role.COURIER, "go online");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        if (courier.getStatus() != CourierStatus.OFFLINE) {
            throw new OrderRuleViolationException("You are already online or have an active order.");
        }
        courier.goOnline();
        courierRepository.save(courier);
        eventManager.logEvent("Courier #" + courier.getAccountId() + ": OFFLINE -> IDLE (on duty)");
        courierStatusPublisher.notifyCourierIdle(courier);
    }

    @Transactional
    public void goOffDuty(User user) {
        roleValidator.requireRole(user, Role.COURIER, "go offline");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        List<Order> readyOrders = orderRepository.findByCourier_AccountIdAndStatus(
                courier.getAccountId(), OrderStatus.READY);
        boolean hasAcceptedAssignment = readyOrders.stream().anyMatch(Order::isCourierAccepted);
        if (courier.getStatus() == CourierStatus.CARRYING || hasAcceptedAssignment
                || (courier.getStatus() == CourierStatus.ASSIGNED && readyOrders.isEmpty())) {
            throw new OrderRuleViolationException("You cannot go offline after accepting an order or while carrying it. "
                    + "Decline an unaccepted offer or complete the delivery first.");
        }
        if (courier.getStatus() == CourierStatus.OFFLINE) {
            throw new OrderRuleViolationException("You are already offline.");
        }

        Long courierId = courier.getAccountId();
        CourierStatus previousStatus = courier.getStatus();
        if (previousStatus == CourierStatus.ASSIGNED) {
            courier.goOfflineWithPendingOffer();
        } else {
            courier.goOffline();
        }
        courierRepository.save(courier);
        eventManager.logEvent("Courier #" + courierId + ": " + previousStatus + " -> OFFLINE (off duty)");
        readyOrders.forEach(order -> courierDispatchService.withdrawOffer(order, courier, false));
    }

    @Transactional
    public void releaseCourier(Courier courier) {
        courier.setStatus(CourierStatus.IDLE);
        courierRepository.save(courier);
        eventManager.logEvent("Courier #" + courier.getAccountId() + " released -> IDLE");
        courierStatusPublisher.notifyCourierIdle(courier);
    }

    public void subscribe(CourierStatusObserver observer) {
        courierStatusPublisher.subscribe(observer);
    }

    public void unsubscribe(CourierStatusObserver observer) {
        courierStatusPublisher.unsubscribe(observer);
    }

}
