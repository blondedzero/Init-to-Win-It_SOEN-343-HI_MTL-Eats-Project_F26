package com.shopeasy.Application;

import com.shopeasy.Domain.GS.GSModule;
import com.shopeasy.Domain.GS.StoreContextData;
import com.shopeasy.Domain.ORD.CourierDisplayData;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.OperatorRequestsModule;
import com.shopeasy.Domain.PRC.PRCModule;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operator")
public class OperatorRequestsController {
    private final RequestAuthenticator authenticator;
    private final PRCModule prcModule;
    private final GSModule gsModule;
    private final OperatorRequestsModule ORM;

    public OperatorRequestsController(RequestAuthenticator authenticator, PRCModule prcModule, GSModule gsModule, OperatorRequestsModule ORM) {
        this.authenticator = authenticator;
        this.prcModule = prcModule;
        this.gsModule = gsModule;
        this.ORM = ORM;
    }

    //Store Operations
    @GetMapping("/stores/all")
    public List<StoreContextData> getAllStores(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getAllStores(user);
    }

    @GetMapping("/stores/open-paused")
    public List<StoreContextData> getAllOpenAndPausedStores(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getAllOpenAndPausedStores(user);
    }

    public record UpdateStoreStatusBody(int queueSize){}

    @PostMapping("stores/{storeId}/open")
    public StoreContextData openStore(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long storeId, @RequestBody UpdateStoreStatusBody body) {
        User user = authenticator.authenticate(authorization);
        return gsModule.openStore(user, storeId, body.queueSize());
    }

    @PostMapping("stores/{storeId}/pause")
    public StoreContextData pauseStore(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long storeId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.pauseStore(user, storeId);
    }

    @PostMapping("stores/{storeId}/close")
    public StoreContextData closeStore(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long storeId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.closeStore(user, storeId);
    }

    //Order Operations
    @GetMapping("/orders/in-progress")
    public List<OrderData> getAllOrdersInProgress(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return prcModule.getAllOrdersInProgress(user);
    }

    @GetMapping("/orders/ready")
    public List<OrderData> getAllOrdersReady(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return prcModule.getAllOrdersReady(user);
    }

    //Courier Operations
    @GetMapping("/couriers/on-duty")
    public List<CourierDisplayData> getAllCouriersOnDuty(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return ORM.getAllCouriersOnDuty(user);
    }

    @GetMapping("/couriers/idle")
    public List<CourierDisplayData> getAllCouriersIdle(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return ORM.getAllCouriersIdle(user);
    }

}
