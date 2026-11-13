package com.shopeasy.Application;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.PRC.PRCModule;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LedgerController {
    private final RequestAuthenticator authenticator;
    private final PRCModule prcModule;

    public LedgerController(RequestAuthenticator authenticator, PRCModule prcModule) {
        this.authenticator = authenticator;
        this.prcModule = prcModule;
    }

    @GetMapping("/orders/in-progress")
    public List<OrderData> getAllOrdersInProgress(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return prcModule.getAllOrdersInProgress(user);
    }
}
