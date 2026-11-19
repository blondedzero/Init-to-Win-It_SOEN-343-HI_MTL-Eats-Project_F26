package com.shopeasy.Application;


import com.shopeasy.Domain.GS.GSModule;
import com.shopeasy.Domain.GS.ProductData;
import com.shopeasy.Domain.GS.ProductRequest;
import com.shopeasy.Domain.GS.StoreContextData;
import com.shopeasy.Domain.GS.StoreMenuData;
import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Foundation.entities.StoreStatus;
import com.shopeasy.Foundation.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController                          // "my return values become JSON"
@RequestMapping("/api/stores")
public class StoreController extends Controller{
    private final GSModule gsModule;

    public StoreController(GSModule gsModule, RequestAuthenticator authenticator) {
        super(authenticator);
        this.gsModule = gsModule;
    }

    @GetMapping
    public List<StoreContextData> getGSModule(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticator.authenticate(authorization);
        return gsModule.listOpenStores().stream()
                .map(StoreContextData::from)
                .toList();
    }

    @GetMapping("/{storeId}/products")
    public StoreMenuData getMenu(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long storeId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getMenu(user, storeId);
    }

    @GetMapping("/me")
    public StoreContextData getMyStore(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getMyStore(user);
    }

    record StatusRequest(StoreStatus status) {}

    @PostMapping("/me/status")
    public StoreContextData changeStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody StatusRequest request) {
        User user = authenticator.authenticate(authorization);
        return gsModule.changeStatus(user, request == null ? null : request.status());
    }


    @GetMapping("/me/products")
    public List<ProductData> getMyProducts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getMyProducts(user);
    }

    @PostMapping("/me/products")
    public ResponseEntity<ProductData> addProduct(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestBody ProductRequest request) {
        User user = authenticator.authenticate(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(gsModule.addProduct(user, request));
    }

    @PutMapping("/me/products/{productId}")
    public ProductData updateProduct(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable Long productId,
                                     @RequestBody ProductRequest request) {
        User user = authenticator.authenticate(authorization);
        return gsModule.updateProduct(user, productId, request);
    }

    @DeleteMapping("/me/products/{productId}")
    public ProductData removeProduct(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable Long productId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.removeProduct(user, productId);
    }

    @PostMapping("/me/products/{productId}/restore")
    public ProductData restoreProduct(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long productId) {
        User user = authenticator.authenticate(authorization);
        return gsModule.restoreProduct(user, productId);
    }

    @GetMapping("/me/orders/history")
    public List<OrderData> getOrderHistory(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authenticator.authenticate(authorization);
        return gsModule.getOrderHistory(user);
    }

}
