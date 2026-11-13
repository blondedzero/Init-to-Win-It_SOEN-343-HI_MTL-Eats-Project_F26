package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    List<Order> findByStore_AccountIdAndStatusInOrderByPlacedAtAsc(Long storeId, Collection<OrderStatus> statuses);

    List<Order> findByStatusNotIn(Collection<OrderStatus> statuses);

    @Query("""
        SELECT o.store.accountId AS storeId, o.status AS status, COUNT(o) AS count
        FROM CustomerOrder o
        WHERE o.store.accountId IN :storeIds
          AND o.status IN :statuses
        GROUP BY o.store.accountId, o.status
        """)
    List<StoreOrderCount> countByStoreAndStatus(
            @Param("storeIds") Collection<Long> storeIds,
            @Param("statuses") Collection<OrderStatus> statuses);
}
