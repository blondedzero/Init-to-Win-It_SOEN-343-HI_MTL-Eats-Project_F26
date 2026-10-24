package com.shopeasy.Application;


import com.shopeasy.Domain.GS.GSModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shopeasy.Domain.GS.StoreContextData;


import java.util.List;

@RestController                          // "my return values become JSON"
@RequestMapping("/api/stores")
public class StoreController {
    private final GSModule gsModule;

    public StoreController(GSModule gsModule) {
        this.gsModule = gsModule;
    }

    @GetMapping
    public List<StoreContextData> getGSModule() {
        return gsModule.listOpenStores().stream()
                .map(s -> new StoreContextData(s.getAccountId(), s.getStoreName(), s.getAddress(),
                        s.getLatitude(), s.getLongitude(), s.getStatus().name()))
                .toList();
    }
}
