package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer_AccountId(Long customerId);
}
