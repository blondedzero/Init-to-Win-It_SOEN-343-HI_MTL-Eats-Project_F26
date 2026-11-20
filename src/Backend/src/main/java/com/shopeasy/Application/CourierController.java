package com.shopeasy.Application;

import com.shopeasy.Domain.DLV.DLVModule;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courier")
public class CourierController extends Controller {

    private final DLVModule dlvModule;

    public CourierController(RequestAuthenticator authenticator, DLVModule dlvModule) {
        super(authenticator);
        this.dlvModule = dlvModule;
    }

    @PostMapping("/orders/{orderId}/accept")
    public OrderResult acceptOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.acceptOffer(user, orderId);
    }

    @PostMapping("/orders/{orderId}/decline")
    public OrderResult declineOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.declineOffer(user, orderId);
    }
}
