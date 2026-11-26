package com.shopeasy.Foundation.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTest {

    @Test
    void courierAssignmentRemainsPendingUntilAcceptedAndResetsWhenReassigned() {
        Customer customer = mock(Customer.class);
        when(customer.getAddress()).thenReturn("Test address");
        Order order = new Order(customer, mock(GroceryStore.class));
        order.markAccepted();
        order.markPreparing();
        order.markReady();
        order.setCourier(new Courier("first@example.com", "hash", "First", "Courier", 0, 0));

        assertFalse(order.isCourierAccepted());

        order.acceptCourierAssignment();

        assertTrue(order.isCourierAccepted());

        order.setCourier(new Courier("second@example.com", "hash", "Second", "Courier", 0, 0));

        assertFalse(order.isCourierAccepted());
    }
}
