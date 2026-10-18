package com.shopeasy.foundation.repositories;

import com.shopeasy.foundation.entities.Product;
import com.shopeasy.foundation.entities.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStore_AccountId(Long storeId);
    List<Product> findByStore_AccountIdAndStatus(Long storeId, ProductStatus status);
}
