package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DLVModule {

    private final CourierStatusService courierStatusService;
    private final CourierDispatchService courierDispatchService;
    private final DeliveryService deliveryService;

    public DLVModule(CourierStatusService courierStatusService,
                     CourierDispatchService courierDispatchService,
                     DeliveryService deliveryService) {
        this.courierStatusService = courierStatusService;
        this.courierDispatchService = courierDispatchService;
        this.deliveryService = deliveryService;
    }

    public void subscribe(CourierStatusObserver observer) {
        courierStatusService.subscribe(observer);
    }

    public void unsubscribe(CourierStatusObserver observer) {
        courierStatusService.unsubscribe(observer);
    }

    public List<OrderData> getPendingOffers(User user) {
        return courierDispatchService.getPendingOffers(user);
    }

    public CourierDashboardData getDashboard(User user) {
        return courierStatusService.getDashboard(user);
    }

    public void goOnDuty(User user) {
        courierStatusService.goOnDuty(user);
    }

    public void goOffDuty(User user) {
        courierStatusService.goOffDuty(user);
    }

    public OrderResult acceptOffer(User user, Long orderId) {
        return courierDispatchService.acceptOffer(user, orderId);
    }

    public OrderResult declineOffer(User user, Long orderId) {
        return courierDispatchService.declineOffer(user, orderId);
    }

    public OrderResult cancelAcceptedAssignment(User user, Long orderId) {
        return courierDispatchService.cancelAcceptedAssignment(user, orderId);
    }

    public boolean hasPendingOffer(Courier courier) {
        return courierDispatchService.hasPendingOffer(courier);
    }

    public boolean hasDeclined(Long orderId, Long courierId) {
        return courierDispatchService.hasDeclined(orderId, courierId);
    }

    public void resetSimulationState() {
        courierDispatchService.resetSimulationState();
    }

    public void assignByOperator(Order order, Courier courier) {
        courierDispatchService.assignByOperator(order, courier);
    }

    public OrderResult confirmPickup(User user, Long orderId) {
        return deliveryService.confirmPickup(user, orderId);
    }

    public OrderResult confirmDelivery(User user, Long orderId) {
        return deliveryService.confirmDelivery(user, orderId);
    }

    public void releaseCourier(Courier courier) {
        courierStatusService.releaseCourier(courier);
    }

    public void offerWaitingOrder(Order order, Courier courier) {
        courierDispatchService.offerWaitingOrder(order, courier);
    }

    public OrderResult assignOrderToClosestCourier(Long orderId) {
        return courierDispatchService.assignOrderToClosestCourier(orderId);
    }

    public OrderResult assignOrderToCourier(User user, Long orderId, Long courierId) {
        return courierDispatchService.assignOrderToCourier(user, orderId, courierId);
    }
}
