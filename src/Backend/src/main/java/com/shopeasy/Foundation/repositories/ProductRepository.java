package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Product;
import com.shopeasy.Foundation.entities.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStore_AccountId(Long storeId);
    List<Product> findByStore_AccountIdAndStatus(Long storeId, ProductStatus status);
}
