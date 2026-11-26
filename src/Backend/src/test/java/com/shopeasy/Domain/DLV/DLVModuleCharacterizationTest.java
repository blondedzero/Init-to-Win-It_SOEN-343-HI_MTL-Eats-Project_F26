package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Characterization tests for the current DLVModule behaviour.
 * These tests deliberately exercise the public module API and its observable
 * effects on Courier/Order state, repository calls, events, and observers.
 */
@ExtendWith(MockitoExtension.class)
class DLVModuleCharacterizationTest {

    @Mock private CourierRepository courierRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private EventManager eventManager;
    @Mock private RoleValidator roleValidator;

    @InjectMocks private DLVModule module;

    private Customer customer;
    private GroceryStore store;

    @BeforeEach
    void setUp() {
        customer = new Customer("customer@test.com", "hash", "Casey", "Customer",
                "10 Main St", -73.60, 45.50);
        store = new GroceryStore("store@test.com", "hash", "Sam", "Store",
                "20 Main St", -73.55, 45.52, "Test Store");
        lenient().when(eventManager.logEvent(anyString())).thenReturn(700L);
    }

    @Test
    void goOnDutyChangesOfflineCourierToIdleAndNotifiesObservers() {
        Courier courier = courier(1L, CourierStatus.OFFLINE, -73.60, 45.50);
        CourierStatusObserver observer = mock(CourierStatusObserver.class);
        module.subscribe(observer);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));

        module.goOnDuty(courier);

        assertEquals(CourierStatus.IDLE, courier.getStatus());
        verify(courierRepository).save(courier);
        verify(observer).onCourierIdle(courier);
        verify(eventManager).logEvent(contains("OFFLINE -> IDLE"));
    }

    @Test
    void goOnDutyRejectsCourierWhoIsAlreadyOnline() {
        Courier courier = courier(1L, CourierStatus.IDLE, -73.60, 45.50);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));

        assertThrows(OrderRuleViolationException.class, () -> module.goOnDuty(courier));
        verify(courierRepository, never()).save(any());
    }

    @Test
    void pendingOffersReturnsOnlyReadyOrdersNotYetAccepted() {
        Courier courier = courier(1L, CourierStatus.IDLE, -73.60, 45.50);
        Order pending = readyOrder(10L);
        pending.setCourier(courier);
        Order accepted = readyOrder(11L);
        accepted.setCourier(courier);
        accepted.acceptCourierAssignment();
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findByCourier_AccountIdAndStatus(1L, OrderStatus.READY))
                .thenReturn(List.of(pending, accepted));

        var offers = module.getPendingOffers(courier);

        assertEquals(1, offers.size());
        assertEquals(10L, offers.get(0).orderId());
    }

    @Test
    void acceptOfferMovesCourierToAssignedAndMarksOrderAccepted() {
        Courier courier = courier(1L, CourierStatus.IDLE, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResult result = module.acceptOffer(courier, 10L);

        assertEquals(CourierStatus.ASSIGNED, courier.getStatus());
        assertTrue(order.isCourierAccepted());
        assertEquals("Order #10 accepted", result.message());
        assertEquals(700L, result.eventId());
        verify(courierRepository).save(courier);
        verify(orderRepository).save(order);
        verify(eventManager).logEvent(contains("accepted by courier #1"));
    }

    @Test
    void acceptOfferRejectsOrderNotOfferedToThatCourier() {
        Courier courier = courier(1L, CourierStatus.IDLE, -73.60, 45.50);
        Courier otherCourier = courier(2L, CourierStatus.IDLE, -73.61, 45.51);
        Order order = readyOrder(10L);
        order.setCourier(otherCourier);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(PermissionDeniedException.class, () -> module.acceptOffer(courier, 10L));
        assertEquals(CourierStatus.IDLE, courier.getStatus());
        verify(orderRepository, never()).save(order);
    }

    @Test
    void declineOfferRemovesOfferAndRecordsDeclinedCourier() {
        Courier courier = courier(1L, CourierStatus.IDLE, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findByStatusIn(List.of(CourierStatus.IDLE))).thenReturn(List.of());

        module.declineOffer(courier, 10L);

        assertNull(order.getCourier());
        assertTrue(module.hasDeclined(10L, 1L));
        assertEquals(CourierStatus.IDLE, courier.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void acceptedCourierCannotGoOffDuty() {
        Courier courier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        order.acceptCourierAssignment();
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findByCourier_AccountIdAndStatus(1L, OrderStatus.READY))
                .thenReturn(List.of(order));

        assertThrows(OrderRuleViolationException.class, () -> module.goOffDuty(courier));
        assertEquals(CourierStatus.ASSIGNED, courier.getStatus());
        assertSame(courier, order.getCourier());
    }

    @Test
    void courierWithUnacceptedOfferCanGoOffDutyAndOfferIsWithdrawn() {
        Courier courier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findByCourier_AccountIdAndStatus(1L, OrderStatus.READY))
                .thenReturn(List.of(order));
        when(courierRepository.findByStatusIn(List.of(CourierStatus.IDLE))).thenReturn(List.of());

        module.goOffDuty(courier);

        assertEquals(CourierStatus.OFFLINE, courier.getStatus());
        assertNull(order.getCourier());
        verify(courierRepository).save(courier);
        verify(orderRepository).save(order);
    }

    @Test
    void carryingCourierCannotGoOffDuty() {
        Courier courier = courier(1L, CourierStatus.CARRYING, -73.60, 45.50);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findByCourier_AccountIdAndStatus(1L, OrderStatus.READY)).thenReturn(List.of());

        assertThrows(OrderRuleViolationException.class, () -> module.goOffDuty(courier));
        assertEquals(CourierStatus.CARRYING, courier.getStatus());
    }

    @Test
    void cancelAcceptedAssignmentReleasesCourierAndWithdrawsOrderOffer() {
        Courier courier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        order.acceptCourierAssignment();
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findByStatusIn(List.of(CourierStatus.IDLE))).thenReturn(List.of());

        OrderResult result = module.cancelAcceptedAssignment(courier, 10L);

        assertEquals(CourierStatus.IDLE, courier.getStatus());
        assertNull(order.getCourier());
        assertFalse(order.isCourierAccepted());
        assertTrue(result.message().contains("Pickup for order #10 canceled"));
        verify(courierRepository).save(courier);
        verify(orderRepository).save(order);
    }

    @Test
    void confirmPickupMovesOrderToPickedUpAndCourierToCarrying() {
        Courier courier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        order.acceptCourierAssignment();
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResult result = module.confirmPickup(courier, 10L);

        assertEquals(OrderStatus.PICKED_UP, order.getStatus());
        assertEquals(CourierStatus.CARRYING, courier.getStatus());
        assertEquals("Order #10 marked picked up", result.message());
        verify(orderRepository).save(order);
        verify(courierRepository).save(courier);
    }

    @Test
    void pickupIsRejectedUntilCourierAcceptsOffer() {
        Courier courier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderRuleViolationException.class, () -> module.confirmPickup(courier, 10L));
        assertEquals(OrderStatus.READY, order.getStatus());
        assertEquals(CourierStatus.ASSIGNED, courier.getStatus());
    }

    @Test
    void confirmDeliveryMovesOrderToDeliveredAndCourierBackToIdle() {
        Courier courier = courier(1L, CourierStatus.CARRYING, -73.60, 45.50);
        Order order = readyOrder(10L);
        order.setCourier(courier);
        order.markPickedUp();
        CourierStatusObserver observer = mock(CourierStatusObserver.class);
        module.subscribe(observer);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResult result = module.confirmDelivery(courier, 10L);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(CourierStatus.IDLE, courier.getStatus());
        assertEquals("Order #10 marked delivered", result.message());
        verify(observer).onCourierIdle(courier);
        verify(orderRepository).save(order);
        verify(courierRepository).save(courier);
    }

    @Test
    void automaticAssignmentChoosesNearestEligibleIdleCourier() {
        Courier farther = courier(2L, CourierStatus.IDLE, -73.70, 45.60);
        Courier nearer = courier(1L, CourierStatus.IDLE, -73.551, 45.521);
        Order order = readyOrder(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findByStatusIn(List.of(CourierStatus.IDLE)))
                .thenReturn(List.of(farther, nearer));
        when(orderRepository.existsByCourier_AccountIdAndStatus(anyLong(), eq(OrderStatus.READY)))
                .thenReturn(false);

        module.assignOrderToClosestCourier(10L);

        assertSame(nearer, order.getCourier());
        verify(orderRepository).save(order);
    }

    @Test
    void automaticAssignmentLeavesOrderUnassignedWhenNoCourierIsAvailable() {
        Order order = readyOrder(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findByStatusIn(List.of(CourierStatus.IDLE))).thenReturn(List.of());

        OrderResult result = module.assignOrderToClosestCourier(10L);

        assertNull(order.getCourier());
        assertTrue(result.message().contains("No idle courier available"));
        verify(orderRepository, never()).save(order);
    }

    @Test
    void operatorCanReassignReadyOrderFromAnAssignedCourier() {
        Operator operator = new Operator("operator@test.com", "hash", "Ollie", "Operator");
        Courier previousCourier = courier(1L, CourierStatus.ASSIGNED, -73.60, 45.50);
        Courier nextCourier = courier(2L, CourierStatus.IDLE, -73.55, 45.52);
        Order order = readyOrder(10L);
        order.setCourier(previousCourier);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findById(2L)).thenReturn(Optional.of(nextCourier));
        when(orderRepository.existsByCourier_AccountIdAndStatus(2L, OrderStatus.READY)).thenReturn(false);

        module.assignOrderToCourier(operator, 10L, 2L);

        assertSame(nextCourier, order.getCourier());
        assertEquals(CourierStatus.IDLE, previousCourier.getStatus());
        verify(courierRepository).save(previousCourier);
        verify(orderRepository).save(order);
    }

    @Test
    void manualAssignmentRejectsCourierWhoIsNotIdle() {
        Operator operator = new Operator("operator@test.com", "hash", "Ollie", "Operator");
        Courier carrying = courier(2L, CourierStatus.CARRYING, -73.60, 45.50);
        Order order = readyOrder(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(courierRepository.findById(2L)).thenReturn(Optional.of(carrying));

        assertThrows(OrderRuleViolationException.class,
                () -> module.assignOrderToCourier(operator, 10L, 2L));
        assertNull(order.getCourier());
    }

    @Test
    void observerCanBeUnsubscribed() {
        Courier courier = courier(1L, CourierStatus.OFFLINE, -73.60, 45.50);
        CourierStatusObserver observer = mock(CourierStatusObserver.class);
        module.subscribe(observer);
        module.unsubscribe(observer);
        when(courierRepository.findById(1L)).thenReturn(Optional.of(courier));

        module.goOnDuty(courier);

        verify(observer, never()).onCourierIdle(any());
    }

    private Courier courier(long id, CourierStatus status, double longitude, double latitude) {
        Courier courier = spy(new Courier("courier" + id + "@test.com", "hash", "Chris", "Courier",
                longitude, latitude));
        lenient().doReturn(id).when(courier).getAccountId();
        courier.setStatus(status);
        return courier;
    }

    private Order readyOrder(long id) {
        Order order = spy(new Order(customer, store));
        lenient().doReturn(id).when(order).getOrderId();
        order.markAccepted();
        order.markPreparing();
        order.markReady();
        return order;
    }
}
