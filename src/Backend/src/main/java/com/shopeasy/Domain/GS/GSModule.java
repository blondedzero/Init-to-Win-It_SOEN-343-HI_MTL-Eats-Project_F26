package com.shopeasy.Domain.GS;


import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GSModule {

    private static final List<OrderStatus> FINAL_ORDER_STATUSES =
            List.of(OrderStatus.DELIVERED, OrderStatus.CANCELED, OrderStatus.REJECTED, OrderStatus.REFUNDED);

    private final GroceryStoreRepository groceryStoreRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RoleValidator roleValidator;
    private final StoreAccess storeAccess;
    private final EventManager eventManager;

    public GSModule(GroceryStoreRepository groceryStoreRepository, ProductRepository productRepository,
                    OrderRepository orderRepository, RoleValidator roleValidator,
                    StoreAccess storeAccess, EventManager eventManager) {
        this.groceryStoreRepository = groceryStoreRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.roleValidator = roleValidator;
        this.storeAccess = storeAccess;
        this.eventManager = eventManager;
    }

    public List<GroceryStore> listStores() {
        return groceryStoreRepository.findAll();
    }

    @Transactional(readOnly = true)
    public StoreMenuData getMenu(User user, Long storeId) {
        if (user.getRole() == Role.COURIER) {
            throw new PermissionDeniedException("A courier account cannot browse store menus");
        }
        boolean isOwner = user.getRole() == Role.STORE && user.getAccountId().equals(storeId);
        if (user.getRole() == Role.STORE && !isOwner) {
            throw new PermissionDeniedException("Stores can only see their own menu");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        List<ProductData> products = productRepository.findByStore_AccountId(storeId).stream()
                // Removed products (5.5.2-6) stay in the DB for order history; only the
                // owning store sees them, customers never do.
                .filter(p -> isOwner || p.getStatus() != ProductStatus.UNAVAILABLE)
                .sorted(Comparator.comparing(Product::getName))
                .map(ProductData::from)
                .toList();

        StoreContextData storeData = StoreContextData.from(store);
        return new StoreMenuData(storeData, products);
    }

    // =====================================================================
    // 5.5.2-5: store status (State pattern: StoreContext + GS/States)
    // Callable by the store itself or by an operator.
    // =====================================================================

    @Transactional
    public StoreContextData openStore(User user, Long storeId) {
        roleValidator.requireRole(user, List.of(Role.OPERATOR, Role.STORE), "open a store");
        if  (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Cannot open another store");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));
        if (user.getRole() == Role.OPERATOR && store.getStatus() != StoreStatus.PAUSED) {
            throw new IllegalStateException("An operator can only open a store from the paused state");
        }

        int queueSize = orderRepository.countByStore_AccountIdAndStatusIn(store.getAccountId(), List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING));

        StoreContext context = new StoreContext(store);
        context.openStore(queueSize);
        eventManager.logEvent("Store " + storeId + ": -> OPEN by " + user.getRole().name().toLowerCase() + " " + user.getAccountId());
        return StoreContextData.from(store);
    }

    @Transactional
    public StoreContextData pauseStore(User user, Long storeId) {
        roleValidator.requireRole(user, List.of(Role.OPERATOR, Role.STORE), "pause a store");
        if  (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Cannot pause another store");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
            .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        StoreContext context = new StoreContext(store);
        context.pauseStore();
        eventManager.logEvent("Store " + storeId + ": -> PAUSED by " + user.getRole().name().toLowerCase() + " " + user.getAccountId());
        return StoreContextData.from(store);
    }

    @Transactional
    public StoreContextData closeStore(User user, Long storeId) {
        roleValidator.requireRole(user, Role.STORE, "close a store");
        if (!user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Cannot close another store");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        StoreContext context = new StoreContext(store);
        context.closeStore();
        eventManager.logEvent("Store " + storeId + ": -> CLOSED by " + user.getRole().name().toLowerCase() + " " + user.getAccountId());
        return StoreContextData.from(store);
    }

    @Transactional
    public StoreContextData changeStatus(User user, StoreStatus target) {
        if (target == null) {
            throw new IllegalArgumentException("A target status is required");
        }
        roleValidator.requireRole(user, Role.STORE, "change a store's status");
        Long storeId = user.getAccountId();
        return switch (target) {
            case OPEN -> openStore(user, storeId);
            case PAUSED -> pauseStore(user, storeId);
            case CLOSED -> closeStore(user, storeId);
        };
    }

    @Transactional(readOnly = true)
    public StoreContextData getMyStore(User user) {
        return StoreContextData.from(storeAccess.requireOwnStore(user, "view store details"));
    }


    @Transactional(readOnly = true)
    public List<OrderData> getOrderHistory(User user) {
        GroceryStore store = storeAccess.requireOwnStore(user, "view order history");
        return orderRepository.findByStore_AccountIdAndStatusIn(store.getAccountId(), FINAL_ORDER_STATUSES).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt, Comparator.reverseOrder()).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<StoreContextData> getAllStores(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all stores");
        List<GroceryStore> stores = groceryStoreRepository.findAll();
        if (stores.isEmpty()) {
            return List.of();
        }
        List<Long> ids = stores.stream().map(GroceryStore::getAccountId).toList();

        Map<Long, Map<OrderStatus, Long>> counts = countActiveOrders(ids);

        return stores.stream()
                .map(s -> {
                    Map<OrderStatus, Long> c = counts.getOrDefault(s.getAccountId(), Map.of());
                    return StoreContextData.from(s,
                            c.getOrDefault(OrderStatus.PREPARING, 0L) + c.getOrDefault(OrderStatus.ACCEPTED, 0L),
                            c.getOrDefault(OrderStatus.READY, 0L));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreContextData> getAllStoresForMap(User user) {
        //roleValidator.requireRole(user, Role.OPERATOR, "view all open and paused stores");

        List<GroceryStore> stores = groceryStoreRepository.findAll();
        if (stores.isEmpty()) {
            return List.of();
        }
        List<Long> ids = stores.stream().map(GroceryStore::getAccountId).toList();

        Map<Long, Map<OrderStatus, Long>> counts = countActiveOrders(ids);

        return stores.stream()
                .map(s -> {
                    Map<OrderStatus, Long> c = counts.getOrDefault(s.getAccountId(), Map.of());
                    return StoreContextData.from(s,
                            c.getOrDefault(OrderStatus.PREPARING, 0L) + c.getOrDefault(OrderStatus.ACCEPTED, 0L),
                            c.getOrDefault(OrderStatus.READY, 0L));
                })
                .toList();
    }

    private Map<Long, Map<OrderStatus, Long>> countActiveOrders(List<Long> ids) {
        return orderRepository
                .countByStoreAndStatus(ids, List.of(OrderStatus.PREPARING, OrderStatus.ACCEPTED, OrderStatus.READY))
                .stream()
                .collect(Collectors.groupingBy(
                        StoreOrderCount::getStoreId,
                        Collectors.toMap(StoreOrderCount::getStatus, StoreOrderCount::getCount)));
    }

    @Transactional(readOnly = true)
    public List<StoreContextData> getAllClosedStores(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all stores");
        return groceryStoreRepository.findByStatus(StoreStatus.CLOSED).stream()
                .map(StoreContextData::from)
                .toList();
    }
}
