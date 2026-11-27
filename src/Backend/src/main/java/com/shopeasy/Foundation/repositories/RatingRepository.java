package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByOrder_OrderId(Long orderId);

    long countByStore_AccountId(Long storeId);

    long countByCourier_AccountId(Long courierId);

    @Query("SELECT AVG(r.storeScore) FROM Rating r WHERE r.store.accountId = :storeId")
    Double averageStoreScore(@Param("storeId") Long storeId);

    @Query("SELECT AVG(r.courierScore) FROM Rating r WHERE r.courier.accountId = :courierId")
    Double averageCourierScore(@Param("courierId") Long courierId);
}
