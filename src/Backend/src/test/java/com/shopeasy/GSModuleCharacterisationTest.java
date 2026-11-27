package com.shopeasy;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.GS.GSModule;
import com.shopeasy.Domain.GS.ProductData;
import com.shopeasy.Domain.GS.StoreContextData;
import com.shopeasy.Domain.GS.StoreMenuData;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterisation tests for GSModule (Phase IV, step 2).
 *
 * These tests pin down the CURRENT behaviour of the methods we are about to
 * refactor. They must pass on the commit before each refactoring and on the
 * commit after it. They are not a specification of what the code should do;
 * they are a record of what it does today (see Feathers, ch. 13).
 *
 * Repositories and the event log are mocked; RoleValidator is the real one so
 * that the permission rules are part of the pinned behaviour.
 */
@ExtendWith(MockitoExtension.class)
class GSModuleCharacterisationTest {

    @Mock GroceryStoreRepository groceryStoreRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductCategoryRepository productCategoryRepository;
    @Mock OrderRepository orderRepository;
    @Mock EventManager eventManager;

    GSModule module;

    GroceryStore store;        // account 10
    GroceryStore otherStore;   // account 20
    User customer;             // account 30
    User courier;              // account 40
    User operator;             // account 50

    @BeforeEach
    void setUp() {
        RoleValidator roleValidator = new RoleValidator() {};   // protected constructor
        module = new GSModule(groceryStoreRepository, productRepository, productCategoryRepository,
                roleValidator, orderRepository, eventManager);

        store = storeWithId(10L, "Test Market", StoreStatus.CLOSED);
        otherStore = storeWithId(20L, "Other Market", StoreStatus.OPEN);
        customer = userWithId(30L, Role.CUSTOMER);
        courier = userWithId(40L, Role.COURIER);
        operator = userWithId(50L, Role.OPERATOR);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    static GroceryStore storeWithId(long id, String name, StoreStatus status) {
        GroceryStore s = new GroceryStore("s" + id + "@test.com", "hash", "First", "Last",
                "1 Rue Test", -73.0, 45.0, name);
        s.setStatus(status);
        ReflectionTestUtils.setField(s, "accountId", id);
        return s;
    }

    static User userWithId(long id, Role role) {
        User u = new User("u" + id + "@test.com", "hash", "First", "Last", role) {
            @Override public List<Order> getHistory() { return List.of(); }
        };
        ReflectionTestUtils.setField(u, "accountId", id);
        return u;
    }

    static Product product(GroceryStore s, long id, String name, ProductStatus status) {
        Product p = new Product(s, new BigDecimal("2.50"), name, "desc", 5, new ProductCategory("Produce"));
        p.setStatus(status);
        ReflectionTestUtils.setField(p, "productId", id);
        return p;
    }

    static StoreOrderCount count(long storeId, OrderStatus status, long n) {
        return new StoreOrderCount() {
            public Long getStoreId() { return storeId; }
            public OrderStatus getStatus() { return status; }
            public long getCount() { return n; }
        };
    }

    // ------------------------------------------------------------------
    // 5.5.2-5 store status: openStore / pauseStore / closeStore / changeStatus
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("openStore")
    class OpenStore {

        @Test
        void storeOpensItself_fromClosed_becomesOpen_andLogsEvent() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(0);

            StoreContextData result = module.openStore(store, 10L);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.OPEN);
            assertThat(result.status()).isEqualTo("OPEN");
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.ordersInProgress()).isZero();
            assertThat(result.ordersReady()).isZero();

            ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
            verify(eventManager).logEvent(event.capture());
            assertThat(event.getValue()).contains("Store 10").contains("-> OPEN").contains("store 10");
        }



        @Test
        void storeCannotOpenAnotherStore() {
            assertThatThrownBy(() -> module.openStore(store, 20L))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasMessageContaining("another store");
            verify(groceryStoreRepository, never()).findById(any());
        }

        @Test
        void customerCannotOpenAStore() {
            assertThatThrownBy(() -> module.openStore(customer, 10L))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void unknownStore_throwsNotFound() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> module.openStore(store, 10L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void alreadyOpen_throwsIllegalState() {
            store.setStatus(StoreStatus.OPEN);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(0);

            assertThatThrownBy(() -> module.openStore(store, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already open");
            verify(eventManager, never()).logEvent(anyString());
        }

        @Test
        void reopeningFromPaused_isRefusedWhenQueueHasSixOrMore() {
            store.setStatus(StoreStatus.PAUSED);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(6);

            assertThatThrownBy(() -> module.openStore(store, 10L))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.PAUSED);
        }

        @Test
        void reopeningFromPaused_isAllowedWhenQueueHasFive() {
            store.setStatus(StoreStatus.PAUSED);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(5);

            module.openStore(store, 10L);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("pauseStore / closeStore")
    class PauseAndClose {

        @Test
        void pause_fromOpen_becomesPaused() {
            store.setStatus(StoreStatus.OPEN);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));

            StoreContextData result = module.pauseStore(store, 10L);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.PAUSED);
            assertThat(result.status()).isEqualTo("PAUSED");
            verify(eventManager).logEvent(org.mockito.ArgumentMatchers.contains("-> PAUSED"));
        }

        @Test
        void pause_fromClosed_isRefused() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));

            assertThatThrownBy(() -> module.pauseStore(store, 10L))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
        }

        @Test
        void close_fromPaused_becomesClosed() {
            store.setStatus(StoreStatus.PAUSED);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));

            module.closeStore(store, 10L);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
            verify(eventManager).logEvent(org.mockito.ArgumentMatchers.contains("-> CLOSED"));
        }

        @Test
        void close_fromClosed_isRefused() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));

            assertThatThrownBy(() -> module.closeStore(store, 10L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void storeCannotPauseOrCloseAnotherStore() {
            assertThatThrownBy(() -> module.pauseStore(store, 20L)).isInstanceOf(PermissionDeniedException.class);
            assertThatThrownBy(() -> module.closeStore(store, 20L)).isInstanceOf(PermissionDeniedException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        void nullTarget_isIllegalArgument() {
            assertThatThrownBy(() -> module.changeStatus(store, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void onlyStoresMayUseIt() {
            assertThatThrownBy(() -> module.changeStatus(operator, StoreStatus.OPEN))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void dispatchesToTheCallersOwnStore() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(0);

            StoreContextData result = module.changeStatus(store, StoreStatus.OPEN);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.OPEN);
        }
    }

    // ------------------------------------------------------------------
    // getMenu
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getMenu")
    class GetMenu {

        @Test
        void courierCannotBrowse() {
            assertThatThrownBy(() -> module.getMenu(courier, 10L))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void storeCannotSeeAnotherStoresMenu() {
            assertThatThrownBy(() -> module.getMenu(store, 20L))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void customerSeesProductsSortedByName_withoutRemovedOnes() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(productRepository.findByStore_AccountId(10L)).thenReturn(List.of(
                    product(store, 1L, "Zucchini", ProductStatus.AVAILABLE),
                    product(store, 2L, "Apples", ProductStatus.OUT_OF_STOCK),
                    product(store, 3L, "Milk", ProductStatus.UNAVAILABLE)));

            StoreMenuData menu = module.getMenu(customer, 10L);

            assertThat(menu.store().id()).isEqualTo(10L);
            assertThat(menu.products()).extracting(ProductData::name).containsExactly("Apples", "Zucchini");
            assertThat(menu.products().get(0).status()).isEqualTo("OUT_OF_STOCK");
        }

        @Test
        void ownerSeesRemovedProductsToo() {
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(productRepository.findByStore_AccountId(10L)).thenReturn(List.of(
                    product(store, 1L, "Zucchini", ProductStatus.AVAILABLE),
                    product(store, 3L, "Milk", ProductStatus.UNAVAILABLE)));

            StoreMenuData menu = module.getMenu(store, 10L);

            assertThat(menu.products()).extracting(ProductData::name).containsExactly("Milk", "Zucchini");
        }
    }

    // ------------------------------------------------------------------
    // operator views: getAllStores / getAllOpenAndPausedStores
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("operator store lists")
    class OperatorLists {

        @Test
        void getAllStores_requiresOperator() {
            assertThatThrownBy(() -> module.getAllStores(store)).isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void getAllStores_emptyRepository_returnsEmptyWithoutCounting() {
            when(groceryStoreRepository.findAll()).thenReturn(List.of());

            assertThat(module.getAllStores(operator)).isEmpty();
            verify(orderRepository, never()).countByStoreAndStatus(anyCollection(), anyCollection());
        }

        @Test
        void getAllStores_inProgressIsPreparingPlusAccepted_readyIsReady() {
            when(groceryStoreRepository.findAll()).thenReturn(List.of(store, otherStore));
            when(orderRepository.countByStoreAndStatus(anyCollection(), anyCollection())).thenReturn(List.of(
                    count(10L, OrderStatus.PREPARING, 2),
                    count(10L, OrderStatus.ACCEPTED, 3),
                    count(10L, OrderStatus.READY, 1)));

            List<StoreContextData> result = module.getAllStores(operator);

            assertThat(result).hasSize(2);
            StoreContextData first = result.get(0);
            assertThat(first.id()).isEqualTo(10L);
            assertThat(first.ordersInProgress()).isEqualTo(5);
            assertThat(first.ordersReady()).isEqualTo(1);
            StoreContextData second = result.get(1);
            assertThat(second.ordersInProgress()).isZero();
            assertThat(second.ordersReady()).isZero();
        }

        @Test
        void getAllStoresForMap_doesNotRequireARole_andMapsCountsTheSameWay() {
            when(groceryStoreRepository.findAll()).thenReturn(List.of(otherStore));
            when(orderRepository.countByStoreAndStatus(anyCollection(), anyCollection())).thenReturn(List.of(
                    count(20L, OrderStatus.READY, 4)));

            List<StoreContextData> result = module.getAllStoresForMap(customer);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ordersReady()).isEqualTo(4);
            assertThat(result.get(0).ordersInProgress()).isZero();
        }
        @Test
        void operatorCanReopenAPausedStore() {
            store.setStatus(StoreStatus.PAUSED);
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));
            when(orderRepository.countByStore_AccountIdAndStatusIn(eq(10L), anyCollection())).thenReturn(0);

            module.openStore(operator, 10L);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.OPEN);
        }

        @Test
        void operatorCannotOpenAClosedStore() {
            // store is CLOSED in setUp
            when(groceryStoreRepository.findById(10L)).thenReturn(Optional.of(store));

            assertThatThrownBy(() -> module.openStore(operator, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("paused state");
            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
            verify(eventManager, never()).logEvent(anyString());
        }



    }
}
