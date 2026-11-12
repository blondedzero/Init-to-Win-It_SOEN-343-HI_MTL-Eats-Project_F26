package com.shopeasy.Application;

import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.GS.GSModule;
import com.shopeasy.Domain.GS.StoreMenuData;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shopeasy.Domain.GS.StoreContextData;


import java.util.List;

@RestController                          // "my return values become JSON"
@RequestMapping("/api/stores")
public class StoreController {
    private final GSModule gsModule;
    private final ORDModule ordModule;
    private final RequestAuthenticator authenticator;

    public StoreController(GSModule gsModule, ORDModule ordModule, RequestAuthenticator authenticator) {
        this.gsModule = gsModule;
        this.ordModule = ordModule;
        this.authenticator = authenticator;
    }

    // Note: In Phase 3, the mapping here has been updated to pass the store's active order queue size into StoreContextData for map rendering - NC
    @GetMapping
    public List<StoreContextData> getGSModule(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.authenticate(authorization);
        return gsModule.listOpenStores().stream()
                .map(s -> new StoreContextData(s.getAccountId(), s.getStoreName(), s.getAddress(),
                        s.getLatitude(), s.getLongitude(), s.getStatus().name(), ordModule.getPreparationQueue(s).size()))
                .toList();
    }

    @GetMapping("/{storeId}/products")
    public StoreMenuData getMenu(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long storeId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getMenu(user, storeId);
    }
}
