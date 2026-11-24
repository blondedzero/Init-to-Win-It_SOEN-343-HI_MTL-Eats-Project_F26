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

import java.math.BigDecimal;
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
    private final ProductCategoryRepository productCategoryRepository;
    private final OrderRepository orderRepository;
    private final RoleValidator roleValidator;
    private final EventManager eventManager;

    public GSModule(GroceryStoreRepository groceryStoreRepository, ProductRepository productRepository,
                    ProductCategoryRepository productCategoryRepository, RoleValidator roleValidator,
                    OrderRepository orderRepository, EventManager eventManager) {
        this.groceryStoreRepository = groceryStoreRepository;
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.roleValidator = roleValidator;
        this.orderRepository = orderRepository;
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
                .map(GSModule::toProductData)
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
        roleValidator.requireRole(user, List.of(Role.OPERATOR, Role.STORE), "close a store");
        if  (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
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

   private GroceryStore requireOwnStore(User user, String action) {
        roleValidator.requireRole(user, Role.STORE, action);
        return groceryStoreRepository.findById(user.getAccountId())
                .orElseThrow(() -> new NotFoundException("Store " + user.getAccountId() + " does not exist"));
    }

    @Transactional(readOnly = true)
    public StoreContextData getMyStore(User user) {
        return StoreContextData.from(requireOwnStore(user, "view store details"));
    }

    @Transactional(readOnly = true)
    public List<ProductData> getMyProducts(User user) {
        GroceryStore store = requireOwnStore(user, "view a catalog");
        return productRepository.findByStore_AccountId(store.getAccountId()).stream()
                .sorted(Comparator.comparing(Product::getName))
                .map(GSModule::toProductData)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return productCategoryRepository.findAll().stream()
                .map(ProductCategory::getCategoryName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional
    public ProductData addProduct(User user, ProductRequest req) {
        GroceryStore store = requireOwnStore(user, "add products");
        validateProductRequest(req);

        Product product = new Product(store, req.price(), req.name().trim(), req.description(),
                req.stockQuantity(), findOrCreateCategory(req.categoryName()));
        if (product.getStockQuantity() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);
        eventManager.logEvent("Store " + store.getAccountId() + ": added product " + saved.getProductId()
                + " (" + saved.getName() + ")");
        return toProductData(saved);
    }

    @Transactional
    public ProductData updateProduct(User user, Long productId, ProductRequest req) {
        GroceryStore store = requireOwnStore(user, "edit products");
        Product product = requireOwnProduct(store, productId);
        validateProductRequest(req);

        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setCategory(findOrCreateCategory(req.categoryName()));

        if (product.getStatus() != ProductStatus.UNAVAILABLE) {
            product.setStatus(product.getStockQuantity() > 0
                    ? ProductStatus.AVAILABLE
                    : ProductStatus.OUT_OF_STOCK);
        }

        eventManager.logEvent("Store " + store.getAccountId() + ": edited product " + productId);
        return toProductData(product);
    }

    @Transactional
    public ProductData removeProduct(User user, Long productId) {
        GroceryStore store = requireOwnStore(user, "remove products");
        Product product = requireOwnProduct(store, productId);

        product.setStatus(ProductStatus.UNAVAILABLE);

        eventManager.logEvent("Store " + store.getAccountId() + ": removed product " + productId);
        return toProductData(product);
    }

    @Transactional
    public ProductData restoreProduct(User user, Long productId) {
        GroceryStore store = requireOwnStore(user, "restore products");
        Product product = requireOwnProduct(store, productId);

        product.setStatus(product.getStockQuantity() > 0
                ? ProductStatus.AVAILABLE
                : ProductStatus.OUT_OF_STOCK);

        eventManager.logEvent("Store " + store.getAccountId() + ": restored product " + productId);
        return toProductData(product);
    }


    @Transactional(readOnly = true)
    public List<OrderData> getOrderHistory(User user) {
        GroceryStore store = requireOwnStore(user, "view order history");
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

    private Product requireOwnProduct(GroceryStore store, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " does not exist"));
        if (!product.getStore().getAccountId().equals(store.getAccountId())) {
            throw new PermissionDeniedException("Stores can only modify their own products");
        }
        return product;
    }

    private ProductCategory findOrCreateCategory(String categoryName) {
        String name = categoryName.trim();
        return productCategoryRepository.findByCategoryName(name)
                .orElseGet(() -> productCategoryRepository.save(new ProductCategory(name)));
    }

    private static void validateProductRequest(ProductRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("A product is required");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be zero or more");
        }
        if (req.stockQuantity() == null || req.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity must be zero or more");
        }
        if (req.categoryName() == null || req.categoryName().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
    }

    private static ProductData toProductData(Product p) {
        return new ProductData(p.getProductId(), p.getName(), p.getDescription(),
                p.getCategory().getCategoryName(), p.getPrice(), p.getStockQuantity(),
                p.getStatus().name());
    }
}
