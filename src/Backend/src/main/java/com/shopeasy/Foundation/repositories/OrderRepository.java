package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomer_AccountId(Long customerId);
    List<Order> findByCustomer_AccountIdAndStatus(Long customerId, OrderStatus status);

    List<Order> findByStore_AccountId(Long storeId);
    List<Order> findByStore_AccountIdAndStatus(Long storeId, OrderStatus status);

    List<Order> findByCourier_AccountId(Long courierId);
    List<Order> findByCourier_AccountIdAndStatus(Long courierId, OrderStatus status);

    List<Order> findByStore_AccountIdOrderByPlacedAtDesc(Long storeId);
}
