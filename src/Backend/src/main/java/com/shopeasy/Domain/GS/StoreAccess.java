package com.shopeasy.Domain.GS;

import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import org.springframework.stereotype.Component;

// R3 (Extract Class): the "which store is calling?" step shared by GSModule and
// CatalogModule. A store representative only ever acts on their own store, and
// the store id comes from the authenticated user, never from the request.
@Component
public class StoreAccess {

    private final GroceryStoreRepository groceryStoreRepository;
    private final RoleValidator roleValidator;

    public StoreAccess(GroceryStoreRepository groceryStoreRepository, RoleValidator roleValidator) {
        this.groceryStoreRepository = groceryStoreRepository;
        this.roleValidator = roleValidator;
    }

    public GroceryStore requireOwnStore(User user, String action) {
        roleValidator.requireRole(user, Role.STORE, action);
        return groceryStoreRepository.findById(user.getAccountId())
                .orElseThrow(() -> new NotFoundException("Store " + user.getAccountId() + " does not exist"));
    }
}
