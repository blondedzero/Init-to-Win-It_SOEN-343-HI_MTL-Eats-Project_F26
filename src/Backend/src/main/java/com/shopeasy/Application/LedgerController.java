package com.shopeasy.Application;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.PRC.PRCModule;
import com.shopeasy.Domain.PRC.RefundRequest;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prc")
public class LedgerController extends Controller{

    private final PRCModule prcModule;
    public LedgerController(RequestAuthenticator authenticator, PRCModule prcModule) {
        super(authenticator);
        this.prcModule = prcModule;
    }

    @GetMapping("/history")
    public List<OrderData> getOrderHistory(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return prcModule.getOrderHistory(user);
    }

    @PostMapping("/orders/{orderId}/refund")
    public OrderData refundOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long orderId, @RequestBody RefundRequest request) {
        User user = authenticator.authenticate(authorization);
        return prcModule.refundOrder(user, orderId, request);
    }

}
