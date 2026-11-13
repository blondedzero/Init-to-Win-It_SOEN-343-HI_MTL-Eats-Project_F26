package com.shopeasy.Domain.ORD;

import com.shopeasy.Foundation.entities.Courier;

public record CourierDisplayData(long courierId, String name, String status, double longitude, double latitude) {
    public static CourierDisplayData from(Courier c) {
        return new CourierDisplayData(c.getAccountId(), c.getFullName(), c.getStatus().name(), c.getLongitude(), c.getLatitude());
    }
}
