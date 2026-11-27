package com.shopeasy.Domain.ORD;

import com.shopeasy.Domain.DLV.DLVModule;
import com.shopeasy.Domain.DLV.DispatchQueue;
import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.PRC.MockPaymentProcessor;
import com.shopeasy.Domain.PRC.PRCModule;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.PaymentInfoRepository;
import com.shopeasy.Foundation.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Characterisation tests for {@link ORDModule#cancelOrder(User, Long)}.
 *
 * <p>These tests pin down what cancelOrder does TODAY (tag phase3), including behaviour
 * that may look questionable (e.g. stock restored for a picked-up order, no refund on cancel).
 * They are not a specification. They must pass unchanged before and after the refactoring.
 *
 * <p>Only repositories and other modules are mocked. RoleValidator is the real class because
 * its refusal text is part of the behaviour being pinned. Order is the real entity so that
 * markCancelled() and OrderData.from() run as in production.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ORDModuleCancelOrderCharacterizationTest {

    private static final Long ORDER_ID = 42L;
    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long STORE_ID = 10L;
    private static final Long OTHER_STORE_ID = 11L;
    private static final Long OPERATOR_ID = 99L;
    private static final Long EVENT_ID = 500L;

    @Mock OrderRepository orderRepository;
    @Mock GroceryStoreRepository groceryStoreRepository;
    @Mock ProductRepository productRepository;
    @Mock PaymentInfoRepository paymentInfoRepository;
    @Mock MockPaymentProcessor paymentProcessor;
    @Mock EventManager eventManager;
    @Mock DLVModule dlvModule;
    @Mock DispatchQueue dispatchQueue;
    @Mock PRCModule prcModule;

    // Real validator (its constructor is protected, hence the anonymous subclass).
    private final RoleValidator roleValidator = new RoleValidator() {};

    private ORDModule module;

    // The parties of the order under test.
    private Customer ownerCustomer;
    private GroceryStore ownerStore;
    // Users who are NOT parties of the order.
    private Customer otherCustomer;
    private GroceryStore otherStore;
    private User operator;

    // Two order lines, so "every line is restocked" is actually tested.
    private Product productA;
    private Product productB;

    // Order status observed at the moment each collaborator is called.
    private final List<OrderStatus> statusWhenSaved = new ArrayList<>();
    private final List<OrderStatus> statusWhenRestocked = new ArrayList<>();
    private final List<OrderStatus> statusWhenQueueSeeded = new ArrayList<>();

    @BeforeEach
    void setUp() {
        module = new ORDModule(orderRepository, groceryStoreRepository, productRepository,
                paymentInfoRepository, paymentProcessor, eventManager, roleValidator,
                dlvModule, dispatchQueue, prcModule);

        ownerCustomer = customer(CUSTOMER_ID);
        otherCustomer = customer(OTHER_CUSTOMER_ID);
        ownerStore = store(STORE_ID);
        otherStore = store(OTHER_STORE_ID);
        operator = user(Role.OPERATOR, OPERATOR_ID);

        productA = mock(Product.class);
        productB = mock(Product.class);

        when(eventManager.logEvent(anyString())).thenReturn(EVENT_ID);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            statusWhenSaved.add(saved.getStatus());
            return saved;
        });
        when(orderRepository.findAllById(any())).thenReturn(List.of());
    }

    // ------------------------------------------------------------------ fixtures

    private static Customer customer(Long id) {
        Customer c = mock(Customer.class);
        when(c.getRole()).thenReturn(Role.CUSTOMER);
        when(c.getAccountId()).thenReturn(id);
        when(c.getAddress()).thenReturn("1 Test Street");
        return c;
    }

    private static GroceryStore store(Long id) {
        GroceryStore s = mock(GroceryStore.class);
        when(s.getRole()).thenReturn(Role.STORE);
        when(s.getAccountId()).thenReturn(id);
        when(s.getStoreName()).thenReturn("Store " + id);
        return s;
    }

    private static User user(Role role, Long id) {
        User u = mock(User.class);
        when(u.getRole()).thenReturn(role);
        when(u.getAccountId()).thenReturn(id);
        return u;
    }

    /** The order's own customer, own store, or an operator. */
    private User partyFor(Role role) {
        return switch (role) {
            case CUSTOMER -> ownerCustomer;
            case STORE -> ownerStore;
            case OPERATOR -> operator;
            default -> throw new IllegalArgumentException(role.name());
        };
    }

    /** A persisted order #42 placed by ownerCustomer at ownerStore: 3 x productA, 1 x productB. */
    private Order existingOrder(OrderStatus status) {
        Order order = new Order(ownerCustomer, ownerStore);
        ReflectionTestUtils.setField(order, "orderId", ORDER_ID);
        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "items", new ArrayList<>(List.of(
                line(productA, 3), line(productB, 1))));
        order.setIsCharged(true);

        doAnswer(inv -> { statusWhenRestocked.add(order.getStatus()); return null; })
                .when(productA).addToStock(anyInt());
        doAnswer(inv -> { statusWhenRestocked.add(order.getStatus()); return null; })
                .when(productB).addToStock(anyInt());
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        // Seed used by queueFor(storeId) the first time the store's preparation queue is touched.
        when(orderRepository.findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(
                STORE_ID, List.of(OrderStatus.ACCEPTED))).thenAnswer(inv -> {
                    statusWhenQueueSeeded.add(order.getStatus());
                    return order.getStatus() == OrderStatus.ACCEPTED ? List.of(order) : List.of();
                });
        return order;
    }

    private static OrderItem line(Product product, int quantity) {
        OrderItem item = mock(OrderItem.class);
        when(item.getProduct()).thenReturn(product);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    private static Courier courier(CourierStatus status) {
        Courier c = mock(Courier.class);
        when(c.getStatus()).thenReturn(status);
        return c;
    }

    /** After a refusal: the order is untouched and no collaborator was asked to change anything. */
    private void assertNothingChanged(Order order, OrderStatus originalStatus) {
        assertEquals(originalStatus, order.getStatus());
        assertNull(order.getCancelledAt());
        verify(productA, never()).addToStock(anyInt());
        verify(productB, never()).addToStock(anyInt());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventManager, dlvModule, dispatchQueue, paymentProcessor);
    }

    // ================================================================== refusals, in check order

    @Nested
    @DisplayName("1. Role check (first)")
    class RoleCheck {

        @ParameterizedTest(name = "{0} is refused")
        @EnumSource(value = Role.class, names = {"OPERATOR", "CUSTOMER", "STORE"},
                mode = EnumSource.Mode.EXCLUDE)
        void otherRolesAreRefusedBeforeTheOrderIsLoaded(Role role) {
            User u = user(role, 77L);

            PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                    () -> module.cancelOrder(u, ORDER_ID));

            assertEquals("A " + role.name().toLowerCase() + " account cannot cancel orders", e.getMessage());
            verifyNoInteractions(orderRepository, eventManager, dlvModule, dispatchQueue, paymentProcessor);
        }
    }

    @Nested
    @DisplayName("2. Order lookup (second)")
    class Lookup {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Role.class, names = {"OPERATOR", "CUSTOMER", "STORE"})
        void unknownOrderIsNotFound(Role role) {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            NotFoundException e = assertThrows(NotFoundException.class,
                    () -> module.cancelOrder(partyFor(role), ORDER_ID));

            assertEquals("Order 42 does not exist", e.getMessage());
            verify(orderRepository, never()).save(any());
            verifyNoInteractions(eventManager, dlvModule, dispatchQueue, paymentProcessor);
        }
    }

    @Nested
    @DisplayName("3. Customer rules")
    class CustomerRules {

        @Test
        void customerCannotCancelAnotherCustomersOrder() {
            Order order = existingOrder(OrderStatus.PLACED);

            PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                    () -> module.cancelOrder(otherCustomer, ORDER_ID));

            assertEquals("Customers can only cancel their own orders", e.getMessage());
            assertNothingChanged(order, OrderStatus.PLACED);
        }

        @Test
        void ownershipIsCheckedBeforeStatusForCustomers() {
            // Not the owner AND not placed: the ownership message wins.
            Order order = existingOrder(OrderStatus.DELIVERED);

            PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                    () -> module.cancelOrder(otherCustomer, ORDER_ID));

            assertEquals("Customers can only cancel their own orders", e.getMessage());
            assertNothingChanged(order, OrderStatus.DELIVERED);
        }

        /*
         * Includes the finished statuses (CANCELED, REJECTED, DELIVERED, REFUNDED): a customer gets
         * THIS message for them, not "cannot be canceled because it is ...", because the customer
         * status check runs before the finished-order check. Status is printed in UPPER case
         * (enum toString), unlike the finished-order message.
         */
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, names = "PLACED", mode = EnumSource.Mode.EXCLUDE)
        void ownerCanOnlyCancelWhilePlaced(OrderStatus status) {
            Order order = existingOrder(status);

            OrderRuleViolationException e = assertThrows(OrderRuleViolationException.class,
                    () -> module.cancelOrder(ownerCustomer, ORDER_ID));

            assertEquals("Order #42 is " + status.name() + " and can no longer be canceled by the customer",
                    e.getMessage());
            assertNothingChanged(order, status);
        }

        @Test
        void ownerCanCancelPlacedOrder() {
            Order order = existingOrder(OrderStatus.PLACED);

            module.cancelOrder(ownerCustomer, ORDER_ID);

            assertEquals(OrderStatus.CANCELED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("4. Store rules")
    class StoreRules {

        @Test
        void storeCannotCancelAnotherStoresOrder() {
            Order order = existingOrder(OrderStatus.PLACED);

            PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                    () -> module.cancelOrder(otherStore, ORDER_ID));

            assertEquals("Stores can only cancel their own orders", e.getMessage());
            assertNothingChanged(order, OrderStatus.PLACED);
        }

        @Test
        void ownershipIsCheckedBeforeFinishedStatusForStores() {
            Order order = existingOrder(OrderStatus.DELIVERED);

            PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                    () -> module.cancelOrder(otherStore, ORDER_ID));

            assertEquals("Stores can only cancel their own orders", e.getMessage());
            assertNothingChanged(order, OrderStatus.DELIVERED);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, names = {"PLACED", "ACCEPTED", "PREPARING", "READY", "PICKED_UP"})
        void ownerStoreCanCancelAnyUnfinishedOrder(OrderStatus status) {
            Order order = existingOrder(status);

            module.cancelOrder(ownerStore, ORDER_ID);

            assertEquals(OrderStatus.CANCELED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("5. Operator rules (no ownership check)")
    class OperatorRules {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, names = {"PLACED", "ACCEPTED", "PREPARING", "READY", "PICKED_UP"})
        void operatorCanCancelAnyUnfinishedOrderOfAnyStore(OrderStatus status) {
            Order order = existingOrder(status);

            module.cancelOrder(operator, ORDER_ID);

            assertEquals(OrderStatus.CANCELED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("6. Finished-order check (last refusal)")
    class FinishedOrders {

        // Status printed in lower case here, unlike the customer message.
        @ParameterizedTest(name = "{0} on {1}")
        @CsvSource({
                "OPERATOR, CANCELED,  canceled",
                "OPERATOR, REJECTED,  rejected",
                "OPERATOR, DELIVERED, delivered",
                "OPERATOR, REFUNDED,  refunded",
                "STORE,    CANCELED,  canceled",
                "STORE,    REJECTED,  rejected",
                "STORE,    DELIVERED, delivered",
                "STORE,    REFUNDED,  refunded"
        })
        void finishedOrdersCannotBeCanceled(Role role, OrderStatus status, String shown) {
            Order order = existingOrder(status);

            OrderRuleViolationException e = assertThrows(OrderRuleViolationException.class,
                    () -> module.cancelOrder(partyFor(role), ORDER_ID));

            assertEquals("Order #42 cannot be canceled because it is " + shown, e.getMessage());
            assertNothingChanged(order, status);
        }
    }

    // ================================================================== side effects of a cancel

    @Nested
    @DisplayName("7. Side effects")
    class SideEffects {

        @Test
        void fullSequenceForAcceptedOrderWithAssignedCourier() {
            Order order = existingOrder(OrderStatus.ACCEPTED);
            Courier courier = courier(CourierStatus.ASSIGNED);
            order.setCourier(courier);

            OrderResult result = module.cancelOrder(ownerStore, ORDER_ID);

            // Order of effects: queue removal -> restock -> markCancelled -> save -> release courier -> log.
            InOrder inOrder = inOrder(orderRepository, productA, productB, dlvModule, eventManager);
            inOrder.verify(orderRepository).findById(ORDER_ID);
            inOrder.verify(orderRepository).findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(
                    STORE_ID, List.of(OrderStatus.ACCEPTED));
            inOrder.verify(productA).addToStock(3);
            inOrder.verify(productB).addToStock(1);
            inOrder.verify(orderRepository).save(order);
            inOrder.verify(dlvModule).releaseCourier(courier);
            inOrder.verify(eventManager).logEvent("Order 42: ACCEPTED -> CANCELED by store 10");
            // (No inOrder.verifyNoMoreInteractions(): OrderData.from may read product getters afterwards.)
            verify(orderRepository, times(1)).save(any());
            verifyNoMoreInteractions(dlvModule, eventManager);

            assertEquals(List.of(OrderStatus.ACCEPTED), statusWhenQueueSeeded, "queue touched before cancel");
            assertEquals(List.of(OrderStatus.ACCEPTED, OrderStatus.ACCEPTED), statusWhenRestocked,
                    "restocked before markCancelled");
            assertEquals(List.of(OrderStatus.CANCELED), statusWhenSaved, "saved after markCancelled");

            assertEquals(OrderStatus.CANCELED, order.getStatus());
            assertNotNull(order.getCancelledAt());
            // ASSUMPTION: OrderResult is a record with components message and eventId.
            assertEquals("Order #42 canceled", result.message());
            assertEquals(EVENT_ID, result.eventId());
        }

        @Test
        void acceptedOrderIsRemovedFromThePreparationQueue() {
            existingOrder(OrderStatus.ACCEPTED);

            module.cancelOrder(ownerStore, ORDER_ID);
            module.getPreparationQueue(ownerStore);

            // The queue was seeded with order 42 during the cancel, then 42 was removed:
            // the queue now asks the repository for no orders at all.
            verify(orderRepository).findAllById(argThat(ids -> !ids.iterator().hasNext()));
        }

        // Structural pin: the preparation queue is only touched for ACCEPTED orders.
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, names = {"PLACED", "PREPARING", "READY", "PICKED_UP"})
        void preparationQueueIsNotTouchedForOtherStatuses(OrderStatus status) {
            existingOrder(status);

            module.cancelOrder(operator, ORDER_ID);

            verify(orderRepository, never()).findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(any(), any());
        }

        // Pinned as is: stock comes back even for READY and PICKED_UP orders.
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, names = {"PLACED", "ACCEPTED", "PREPARING", "READY", "PICKED_UP"})
        void everyLineIsRestockedOnce(OrderStatus status) {
            existingOrder(status);

            module.cancelOrder(operator, ORDER_ID);

            verify(productA, times(1)).addToStock(3);
            verify(productB, times(1)).addToStock(1);
            verify(productA, times(1)).addToStock(anyInt());
            verify(productB, times(1)).addToStock(anyInt());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CourierStatus.class, names = {"ASSIGNED", "CARRYING"})
        void busyCourierIsReleased(CourierStatus courierStatus) {
            Order order = existingOrder(OrderStatus.READY);
            Courier courier = courier(courierStatus);
            order.setCourier(courier);

            module.cancelOrder(ownerStore, ORDER_ID);

            verify(dlvModule).releaseCourier(courier);
            verifyNoMoreInteractions(dlvModule);
            // ORDModule itself does not detach the courier from the order.
            assertSame(courier, order.getCourier());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = CourierStatus.class, names = {"ASSIGNED", "CARRYING"}, mode = EnumSource.Mode.EXCLUDE)
        void courierInAnyOtherStatusIsNotReleased(CourierStatus courierStatus) {
            Order order = existingOrder(OrderStatus.READY);
            order.setCourier(courier(courierStatus));

            module.cancelOrder(ownerStore, ORDER_ID);

            verifyNoInteractions(dlvModule);
        }

        @Test
        void noCourierMeansNoDeliveryInteraction() {
            existingOrder(OrderStatus.READY);

            module.cancelOrder(ownerStore, ORDER_ID);

            verifyNoInteractions(dlvModule, dispatchQueue);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "OPERATOR, PLACED,    Order 42: PLACED -> CANCELED by operator 99",
                "CUSTOMER, PLACED,    Order 42: PLACED -> CANCELED by customer 1",
                "STORE,    PICKED_UP, Order 42: PICKED_UP -> CANCELED by store 10"
        })
        void eventNamesPreviousStatusRoleAndAccount(Role role, OrderStatus status, String expected) {
            existingOrder(status);

            module.cancelOrder(partyFor(role), ORDER_ID);

            verify(eventManager, times(1)).logEvent(expected);
            verifyNoMoreInteractions(eventManager);
        }

        // Pinned as is: cancelling neither refunds nor touches the payment processor.
        @Test
        void cancelDoesNotRefund() {
            Order order = existingOrder(OrderStatus.PLACED);

            module.cancelOrder(ownerCustomer, ORDER_ID);

            verifyNoInteractions(paymentProcessor);
            assertEquals(OrderStatus.CANCELED, order.getStatus());
            assertTrue(order.getIsCharged());
            assertNull(order.getRefund());
        }

        @Test
        void orderIsSavedExactlyOnce() {
            Order order = existingOrder(OrderStatus.PLACED);

            module.cancelOrder(operator, ORDER_ID);

            verify(orderRepository, times(1)).save(order);
        }
    }
}
