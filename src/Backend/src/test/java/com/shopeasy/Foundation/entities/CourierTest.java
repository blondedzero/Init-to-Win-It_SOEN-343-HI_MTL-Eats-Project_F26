package com.shopeasy.Foundation.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourierTest {

    @Test
    void cannotGoOfflineWhileAssignedOrCarryingAnOrder() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);

        for (CourierStatus status : new CourierStatus[]{CourierStatus.ASSIGNED, CourierStatus.CARRYING}) {
            courier.setStatus(status);

            IllegalStateException error = assertThrows(IllegalStateException.class, courier::goOffline);

            assertEquals(status, courier.getStatus());
            assertEquals(
                    "Courier null cannot go offline while an order is assigned or being carried",
                    error.getMessage()
            );
        }
    }

    @Test
    void idleCourierCanGoOffline() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);

        courier.goOffline();

        assertEquals(CourierStatus.OFFLINE, courier.getStatus());
    }

    @Test
    void assignedCourierCanGoOfflineWithPendingOffer() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);
        courier.assignOrder();

        courier.goOfflineWithPendingOffer();

        assertEquals(CourierStatus.OFFLINE, courier.getStatus());
    }

    @Test
    void assigningAnOrderMarksCourierAsAssigned() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);

        courier.assignOrder();

        assertEquals(CourierStatus.ASSIGNED, courier.getStatus());
    }

    @Test
    void decliningAnOfferReturnsCourierToIdle() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);
        courier.setStatus(CourierStatus.ASSIGNED);

        courier.declineOrder();

        assertEquals(CourierStatus.IDLE, courier.getStatus());
    }

    @Test
    void acceptedAssignmentCanBeCanceledBeforePickup() {
        Courier courier = new Courier("courier@example.com", "hash", "Test", "Courier", 0, 0);
        courier.setStatus(CourierStatus.ASSIGNED);

        courier.cancelAssignment();

        assertEquals(CourierStatus.IDLE, courier.getStatus());
    }
}
