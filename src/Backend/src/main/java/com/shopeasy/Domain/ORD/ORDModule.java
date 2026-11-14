package com.shopeasy.Domain.ORD;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.*;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ORDModule {

    private final OrderRepository orderRepository;
    private final GroceryStoreRepository groceryStoreRepository;
    private final ProductRepository productRepository;
    private final EventManager eventManager;
    private final RoleValidator roleValidator;
    private final CourierRepository courierRepository;

    private final Map<Long, PreparationQueue> queues = new ConcurrentHashMap<>();

    public ORDModule(OrderRepository orderRepository, GroceryStoreRepository groceryStoreRepository,
                     ProductRepository productRepository, EventManager eventManager, RoleValidator roleValidator,
                     CourierRepository courierRepository) {
        this.orderRepository = orderRepository;
        this.groceryStoreRepository = groceryStoreRepository;
        this.productRepository = productRepository;
        this.eventManager = eventManager;
        this.roleValidator = roleValidator;
        this.courierRepository = courierRepository;
    }

    @Transactional
    public OrderResult placeOrder(User user, PlaceOrderRequest request) {
        roleValidator.requireRole(user, Role.CUSTOMER, "place orders");
        Customer customer = (Customer) user;

        if (request == null || request.storeId() == null) {
            throw new IllegalArgumentException("A store must be selected");
        }
        Map<Long, Integer> quantities = mergeLines(request.lines());

        GroceryStore store = groceryStoreRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Store " + request.storeId() + " does not exist"));

        if (store.getStatus() != StoreStatus.OPEN) {
            throw new OrderRuleViolationException(store.getStoreName() + " is "
                    + store.getStatus().name().toLowerCase() + " and is not accepting orders");
        }

        Order order = new Order(customer, store);
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            Product product = productRepository.findById(line.getKey())
                    .orElseThrow(() -> new NotFoundException("Item " + line.getKey() + " does not exist"));
            if (!product.getStore().getAccountId().equals(store.getAccountId())) {
                throw new OrderRuleViolationException(product.getName() + " is not sold by " + store.getStoreName());
            }
            if (!product.isAvailable()) {
                throw new OrderRuleViolationException(product.getName() + " is sold out");
            }
            int quantity = line.getValue();
            if (quantity > product.getStockQuantity()) {
                throw new OrderRuleViolationException(product.getName() + " has only "
                        + product.getStockQuantity() + " left in stock");
            }
            product.removeFromStock(quantity);
            order.addItem(product, quantity);
        }

        Order saved = orderRepository.save(order);
        Long eventId = eventManager.logEvent("Order " + saved.getOrderId() + ": placed (PLACED) by customer "
                + customer.getAccountId() + " at store " + store.getAccountId());

        return new OrderResult("Order #" + saved.getOrderId() + " placed at " + store.getStoreName(),
                eventId, OrderData.from(saved));
    }

    @Transactional(readOnly = true)
    public List<OrderData> getIncomingOrders(User user) {
        //roleValidator.requireRole(user, Role.STORE, "view incoming orders");
        return orderRepository.findByStore_AccountIdAndStatus(user.getAccountId(), OrderStatus.PLACED).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    @Transactional
    public OrderResult acceptOrder(User user, Long orderId) {
        roleValidator.requireRole(user, Role.STORE, "accept orders");
        Order order = findOwnOrder(user, orderId);

        order.markAccepted();
        Long eventId = eventManager.logEvent("Order " + orderId + ": PLACED -> ACCEPTED by store " + user.getAccountId());

        PreparationQueue queue = queueFor(user.getAccountId());
        queue.enqueue(order);

        return new OrderResult("Order #" + orderId + " accepted - position " + queue.positionOf(orderId)
                + " of " + queue.size() + " in the preparation queue", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult rejectOrder(User user, Long orderId, String reason) {
        roleValidator.requireRole(user, Role.STORE, "reject orders");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reject an order");
        }
        Order order = findOwnOrder(user, orderId);

        order.markRejected(reason.trim());
        Long eventId = eventManager.logEvent("Order " + orderId + ": PLACED -> REJECTED by store "
                + user.getAccountId() + " (" + reason.trim() + ")");

        return new OrderResult("Order #" + orderId + " rejected", eventId, OrderData.from(order));
    }

    @Transactional(readOnly = true)
    public QueueData getPreparationQueue(User user) {
        roleValidator.requireRole(user, Role.STORE, "view a preparation queue");
        GroceryStore store = (GroceryStore) user;
        List<Long> orderIds = queueFor(store.getAccountId()).orderIds();

        Map<Long, Order> ordersById = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getOrderId, Function.identity()));
        List<OrderData> orders = orderIds.stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .map(OrderData::from)
                .toList();
        return new QueueData(store.getAccountId(), store.getStoreName(), orders.size(), orders);
    }

    @Transactional
    public OrderResult startPreparing(User user, Long orderId) {
        roleValidator.requireRole(user, Role.STORE, "prepare orders");
        Order order = findOwnOrder(user, orderId);
        PreparationQueue queue = queueFor(user.getAccountId());

        order.markPreparing();
        Long eventId = eventManager.logEvent("Order " + orderId + ": ACCEPTED -> PREPARING by store " + user.getAccountId());

        queue.dequeue(orderId);

        return new OrderResult("Order #" + orderId + " is being prepared", eventId, OrderData.from(order));
    }

    @Transactional
    public OrderResult markReady(User user, Long orderId) {
        roleValidator.requireRole(user, Role.STORE, "mark orders ready");
        Order order = findOwnOrder(user, orderId);

        order.markReady();
        Long eventId = eventManager.logEvent("Order " + orderId + ": PREPARING -> READY by store " + user.getAccountId());

        return new OrderResult("Order #" + orderId + " is ready for pickup", eventId, OrderData.from(order));
    }

    @Transactional(readOnly = true)
    public List<OrderData> getOrdersInPreparation(User user) {
        roleValidator.requireRole(user, Role.STORE, "view orders in preparation");
        return orderRepository.findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(
                        user.getAccountId(), List.of(OrderStatus.PREPARING)).stream()
                .map(OrderData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderData> getReadyOrders(User user) {
        roleValidator.requireRole(user, Role.STORE, "view ready orders");
        return orderRepository.findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(
                        user.getAccountId(), List.of(OrderStatus.READY)).stream()
                .map(OrderData::from)
                .toList();
    }

    private PreparationQueue queueFor(Long storeId) {
        return queues.computeIfAbsent(storeId, id -> {
            PreparationQueue queue = new PreparationQueue();
            orderRepository.findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(id, List.of(OrderStatus.ACCEPTED))
                    .forEach(queue::enqueue);
            return queue;
        });
    }

    private Order findOwnOrder(User store, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " does not exist"));
        if (!order.getStore().getAccountId().equals(store.getAccountId())) {
            throw new PermissionDeniedException("Order " + orderId + " belongs to another store");
        }
        return order;
    }

    private Map<Long, Integer> mergeLines(List<PlaceOrderRequest.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one item");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PlaceOrderRequest.Line line : lines) {
            if (line.productId() == null) {
                throw new IllegalArgumentException("Every order line needs an item");
            }
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Quantities must be positive");
            }
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }
        return quantities;
    }
}
