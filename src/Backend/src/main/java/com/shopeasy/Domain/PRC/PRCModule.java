package com.shopeasy.Domain.PRC;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.RoleValidator;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class PRCModule {
    private final OrderRepository orderRepository;
    private final RoleValidator roleValidator;

    public PRCModule(OrderRepository orderRepository, RoleValidator roleValidator) {
        this.orderRepository = orderRepository;
        this.roleValidator = roleValidator;
    }

    @Transactional(readOnly = true)
    public List<OrderData> getAllOrdersInProgress(User user) {
        //return all orders in progress, i.e. NOT canceled, delivered, rejected
        roleValidator.requireRole(user, Role.OPERATOR, "view all orders in progress");
        Collection<OrderStatus> StatusFinal = List.of(OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED);
        return orderRepository.findByStatusNotIn(StatusFinal).stream()
                .sorted(Comparator.comparing(Order::getPlacedAt).thenComparing(Order::getOrderId))
                .map(OrderData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderData> getAllOrdersInProgressExceptReady(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all orders in progress");
        Collection<OrderStatus> StatusFinal = List.of(OrderStatus.CANCELED, OrderStatus.DELIVERED, OrderStatus.REJECTED, OrderStatus.READY);
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
}
