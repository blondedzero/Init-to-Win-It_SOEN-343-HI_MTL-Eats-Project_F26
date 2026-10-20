package com.shopeasy.Foundation.repositories;

import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroceryStoreRepository extends JpaRepository<GroceryStore, Long> {
    boolean existsByAddressAndStoreName(String address, String name);
    Optional<GroceryStore> findByAddressAndStoreName(String address, String name);

    List<GroceryStore> findByStoreName(String name);
}
