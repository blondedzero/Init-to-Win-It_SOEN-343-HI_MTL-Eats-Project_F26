package com.shopeasy.Domain.PRC;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.exceptions.OrderRuleViolationException;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.entities.PaymentInfo;
import com.shopeasy.Foundation.entities.Refund;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.PaymentInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PRCModule {
    private final OrderRepository orderRepository;
    private final RoleValidator roleValidator;
    private final PaymentInfoRepository paymentInfoRepository;
    private final MockPaymentProcessor paymentProcessor;
    private final EventManager eventManager;

    public PRCModule(OrderRepository orderRepository, RoleValidator roleValidator,
                     PaymentInfoRepository paymentInfoRepository, MockPaymentProcessor paymentProcessor,
                     EventManager eventManager) {
        this.orderRepository = orderRepository;
        this.roleValidator = roleValidator;
        this.paymentInfoRepository = paymentInfoRepository;
        this.paymentProcessor = paymentProcessor;
        this.eventManager = eventManager;
    }

    @Transactional(readOnly = true)
    public List<OrderData> getAllOrdersInProgress(User user) {
        //return all orders in progress, i.e. NOT canceled, delivered, rejected
        roleValidator.requireRole(user, Role.OPERATOR, "view all orders in progress");
        Collection<OrderStatus> StatusFinal = List.of(
                OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.REFUNDED);
        return orderRepository.findByStatusNotIn(StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderData> getAllOrdersInProgressExceptReady(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all orders in progress");
        Collection<OrderStatus> StatusFinal = List.of(
                OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.REFUNDED, OrderStatus.READY);
        return orderRepository.findByStatusNotIn(StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderData> getAllOrdersReady(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all orders in progress");
        return orderRepository.findByStatus(OrderStatus.READY).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    //Order History
    @Transactional(readOnly = true)
    public List<OrderData> getOrderHistory(User user) {
        return switch (user.getRole()) {
            case STORE -> getStoreHistory(user);
            case COURIER -> getCourierHistory(user);
            case CUSTOMER -> getCustomerHistory(user);
            case OPERATOR -> getFullHistory(user);
            default -> throw new PermissionDeniedException("Access denied");
        };
    }

    @Transactional
    public OrderData refundOrder(User user, Long orderId, RefundRequest request) {
        roleValidator.requireRole(user, Role.OPERATOR, "refund orders");
        if (request == null) {
            throw new IllegalArgumentException("Refund details are required");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("Refund amount is required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("A refund reason is required");
        }
        if (request.reason().trim().length() > 500) {
            throw new IllegalArgumentException("Refund reason must be 500 characters or fewer");
        }

        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Refund amount may have at most two decimal places");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " does not exist"));
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELED) {
            throw new OrderRuleViolationException("Only delivered or canceled orders can be refunded");
        }
        if (order.getRefund() != null || order.getStatus() == OrderStatus.REFUNDED) {
            throw new OrderRuleViolationException("Order #" + orderId + " has already been refunded");
        }
        if (!Boolean.TRUE.equals(order.getIsCharged())) {
            throw new OrderRuleViolationException("Order #" + orderId + " has no recorded payment to refund");
        }
        if (amount.compareTo(order.getTotalCost()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed the order's total charge");
        }

        PaymentInfo paymentInfo = paymentInfoRepository.findById(order.getCustomer().getAccountId())
                .orElseThrow(() -> new OrderRuleViolationException("Customer payment method is unavailable"));
        String processorReference = paymentProcessor.refund(paymentInfo, orderId, amount);
        Refund refund = new Refund(order, request.reason().trim(), amount, user.getAccountId(), processorReference);
        order.markRefunded(refund);
        orderRepository.save(order);

        eventManager.logEvent("Order #" + orderId + " refunded by operator " + user.getAccountId()
                + " for " + amount.toPlainString());
        return OrderData.from(order);
    }

    private List<OrderData> getFullHistory(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view full order ledger");
        Collection<OrderStatus> StatusFinal = List.of(
                OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.REFUNDED);
        return orderRepository.findByStatusIn(StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt, Comparator.reverseOrder()).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    private List<OrderData> getCustomerHistory(User user) {
        roleValidator.requireRole(user, Role.CUSTOMER, "view customer order history");
        Collection<OrderStatus> StatusFinal = List.of(
                OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.REFUNDED);
        return orderRepository.findByCustomer_AccountIdAndStatusIn(user.getAccountId(), StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt, Comparator.reverseOrder()).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    private List<OrderData> getStoreHistory(User user) {
        roleValidator.requireRole(user, Role.STORE, "view store order history");
        Collection<OrderStatus> StatusFinal = List.of(
                OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.REFUNDED);
        return orderRepository.findByStore_AccountIdAndStatusIn(user.getAccountId(), StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt, Comparator.reverseOrder()).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    private List<OrderData> getCourierHistory(User user) {
        roleValidator.requireRole(user, Role.COURIER, "view courier order history");
        Collection<OrderStatus> StatusFinal = List.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED);
        return orderRepository.findByCourier_AccountIdAndStatusIn(user.getAccountId(), StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getDeliveredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }
}
