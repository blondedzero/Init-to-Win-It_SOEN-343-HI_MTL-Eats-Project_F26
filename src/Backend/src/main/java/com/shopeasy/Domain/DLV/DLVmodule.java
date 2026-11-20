package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class DLVmodule {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;

    public DLVmodule(CourierRepository courierRepository, OrderRepository orderRepository, EventManager eventManager, RoleValidator roleValidator) {
        this.courierRepository = courierRepository;
        this.orderRepository = orderRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
    }

    @Transactional
    public OrderResult assignOrderToClosestCourier(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new NotFoundException("Order with id " + orderId + " not found");
        }
        GroceryStore store = order.getStore();
        Courier closetIdleCourier = findClosestCourier(store);
        if (closetIdleCourier == null || closetIdleCourier.getStatus() != CourierStatus.IDLE) {
            Long eventId = eventManager.logEvent("Order #"+orderId+" Preparing -> Ready. No idle courier available");
            return new OrderResult("Order #"+orderId+" Preparing -> Ready. No idle courier available", eventId, OrderData.from(order));
        }
        order.setCourier(closetIdleCourier);
        orderRepository.save(order);
        closetIdleCourier.setStatus(CourierStatus.ASSIGNED);
        courierRepository.save(closetIdleCourier);

        Long eventId = eventManager.logEvent("Order #"+orderId+" Preparing -> Ready. Order assigned to courrier #"+closetIdleCourier.getAccountId());
        return new OrderResult("Order assigned to courier", eventId, OrderData.from(order));
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

        if (courier.getStatus() != CourierStatus.IDLE) {
            throw new IllegalStateException("Cannot assign order to courier with status " + courier.getStatus());
        }

        order.setCourier(courier);
        orderRepository.save(order);
        courier.setStatus(CourierStatus.ASSIGNED);
        courierRepository.save(courier);

        Long eventId = eventManager.logEvent(String.format("Order #%d Assigned to courier", orderId));
        return new OrderResult("Order manually assigned to courier", eventId, OrderData.from(order));
    }

    private Courier findClosestCourier(GroceryStore store) {
        List<Courier> idleCouriers = courierRepository.findByStatusIn(List.of(CourierStatus.IDLE));
        if (idleCouriers.isEmpty()) {
            return null;
        }

        return idleCouriers.stream()
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
