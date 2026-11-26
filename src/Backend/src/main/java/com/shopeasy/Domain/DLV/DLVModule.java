package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DLVModule {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;

    private final List<CourierStatusObserver> observers = new CopyOnWriteArrayList<>();
    private final Map<Long, Set<Long>> declinedBy = new ConcurrentHashMap<>();

    public DLVModule(CourierRepository courierRepository, OrderRepository orderRepository, EventManager eventManager, RoleValidator roleValidator) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
    }

    public void subscribe(CourierStatusObserver o) {
        observers.add(o);
    }

    public void unsubscribe(CourierStatusObserver o) {
        observers.remove(o);
    }

    @Transactional(readOnly = true)
    public List<OrderData> getPendingOffers(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view delivery offers");
        Courier courier = findCourier(user.getAccountId());
        return orderRepository.findByCourier_AccountIdAndStatus(courier.getAccountId(), OrderStatus.READY)
                .stream()
                .filter(order -> !order.isCourierAccepted())
                .map(OrderData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourierDashboardData getDashboard(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view their delivery dashboard");
        Courier courier = findCourier(user.getAccountId());
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

    private void notifyCourierIdle(Courier c) {
        for (CourierStatusObserver o : observers) {
            o.onCourierIdle(c);
        }
    }

    @Transactional
    public void goOnDuty(User user) {
        roleValidator.requireRole(user, Role.COURIER, "go online");
        Courier courier = findCourier(user.getAccountId());
        if (courier.getStatus() != CourierStatus.OFFLINE) {
            throw new OrderRuleViolationException("You are already online or have an active order.");
        }
        courier.goOnline();
        courierRepository.save(courier);
        eventManager.logEvent("Courier #" + courier.getAccountId() + ": OFFLINE -> IDLE (on duty)");
        notifyCourierIdle(courier);
    }

    @Transactional
    public void goOffDuty(User user) {
        roleValidator.requireRole(user, Role.COURIER, "go offline");
        Courier courier = findCourier(user.getAccountId());
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
        readyOrders.forEach(order -> withdrawOffer(order, courier, false));
    }

    @Transactional
    public OrderResult acceptOffer(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "accept orders");
        Courier courier = findCourier(user.getAccountId());
        Order order = findOfferedOrder(orderId, courier);

        order.acceptCourierAssignment();
        orderRepository.save(order);
        declinedBy.remove(orderId);

        Long eventId = eventManager.logEvent("Order #" + orderId + " accepted by courier #" + courier.getAccountId());
        return new OrderResult("Order #" + orderId + " accepted", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult declineOffer(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "decline orders");
        Courier courier = findCourier(user.getAccountId());
        Order order = findOfferedOrder(orderId, courier);

        courier.declineOrder();
        courierRepository.save(courier);
        String outcome = withdrawOffer(order, courier);
        notifyCourierIdle(courier);

        Long eventId = eventManager.logEvent("Order #" + orderId + " declined by courier #" + courier.getAccountId()
                + "; " + outcome);
        return new OrderResult("Order #" + orderId + " declined", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult cancelAcceptedAssignment(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "cancel an accepted delivery");
        Courier courier = findCourier(user.getAccountId());
        Order order = findAssignedOrder(orderId, courier);
        if (courier.getStatus() != CourierStatus.ASSIGNED || order.getStatus() != OrderStatus.READY
                || !order.isCourierAccepted()) {
            throw new OrderRuleViolationException("Order #" + orderId
                    + " can only be canceled before pickup and after acceptance");
        }

        courier.cancelAssignment();
        courierRepository.save(courier);
        String outcome = withdrawOffer(order, courier);
        Long eventId = eventManager.logEvent("Courier #" + courier.getAccountId()
                + " canceled the accepted pickup for order #" + orderId + "; " + outcome);
        notifyCourierIdle(courier);
        return new OrderResult("Pickup for order #" + orderId + " canceled; " + outcome,
                eventId, OrderData.from(order));
    }

    public boolean hasPendingOffer(Courier courier) {
        return courier.getStatus() == CourierStatus.IDLE
                && orderRepository.existsByCourier_AccountIdAndStatus(courier.getAccountId(), OrderStatus.READY);
    }

    public boolean hasDeclined(Long orderId, Long courierId) {
        return declinedBy.getOrDefault(orderId, Set.of()).contains(courierId);
    }

    public void resetSimulationState() {
        declinedBy.clear();
    }

    private String withdrawOffer(Order order, Courier courier) {
        return withdrawOffer(order, courier, true);
    }

    private String withdrawOffer(Order order, Courier courier, boolean markDeclined) {
        if (markDeclined) {
            declinedBy.computeIfAbsent(order.getOrderId(), id -> ConcurrentHashMap.newKeySet())
                    .add(courier.getAccountId());
        }
        order.setCourier(null);
        orderRepository.save(order);

        Courier next = findClosestCourier(order.getStore(), order.getOrderId());
        if (next == null) {
            return "no other idle courier, order waits in the dispatch queue";
        }
        assign(order, next);
        return "offered to courier #" + next.getAccountId();
    }

    private Order findOfferedOrder(Long orderId, Courier courier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
        if (order.getCourier() == null || !order.getCourier().getAccountId().equals(courier.getAccountId())) {
            throw new PermissionDeniedException("Order #" + orderId + " was not offered to you");
        }
        if (order.getStatus() != OrderStatus.READY || courier.getStatus() != CourierStatus.ASSIGNED
                || order.isCourierAccepted()) {
            throw new OrderRuleViolationException("Order #" + orderId + " has no pending offer (order " + order.getStatus()
                    + ", courier " + courier.getStatus() + ")");
        }
        return order;
    }

    private void assign(Order order, Courier courier) {
        if (order.getStatus() != OrderStatus.READY) {
            throw new OrderRuleViolationException("Order #" + order.getOrderId()
                    + " must be ready before it can be assigned to a courier.");
        }
        if (courier.getStatus() != CourierStatus.IDLE) {
            throw new OrderRuleViolationException("Courier #" + courier.getAccountId() + " is "
                    + courier.getStatus() + ". Only idle couriers can be assigned an order.");
        }
        if (hasPendingOffer(courier)) {
            throw new OrderRuleViolationException("Courier #" + courier.getAccountId()
                    + " already has an assigned order offer.");
        }

        Courier oldCourier = order.getCourier();
        if (oldCourier != null && oldCourier != courier) {
            if (oldCourier.getStatus() == CourierStatus.ASSIGNED) {
                oldCourier.cancelAssignment();
                courierRepository.save(oldCourier);
            } else if (oldCourier.getStatus() != CourierStatus.IDLE) {
                throw new OrderRuleViolationException("Order #" + order.getOrderId()
                        + " cannot be reassigned while its courier is " + oldCourier.getStatus());
            }
        }
        order.setCourier(courier);
        orderRepository.save(order);
        courier.assignOrder();
        courierRepository.save(courier);
        if (oldCourier != null && oldCourier != courier) {
            notifyCourierIdle(oldCourier);
        }
    }

    @Transactional
    public OrderResult confirmPickup(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "confirm order pickups");
        Courier courier = findCourier(user.getAccountId());
        Order order = findAssignedOrder(orderId, courier);
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
        roleValidator.requireRole(user, Role.COURIER, "confirm order deliveries");
        Courier courier = findCourier(user.getAccountId());
        Order order = findAssignedOrder(orderId, courier);
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
        notifyCourierIdle(courier);
        return new OrderResult("Order #" + orderId + " marked delivered", eventId, OrderData.from(order));
    }

    private Order findAssignedOrder(Long orderId, Courier courier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order with id " + orderId + " not found"));
        if (order.getCourier() == null || !order.getCourier().getAccountId().equals(courier.getAccountId())) {
            throw new PermissionDeniedException("Order #" + orderId + " is not assigned to you");
        }
        return order;
    }

    @Transactional
    public void releaseCourier(Courier courier) {
        courier.setStatus(CourierStatus.IDLE);
        courierRepository.save(courier);
        eventManager.logEvent("Courier #" + courier.getAccountId() + " released -> IDLE");
        notifyCourierIdle(courier);
    }

    @Transactional
    public void offerWaitingOrder(Order order, Courier courier) {
        assign(order, courier);
        eventManager.logEvent("Order #" + order.getOrderId() + " taken from the dispatch queue and offered to courier #"
                + courier.getAccountId());
    }

    private Courier findCourier(Long courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("Courier with id " + courierId + " not found"));
    }

    @Transactional
    public OrderResult assignOrderToClosestCourier(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new NotFoundException("Order with id " + orderId + " not found");
        }
        GroceryStore store = order.getStore();
        Courier closetIdleCourier = findClosestCourier(store, orderId);
        if (closetIdleCourier == null || closetIdleCourier.getStatus() != CourierStatus.IDLE) {
            Long eventId = eventManager.logEvent("Order #"+orderId+" Preparing -> Ready. No idle courier available");
            return new OrderResult("Order #"+orderId+" Preparing -> Ready. No idle courier available", eventId, OrderData.from(order));
        }
        assign(order, closetIdleCourier);

        Long eventId = eventManager.logEvent("Order #"+orderId+" Preparing -> Ready. Order offered to courrier #"+closetIdleCourier.getAccountId());
        return new OrderResult("Order offered to courier", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult assignOrderToCourier(User user, Long orderId, Long courierId) {
        roleValidator.requireRole(user, Role.OPERATOR, "manually assign order to courier");

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new NotFoundException("Order with id " + orderId + " not found");
        }

        Courier courier = courierRepository.findById(courierId).orElse(null);
        if (courier == null) {
            throw new NotFoundException("Courier with id " + courierId + " not found");
        }

        assign(order, courier);

        Long eventId = eventManager.logEvent(String.format("Order #%d manually offered to courier #%d", orderId, courierId));
        return new OrderResult("Order manually offered to courier", eventId, OrderData.from(order));
    }

    private Courier findClosestCourier(GroceryStore store, Long orderId) {
        List<Courier> idleCouriers = courierRepository.findByStatusIn(List.of(CourierStatus.IDLE));
        if (idleCouriers.isEmpty()) {
            return null;
        }

        return idleCouriers.stream()
                .filter(c -> !hasPendingOffer(c) && !hasDeclined(orderId, c.getAccountId()))
                .min(Comparator
                        .comparingDouble((Courier c) -> getDistanceKm(c, store))
                        .thenComparing(Courier::getAccountId))
                .orElse(null);
    }

    //compute the distance in Km between a Courier and a Customer OR a GroceryStore
    private double getDistanceKm(Courier courier, User user) {
        if (courier == null || user == null) {
            throw new IllegalArgumentException("Courier or User is null");
        }

        double userLat;
        double userLon;

        if (user instanceof GroceryStore store) {
            userLat = store.getLatitude();
            userLon = store.getLongitude();
        } else if (user instanceof Customer customer) {
            userLat = customer.getLatitude();
            userLon = customer.getLongitude();
        } else {
            throw new IllegalArgumentException("Unsupported user type: " + user.getClass().getSimpleName());
        }

        return haversineKm(courier.getLatitude(), courier.getLongitude(), userLat, userLon);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
