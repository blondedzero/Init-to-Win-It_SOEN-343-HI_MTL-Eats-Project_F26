package com.shopeasy.Domain.GS;


import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.Product;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.StoreStatus;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class GSModule {

    private final GroceryStoreRepository groceryStoreRepository;
    private final ProductRepository productRepository;

    public GSModule(GroceryStoreRepository groceryStoreRepository, ProductRepository productRepository) {
        this.groceryStoreRepository = groceryStoreRepository;
        this.productRepository = productRepository;
    }

    public List<GroceryStore> listOpenStores() {
        return groceryStoreRepository.findByStatus(StoreStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public StoreMenuData getMenu(User user, Long storeId) {
        if (user.getRole() == Role.COURIER) {
            throw new PermissionDeniedException("A courier account cannot browse store menus");
        }
        if (user.getRole() == Role.STORE && !user.getAccountId().equals(storeId)) {
            throw new PermissionDeniedException("Stores can only see their own menu");
        }

        GroceryStore store = groceryStoreRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store " + storeId + " does not exist"));

        List<ProductData> products = productRepository.findByStore_AccountId(storeId).stream()
                .sorted(Comparator.comparing(Product::getName))
                .map(p -> new ProductData(p.getProductId(), p.getName(), p.getDescription(),
                        p.getCategory().getCategoryName(), p.getPrice(), p.getStockQuantity(),
                        p.getStatus().name()))
                .toList();

        StoreContextData storeData = new StoreContextData(store.getAccountId(), store.getStoreName(),
                store.getAddress(), store.getLatitude(), store.getLongitude(), store.getStatus().name());
        return new StoreMenuData(storeData, products);
    }
}
