package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
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

@Service
class CourierDispatchService {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;
    private final CourierOrderLookup courierOrderLookup;
    private final CourierStatusPublisher courierStatusPublisher;
    private final DistanceCalculator distanceCalculator;

    private final Map<Long, Set<Long>> declinedBy = new ConcurrentHashMap<>();

    CourierDispatchService(CourierRepository courierRepository, OrderRepository orderRepository,
                           EventManager eventManager, RoleValidator roleValidator,
                           CourierOrderLookup courierOrderLookup, CourierStatusPublisher courierStatusPublisher,
                           DistanceCalculator distanceCalculator) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
        this.courierOrderLookup = courierOrderLookup;
        this.courierStatusPublisher = courierStatusPublisher;
        this.distanceCalculator = distanceCalculator;
    }

    @Transactional(readOnly = true)
    public List<OrderData> getPendingOffers(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view delivery offers");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        return orderRepository.findByCourier_AccountIdAndStatus(courier.getAccountId(), OrderStatus.READY)
                .stream()
                .filter(order -> !order.isCourierAccepted())
                .map(OrderData::from)
                .toList();
    }

    @Transactional
    public OrderResult acceptOffer(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "accept orders");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        Order order = courierOrderLookup.findOfferedOrder(orderId, courier);

        courier.assignOrder();
        courierRepository.save(courier);
        order.acceptCourierAssignment();
        orderRepository.save(order);
        declinedBy.remove(orderId);

        Long eventId = eventManager.logEvent("Order #" + orderId + " accepted by courier #" + courier.getAccountId()
                + "; courier IDLE -> ASSIGNED");
        return new OrderResult("Order #" + orderId + " accepted", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult declineOffer(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "decline orders");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        Order order = courierOrderLookup.findOfferedOrder(orderId, courier);

        courier.declineOrder();
        courierRepository.save(courier);
        String outcome = withdrawOffer(order, courier);
        courierStatusPublisher.notifyCourierIdle(courier);

        Long eventId = eventManager.logEvent("Order #" + orderId + " declined by courier #" + courier.getAccountId()
                + "; " + outcome);
        return new OrderResult("Order #" + orderId + " declined", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult cancelAcceptedAssignment(User user, Long orderId) {
        roleValidator.requireRole(user, Role.COURIER, "cancel an accepted delivery");
        Courier courier = courierOrderLookup.findCourier(user.getAccountId());
        Order order = courierOrderLookup.findAssignedOrder(orderId, courier);
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
        courierStatusPublisher.notifyCourierIdle(courier);
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

    public String withdrawOffer(Order order, Courier courier, boolean markDeclined) {
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

    private String withdrawOffer(Order order, Courier courier) {
        return withdrawOffer(order, courier, true);
    }

    private Courier findClosestCourier(GroceryStore store, Long orderId) {
        List<Courier> idleCouriers = courierRepository.findByStatusIn(List.of(CourierStatus.IDLE));
        if (idleCouriers.isEmpty()) {
            return null;
        }

        return idleCouriers.stream()
                .filter(courier -> !hasPendingOffer(courier) && !hasDeclined(orderId, courier.getAccountId()))
                .min(Comparator
                        .comparingDouble((Courier courier) -> distanceCalculator.getDistanceKm(courier, store))
                        .thenComparing(Courier::getAccountId))
                .orElse(null);
    }

    private void assign(Order order, Courier courier) {
        if (order.getStatus() != OrderStatus.READY) {
            throw new OrderRuleViolationException("Order #" + order.getOrderId()
                    + " must be ready before it can be assigned to a courier.");
        }
        if (order.getCourier() != null) {
            throw new OrderRuleViolationException(
                    "Order #" + order.getOrderId() + " is already assigned."
            );
        }
        if (courier.getStatus() != CourierStatus.IDLE) {
            throw new OrderRuleViolationException("Courier #" + courier.getAccountId() + " is "
                    + courier.getStatus() + ". Only idle couriers can be assigned an order.");
        }
        if (hasPendingOffer(courier)) {
            throw new OrderRuleViolationException("Courier #" + courier.getAccountId()
                    + " already has an assigned order offer.");
        }

        order.setCourier(courier);
        orderRepository.save(order);
    }

    @Transactional
    public void assignByOperator(Order order, Courier courier) {
        if (order.getStatus() != OrderStatus.READY) {
            throw new OrderRuleViolationException("Order #" + order.getOrderId()
                    + " must be ready before it can be assigned to a courier.");
        }
        if (courier.getStatus() != CourierStatus.IDLE) {
            throw new OrderRuleViolationException("Courier #" + courier.getAccountId() + " is "
                    + courier.getStatus() + ". Only idle couriers can be assigned an order.");
        }
        if (order.getCourier() == courier) {
            throw new OrderRuleViolationException(
                    "Order #" + order.getOrderId() + " is already assigned to this courier."
            );
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
        if (oldCourier != null && oldCourier != courier) {
            courierStatusPublisher.notifyCourierIdle(oldCourier);
        }
    }

    @Transactional
    public void offerWaitingOrder(Order order, Courier courier) {
        assign(order, courier);
        eventManager.logEvent("Order #" + order.getOrderId() + " taken from the dispatch queue and offered to courier #"
                + courier.getAccountId());
    }

    @Transactional
    public OrderResult assignOrderToClosestCourier(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new NotFoundException("Order with id " + orderId + " not found");
        }
        GroceryStore store = order.getStore();
        Courier closestIdleCourier = findClosestCourier(store, orderId);
        if (closestIdleCourier == null || closestIdleCourier.getStatus() != CourierStatus.IDLE) {
            Long eventId = eventManager.logEvent("Order #" + orderId + " Preparing -> Ready. No idle courier available");
            return new OrderResult("Order #" + orderId + " Preparing -> Ready. No idle courier available",
                    eventId, OrderData.from(order));
        }
        assign(order, closestIdleCourier);

        Long eventId = eventManager.logEvent("Order #" + orderId + " Preparing -> Ready. Order offered to courrier #"
                + closestIdleCourier.getAccountId());
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

        assignByOperator(order, courier);

        Long eventId = eventManager.logEvent(String.format("Order #%d manually offered to courier #%d", orderId, courierId));
        return new OrderResult("Order manually offered to courier", eventId, OrderData.from(order));
    }
}
