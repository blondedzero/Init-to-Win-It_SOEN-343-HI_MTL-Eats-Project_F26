package com.shopeasy;

import com.shopeasy.Domain.PRC.PRCModule;
import com.shopeasy.Domain.REG.PasswordEncoder;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApplicationSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSeeder.class);
    private static final String SEED_MARKER = "Demo dataset v1 seeded";
    private static final String INITIAL_STATE_MARKER = "Demo reset baseline v1 captured";
    private static final String DEMO_PASSWORD = "test";

    private static final List<CategorySeed> CATEGORIES = List.of(
            new CategorySeed(1L, "Vegetables"),
            new CategorySeed(2L, "Frozen Food"),
            new CategorySeed(3L, "Snacks"),
            new CategorySeed(4L, "Meat & Poultry"),
            new CategorySeed(5L, "Dairy"),
            new CategorySeed(6L, "Fruits"),
            new CategorySeed(7L, "Vegan Food"),
            new CategorySeed(8L, "Baking & Spices"),
            new CategorySeed(10L, "Grains, Pasta & Cereal")
    );

    private static final List<ProductSeed> PRODUCTS = List.of(
            new ProductSeed(1L, "Tomatoes", "Fresh tomatoes", "2.99", 12),
            new ProductSeed(2L, "Frozen Peas", "Frozen green peas", "3.49", 0),
            new ProductSeed(3L, "Potato Chips", "Crispy salted chips", "2.79", 12),
            new ProductSeed(4L, "Chicken Breast", "Boneless chicken breast", "10.99", 12),
            new ProductSeed(5L, "Milk", "Two percent milk", "4.29", 12),
            new ProductSeed(6L, "Apples", "Fresh apples", "4.99", 12),
            new ProductSeed(7L, "Vegan Burgers", "Plant-based burger patties", "7.99", 0),
            new ProductSeed(8L, "Baking Flour", "All-purpose flour", "5.49", 0),
            new ProductSeed(10L, "Pasta", "Dry pasta", "2.49", 12)
    );

    private final UserRepository userRepository;
    private final GroceryStoreRepository groceryStoreRepository;
    private final CourierRepository courierRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentInfoRepository paymentInfoRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SystemEventRepository systemEventRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new PasswordEncoder();
    private final PRCModule prcModule;
    private boolean seedDataAlreadyPresent;

    public ApplicationSeeder(
            UserRepository userRepository,
            GroceryStoreRepository groceryStoreRepository,
            CourierRepository courierRepository,
            VehicleRepository vehicleRepository,
            PaymentInfoRepository paymentInfoRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            SystemEventRepository systemEventRepository,
            JdbcTemplate jdbcTemplate,
            PRCModule prcModule) {
        this.userRepository = userRepository;
        this.groceryStoreRepository = groceryStoreRepository;
        this.courierRepository = courierRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentInfoRepository = paymentInfoRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.systemEventRepository = systemEventRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.prcModule = prcModule;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (systemEventRepository.existsByEvent(SEED_MARKER)) {
            if (!systemEventRepository.existsByEvent(INITIAL_STATE_MARKER)) {
                captureInitialStates();
                systemEventRepository.save(new SystemEvent(INITIAL_STATE_MARKER));
            }
            seedDataAlreadyPresent = true;
            return;
        }

        Map<Long, ProductCategory> categories = seedCategories();
        Operator operator = ensureOperator();
        List<Customer> customers = ensureCustomers();
        List<GroceryStore> stores = ensureStores();
        CourierSetup courierSetup = ensureCouriers();
        ensureCustomerPaymentInfo(customers);
        ensureCourierVehicles(courierSetup.couriers());

        Map<Long, List<Product>> productsByStore = seedProducts(stores, categories);
        seedOrders(customers, stores, courierSetup.availableCouriers(), operator, productsByStore);
        captureInitialStates();

        systemEventRepository.save(new SystemEvent(SEED_MARKER));
        systemEventRepository.save(new SystemEvent(INITIAL_STATE_MARKER));
    }

    @Transactional
    public void restoreInitialEntityStates() {
        groceryStoreRepository.findAll().forEach(store -> store.setStatus(
                store.getSeedInitialStatus() == null ? StoreStatus.CLOSED : store.getSeedInitialStatus()));
        courierRepository.findAll().forEach(courier -> courier.setStatus(
                courier.getSeedInitialStatus() == null ? CourierStatus.OFFLINE : courier.getSeedInitialStatus()));
        productRepository.findAll().forEach(Product::restoreSeedInitialState);
    }

    private void captureInitialStates() {
        groceryStoreRepository.findAll().forEach(store -> {
            if (store.getSeedInitialStatus() == null) {
                store.setSeedInitialStatus(store.getStatus());
            }
        });
        courierRepository.findAll().forEach(courier -> {
            if (courier.getSeedInitialStatus() == null) {
                courier.setSeedInitialStatus(courier.getStatus());
            }
        });
        productRepository.findAll().forEach(product ->
                product.setSeedInitialState(product.getStatus(), product.getStockQuantity()));
        orderRepository.findAll().forEach(Order::captureSeedInitialState);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationReady() {
        if (seedDataAlreadyPresent) {
            LOGGER.info("Application is running; database already contains demo seed data.");
        } else {
            LOGGER.info("Application is running; demo seed data was loaded.");
        }
    }

    private Map<Long, ProductCategory> seedCategories() {
        Map<Long, ProductCategory> categories = new HashMap<>();
        for (CategorySeed category : CATEGORIES) {
            List<String> namesAtId = jdbcTemplate.query(
                    "SELECT category_name FROM product_categories WHERE category_id = ?",
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    category.id());
            List<Long> idsForName = jdbcTemplate.queryForList(
                    "SELECT category_id FROM product_categories WHERE category_name = ?",
                    Long.class,
                    category.name());

            if (namesAtId.isEmpty()) {
                if (!idsForName.isEmpty()) {
                    LOGGER.info("Product category already exists at an unexpected ID: "
                            + category.name());
                }
                jdbcTemplate.update(
                        "INSERT INTO product_categories (category_id, category_name) "
                                + "OVERRIDING SYSTEM VALUE VALUES (?, ?)",
                        category.id(),
                        category.name());
            } else if (!category.name().equals(namesAtId.get(0))) {
                LOGGER.info("Product category ID " + category.id()
                        + " is already assigned to " + namesAtId.get(0));
            }

            Optional<ProductCategory> persisted = productCategoryRepository.findById(category.id());
            persisted.ifPresent(productCategory -> categories.put(category.id(), productCategory));
        }

        jdbcTemplate.queryForObject("""
                SELECT setval(
                    pg_get_serial_sequence('product_categories', 'category_id'),
                    (SELECT MAX(category_id) FROM product_categories),
                    true
                )
                """, Long.class);
        return categories;
    }

    private Operator ensureOperator() {
        User user = userRepository.findByEmail("operator@email.com").orElse(null);
        if (user == null) {
            return userRepository.save(new Operator(
                    "operator@email.com", passwordEncoder.hashPassword(DEMO_PASSWORD), "Demo", "Operator"));
        }
        if (!(user instanceof Operator operator)) {
            user.setEmail(UUID.randomUUID().toString()+"@email.com");
            userRepository.save(user);
            return userRepository.save(new Operator(
                    "operator@email.com", passwordEncoder.hashPassword(DEMO_PASSWORD), "Demo", "Operator"));
        } else return operator;
    }

    private List<Customer> ensureCustomers() {
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String email = "customer" + i + "@email.com";
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                double latitude = 45.50 + (i * 0.006);
                double longitude = -73.58 + (i * 0.006);
                user = userRepository.save(new Customer(
                        email,
                        passwordEncoder.hashPassword(DEMO_PASSWORD),
                        "Customer",
                        Integer.toString(i),
                        (100 + i) + " Demo Street, Montreal, QC",
                        longitude,
                        latitude));
            }
            if (!(user instanceof Customer customer)) {
                user.setPasswordHash(passwordEncoder.hashPassword(DEMO_PASSWORD));
                userRepository.save(user);
                continue;
            }
            customers.add(customer);
        }
        return customers;
    }

    private List<GroceryStore> ensureStores() {
        List<StoreSeed> storeSeeds = List.of(
                new StoreSeed("IGA", "iga", StoreStatus.PAUSED, "1000 Rue Sainte-Catherine O, Montreal, QC", 45.496, -73.578),
                new StoreSeed("Super C", "superc", StoreStatus.OPEN, "2000 Rue Saint-Denis, Montreal, QC", 45.515, -73.565),
                new StoreSeed("Walmart", "walmart", StoreStatus.OPEN, "3000 Rue Jean-Talon O, Montreal, QC", 45.535, -73.615),
                new StoreSeed("Provigo", "provigo", StoreStatus.CLOSED, "4000 Avenue du Parc, Montreal, QC", 45.525, -73.590),
                new StoreSeed("Maxi", "maxi", StoreStatus.CLOSED, "5000 Boulevard Saint-Laurent, Montreal, QC", 45.530, -73.585)
        );

        List<GroceryStore> stores = new ArrayList<>();
        for (StoreSeed seed : storeSeeds) {
            String email = "store@" + seed.emailName() + ".com";
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                GroceryStore newStore = new GroceryStore(
                        email,
                        passwordEncoder.hashPassword(DEMO_PASSWORD),
                        seed.name(),
                        "Demo Store",
                        seed.address(),
                        seed.longitude(),
                        seed.latitude(),
                        seed.name());
                newStore.setStatus(seed.status());
                user = newStore;
            }
            if (!(user instanceof GroceryStore store)) {
                LOGGER.info(email + " is already used by a non-store account");
            } else {
                store.setStoreName(seed.name());
                store.setStatus(seed.status());
                stores.add(groceryStoreRepository.save(store));
            }
        }
        return stores;
    }

    private CourierSetup ensureCouriers() {
        List<Courier> couriers = new ArrayList<>();
        List<Courier> availableCouriers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String email = "courier" + i + "@email.com";
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = userRepository.save(new Courier(
                        email,
                        passwordEncoder.hashPassword(DEMO_PASSWORD),
                        "Courier",
                        Integer.toString(i),
                        -73.58 + (i * 0.004),
                        45.50 + (i * 0.004)));
            }
            if (!(user instanceof Courier courier)) {
                LOGGER.info(email + " is already used by a non-courier account");
            } else {
                couriers.add(courier);
                if (hasActiveOrder(courier)) {
                    continue;
                }
                courier.setStatus(CourierStatus.IDLE);
                Courier savedCourier = courierRepository.save(courier);
                couriers.set(couriers.size() - 1, savedCourier);
                availableCouriers.add(savedCourier);
            }
        }
        return new CourierSetup(couriers, availableCouriers);
    }

    private boolean hasActiveOrder(Courier courier) {
        return orderRepository.existsByCourier_AccountIdAndStatus(courier.getAccountId(), OrderStatus.READY)
                || orderRepository.existsByCourier_AccountIdAndStatus(courier.getAccountId(), OrderStatus.PICKED_UP);
    }

    private void ensureCustomerPaymentInfo(List<Customer> customers) {
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            if (!paymentInfoRepository.existsById(customer.getAccountId())) {
                paymentInfoRepository.save(new PaymentInfo(
                        customer, "VISA", "4242", 12, 2030,
                        "demo-payment-token-" + (i + 1),
                        "demo-stripe-customer-" + (i + 1)));
            }
        }
    }

    private void ensureCourierVehicles(List<Courier> couriers) {
        for (int i = 0; i < couriers.size(); i++) {
            Courier courier = couriers.get(i);
            if (!vehicleRepository.existsById(courier.getAccountId())) {
                vehicleRepository.save(new Vehicle(
                        courier, "Toyota", "Corolla", "Blue", 2022 + (i % 3)));
            }
        }
    }

    private Map<Long, List<Product>> seedProducts(
            List<GroceryStore> stores,
            Map<Long, ProductCategory> categories) {
        Map<Long, List<Product>> productsByStore = new HashMap<>();
        for (GroceryStore store : stores) {
            List<Product> storeProducts = productRepository.findByStore_AccountId(store.getAccountId());
            List<Product> seededProducts = new ArrayList<>();

            for (ProductSeed seed : PRODUCTS) {
                String name = "Demo " + store.getStoreName() + " " + seed.name();
                BigDecimal price = new BigDecimal(seed.price());
                Product product = storeProducts.stream()
                        .filter(existing -> existing.getName().equals(name))
                        .findFirst()
                        .orElseGet(() -> new Product(
                                store,
                                price,
                                name,
                                seed.description(),
                                seed.stockQuantity(),
                                categories.get(seed.categoryId())));
                product.setPrice(price);
                product.setDescription(seed.description());
                product.setCategory(categories.get(seed.categoryId()));
                product.setStockQuantity(seed.stockQuantity());
                product.setStatus(seed.stockQuantity() > 0
                        ? ProductStatus.AVAILABLE
                        : ProductStatus.OUT_OF_STOCK);
                seededProducts.add(productRepository.save(product));
            }
            productsByStore.put(store.getAccountId(), seededProducts);
        }
        return productsByStore;
    }

    private void seedOrders(
            List<Customer> customers,
            List<GroceryStore> stores,
            List<Courier> availableCouriers,
            Operator operator,
            Map<Long, List<Product>> productsByStore) {
        Map<Long, Product> firstAvailableProducts = new HashMap<>();
        for (GroceryStore store : stores) {
            firstAvailableProduct(productsByStore, store).ifPresentOrElse(
                    product -> firstAvailableProducts.put(store.getAccountId(), product),
                    () -> LOGGER.warn("Skipping demo orders for store {}: no available product with stock",
                            store.getStoreName()));
        }

        if (availableCouriers.size() >= 5) {
            availableCouriers.get(4).setStatus(CourierStatus.OFFLINE);
        }

        AtomicInteger orderNumber = new AtomicInteger(1);
        int customerIndex = 0;
        List<OrderStatus> igaOrderStatuses = new ArrayList<>(List.of(
                OrderStatus.PLACED, OrderStatus.PLACED, OrderStatus.ACCEPTED,
                OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.PICKED_UP));
        Map<Integer, Courier> igaCourierAssignments = new HashMap<>();
        if (availableCouriers.isEmpty()) {
            igaOrderStatuses.set(5, OrderStatus.READY);
        } else {
            igaCourierAssignments.put(5, availableCouriers.get(0));
        }

        customerIndex = seedProgressOrders(
                igaOrderStatuses,
                igaCourierAssignments,
                customers, stores.get(0), firstAvailableProducts, orderNumber, customerIndex, operator);

        Map<Integer, Courier> superCCourierAssignments = new HashMap<>();
        int nextCourierIndex = availableCouriers.isEmpty() ? 0 : 1;
        for (int readyOrderIndex = 7; readyOrderIndex <= 8
                && nextCourierIndex < availableCouriers.size(); readyOrderIndex++) {
            superCCourierAssignments.put(readyOrderIndex, availableCouriers.get(nextCourierIndex++));
        }

        customerIndex = seedProgressOrders(
                List.of(OrderStatus.PLACED, OrderStatus.PLACED, OrderStatus.PLACED,
                        OrderStatus.ACCEPTED, OrderStatus.ACCEPTED,
                        OrderStatus.PREPARING, OrderStatus.PREPARING,
                        OrderStatus.READY, OrderStatus.READY, OrderStatus.READY),
                superCCourierAssignments,
                customers, stores.get(1), firstAvailableProducts, orderNumber, customerIndex, operator);

        seedProgressOrders(
                List.of(OrderStatus.PLACED, OrderStatus.PLACED, OrderStatus.PLACED),
                Map.of(),
                customers, stores.get(2), firstAvailableProducts, orderNumber, customerIndex, operator);

        for (int customerNumber = 0; customerNumber < customers.size(); customerNumber++) {
            Customer customer = customers.get(customerNumber);
            GroceryStore store = stores.get(customerNumber % 3);
            Product product = firstAvailableProducts.get(store.getAccountId());
            if (product == null) {
                continue;
            }

            for (int i = 0; i < 2; i++) {
                createOrder(customer, store, product, OrderStatus.REJECTED, null,
                        operator, orderNumber.getAndIncrement());
            }
            for (int i = 0; i < 2; i++) {
                createOrder(customer, store, product, OrderStatus.CANCELED, null,
                        operator, orderNumber.getAndIncrement());
            }
            for (int i = 0; i < 2; i++) {
                createOrder(customer, store, product, OrderStatus.REFUNDED, null,
                        operator, orderNumber.getAndIncrement());
            }
        }
    }

    private int seedProgressOrders(
            List<OrderStatus> statuses,
            Map<Integer, Courier> courierAssignments,
            List<Customer> customers,
            GroceryStore store,
            Map<Long, Product> firstAvailableProducts,
            AtomicInteger orderNumber,
            int customerIndex,
            Operator operator) {
        Product product = firstAvailableProducts.get(store.getAccountId());
        if (product == null) {
            return customerIndex;
        }
        for (int i = 0; i < statuses.size(); i++) {
            createOrder(
                    customers.get(customerIndex++ % customers.size()),
                    store,
                    product,
                    statuses.get(i),
                    courierAssignments.get(i),
                    operator,
                    orderNumber.getAndIncrement());
        }
        return customerIndex;
    }

    private Order createOrder(
            Customer customer,
            GroceryStore store,
            Product product,
            OrderStatus targetStatus,
            Courier courier,
            Operator operator,
            int orderNumber) {
        Order order = new Order(customer, store);
        order.addItem(product, 1);
        order = prcModule.calculateOrderTotalCost(order);

        switch (targetStatus) {
            case PLACED -> {
            }
            case ACCEPTED -> order.markAccepted();
            case PREPARING -> {
                order.markAccepted();
                order.markPreparing();
            }
            case READY, PICKED_UP -> {
                order.markAccepted();
                order.markPreparing();
                order.markReady();
                if (courier != null) {
                    order.setCourier(courier);
                    courier.assignOrder();
                    order.acceptCourierAssignment();
                }
                if (targetStatus == OrderStatus.PICKED_UP) {
                    if (courier == null) {
                        order.markReady();
                    } else {
                        order.markPickedUp();  
                        courier.confirmPickup();
                    }
                }
            }

            case DELIVERED -> {
                if (order.getCourier() == null) {
                    order.markReady();
                } else {
                    order.markDelivered();
                    order.getCourier().setStatus(CourierStatus.IDLE);
                }
            }

            case REJECTED -> order.markRejected("Demo seed rejected order");
            case CANCELED -> order.markCancelled();
            case REFUNDED -> {
                order.markCancelled();
                order.markCharged("demo-charge-v1-" + orderNumber);
                order.markRefunded(new Refund(
                        order,
                        "Demo seed refund",
                        order.getTotalCost(),
                        operator.getAccountId(),
                        "demo-refund-v1-" + orderNumber));
            }
        }
        order.captureSeedInitialState();
        return orderRepository.save(order);
    }

    private Optional<Product> firstAvailableProduct(Map<Long, List<Product>> productsByStore, GroceryStore store) {
        return productsByStore.getOrDefault(store.getAccountId(), List.of()).stream()
                .filter(product -> product.getStatus() == ProductStatus.AVAILABLE
                        && product.getStockQuantity() > 0)
                .findFirst();
    }

    private record CategorySeed(Long id, String name) {}

    private record ProductSeed(
            Long categoryId,
            String name,
            String description,
            String price,
            int stockQuantity) {}

    private record StoreSeed(
            String name,
            String emailName,
            StoreStatus status,
            String address,
            double latitude,
            double longitude) {}

    private record CourierSetup(List<Courier> couriers, List<Courier> availableCouriers) {}
}
