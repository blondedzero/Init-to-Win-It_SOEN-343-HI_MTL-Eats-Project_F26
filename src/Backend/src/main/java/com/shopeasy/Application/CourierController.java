package com.shopeasy.Application;

import com.shopeasy.Domain.DLV.DLVModule;
import com.shopeasy.Domain.DLV.CourierDashboardData;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Foundation.entities.CourierStatus;
import com.shopeasy.Foundation.entities.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courier")
public class CourierController extends Controller {

    private final DLVModule dlvModule;

    public CourierController(RequestAuthenticator authenticator, DLVModule dlvModule) {
        super(authenticator);
        this.dlvModule = dlvModule;
    }

    @GetMapping("/me")
    public CourierDashboardData getDashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.getDashboard(user);
    }

    record CourierStatusRequest(CourierStatus status) {}

    @PostMapping("/me/status")
    public CourierDashboardData changeStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) CourierStatusRequest request) {
        User user = authenticator.authenticate(authorization);
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("A courier status is required");
        }
        switch (request.status()) {
            case IDLE -> dlvModule.goOnDuty(user);
            case OFFLINE -> dlvModule.goOffDuty(user);
            default -> throw new IllegalArgumentException("Courier status can only be changed to IDLE or OFFLINE");
        }
        return dlvModule.getDashboard(user);
    }

    @GetMapping("/orders/offers")
    public List<OrderData> getOffers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.getPendingOffers(user);
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

    @PostMapping("/orders/{orderId}/cancel-assignment")
    public OrderResult cancelAcceptedAssignment(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.cancelAcceptedAssignment(user, orderId);
    }

    @PostMapping("/orders/{orderId}/pickup")
    public OrderResult confirmPickup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.confirmPickup(user, orderId);
    }

    @PostMapping("/orders/{orderId}/deliver")
    public OrderResult confirmDelivery(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return dlvModule.confirmDelivery(user, orderId);
    }
}
