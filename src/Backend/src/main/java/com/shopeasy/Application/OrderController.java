package com.shopeasy.Application;

import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Domain.ORD.OrderResult;
import com.shopeasy.Domain.ORD.PlaceOrderRequest;
import com.shopeasy.Domain.ORD.QueueData;
import com.shopeasy.Foundation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final ORDModule ordModule;
    private final RequestAuthenticator authenticator;

    public OrderController(ORDModule ordModule, RequestAuthenticator authenticator) {
        this.ordModule = ordModule;
        this.authenticator = authenticator;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResult> placeOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestBody PlaceOrderRequest request) {
        User user = authenticator.authenticate(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(ordModule.placeOrder(user, request));
    }

    @GetMapping("/store/orders/incoming")
    public List<OrderData> getIncomingOrders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        System.out.println("Authorization: " + authorization);
        User user = authenticator.authenticate(authorization);
        return ordModule.getIncomingOrders(user);
    }

    @GetMapping("/store/queue")
    public QueueData getPreparationQueue(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return ordModule.getPreparationQueue(user);
    }

    @PostMapping("/store/orders/{orderId}/accept")
    public OrderResult acceptOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return ordModule.acceptOrder(user, orderId);
    }

    @GetMapping("/store/orders/preparing")
    public List<OrderData> getOrdersInPreparation(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return ordModule.getOrdersInPreparation(user);
    }

    @GetMapping("/store/orders/ready")
    public List<OrderData> getReadyOrders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return ordModule.getReadyOrders(user);
    }

    @PostMapping("/store/orders/{orderId}/start-preparing")
    public OrderResult startPreparing(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return ordModule.startPreparing(user, orderId);
    }

    @PostMapping("/store/orders/{orderId}/ready")
    public OrderResult markReady(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long orderId) {
        User user = authenticator.authenticate(authorization);
        return ordModule.markReady(user, orderId);
    }

    record RejectRequest(String reason) {}

    @PostMapping("/store/orders/{orderId}/reject")
    public OrderResult rejectOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long orderId,
                                   @RequestBody(required = false) RejectRequest request) {
        User user = authenticator.authenticate(authorization);
        return ordModule.rejectOrder(user, orderId, request == null ? null : request.reason());
    }

}
