package com.shopeasy.Domain.GS;


import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.ProductRepository;
import com.shopeasy.Foundation.repositories.StoreOrderCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GSModule {

    private final GroceryStoreRepository groceryStoreRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RoleValidator roleValidator;

    public GSModule(GroceryStoreRepository groceryStoreRepository, ProductRepository productRepository, RoleValidator roleValidator, OrderRepository orderRepository) {
        this.groceryStoreRepository = groceryStoreRepository;
        this.productRepository = productRepository;
        this.roleValidator = roleValidator;
        this.orderRepository = orderRepository;
    }

    public List<GroceryStore> listOpenStores() {
        return groceryStoreRepository.findByStatus(StoreStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public StoreMenuData getMenu(User user, Long storeId) {
        if (user.getRole() == Role.COURIER) {
            throw new PermissionDeniedException("A courier account cannot browse store menus");
        }
        if (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Stores can only see their own menu");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        List<ProductData> products = productRepository.findByStore_AccountId(storeId).stream()
                .sorted(Comparator.comparing(Product::getName))
                .map(p -> new ProductData(p.getProductId(), p.getName(), p.getDescription(),
                        p.getCategory().getCategoryName(), p.getPrice(), p.getStockQuantity(),
                        p.getStatus().name()))
                .toList();

        StoreContextData storeData = StoreContextData.from(store);
        return new StoreMenuData(storeData, products);
    }

    @Transactional
    public StoreContextData openStore(User user, Long storeId, int queueSize) {
        roleValidator.requireRole(user, List.of(Role.OPERATOR, Role.STORE), "open a store");
        if  (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Cannot open another store");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        StoreContext context = new StoreContext(store);
        context.openStore(queueSize);
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

        return StoreContextData.from(store);
    }

    @Transactional
    public StoreContextData closeStore(User user, Long storeId) {
        roleValidator.requireRole(user, List.of(Role.OPERATOR, Role.STORE), "pause a store");
        if  (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Cannot close another store");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        StoreContext context = new StoreContext(store);
        context.closeStore();

        return StoreContextData.from(store);
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
    public List<StoreContextData> getAllOpenAndPausedStores(User user) {
        //roleValidator.requireRole(user, Role.OPERATOR, "view all open and paused stores");

        List<GroceryStore> stores = groceryStoreRepository.findByStatusIn(
                List.of(StoreStatus.OPEN, StoreStatus.PAUSED));
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
