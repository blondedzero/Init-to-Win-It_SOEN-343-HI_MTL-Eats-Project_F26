package com.shopeasy.Domain.DLV;

import com.shopeasy.Foundation.entities.Courier;
import com.shopeasy.Foundation.entities.Customer;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.User;
import org.springframework.stereotype.Component;

@Component
class DistanceCalculator {

    double getDistanceKm(Courier courier, User user) {
        if (courier == null || user == null) {
            throw new IllegalArgumentException("Courier or User is null");
        }

        double userLat;
        double userLon;

        if (user instanceof GroceryStore store) {
            userLat = store.getLatitude();
            userLon = store.getLongitude();
        } else if (user instanceof Customer customer) {
            userLat = customer.getLatitude();
            userLon = customer.getLongitude();
        } else {
            throw new IllegalArgumentException("Unsupported user type: " + user.getClass().getSimpleName());
        }

        return haversineKm(courier.getLatitude(), courier.getLongitude(), userLat, userLon);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
