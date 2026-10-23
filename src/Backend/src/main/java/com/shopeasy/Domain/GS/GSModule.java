package com.shopeasy.Domain.GS;


import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.StoreStatus;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GSModule {

    private final GroceryStoreRepository groceryStoreRepository;

    public GSModule(GroceryStoreRepository groceryStoreRepository) {
        this.groceryStoreRepository = groceryStoreRepository;
    }

    public List<GroceryStore> listOpenStores() {
        return groceryStoreRepository.findByStatus(StoreStatus.OPEN);
    }
}
