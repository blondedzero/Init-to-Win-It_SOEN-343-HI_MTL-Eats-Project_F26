package com.shopeasy;

import com.shopeasy.Domain.GS.StoreContextData;
import com.shopeasy.Foundation.entities.GroceryStore;
import com.shopeasy.Foundation.entities.StoreStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterisation tests for StoreContextData (Phase IV, step 2).
 * Pins the mapping from the entity to the response record before it is refactored.
 */
class StoreContextDataCharacterisationTest {

    private static GroceryStore store() {
        GroceryStore s = new GroceryStore("s@test.com", "hash", "First", "Last",
                "1 Rue Test", -73.5, 45.5, "Test Market");
        s.setStatus(StoreStatus.PAUSED);
        ReflectionTestUtils.setField(s, "accountId", 7L);
        return s;
    }

    @Test
    void from_entity_copiesEveryFieldAndStatusName() {
        StoreContextData data = StoreContextData.from(store());

        assertThat(data.id()).isEqualTo(7L);
        assertThat(data.storeName()).isEqualTo("Test Market");
        assertThat(data.address()).isEqualTo("1 Rue Test");
        assertThat(data.latitude()).isEqualTo(45.5);
        assertThat(data.longitude()).isEqualTo(-73.5);
        assertThat(data.status()).isEqualTo("PAUSED");
    }

    @Test
    void from_entity_defaultsOrderCountsToZero() {
        StoreContextData data = StoreContextData.from(store());

        assertThat(data.ordersInProgress()).isZero();
        assertThat(data.ordersReady()).isZero();
    }

    @Test
    void from_entityWithCounts_keepsTheCounts() {
        StoreContextData data = StoreContextData.from(store(), 3, 2);

        assertThat(data.ordersInProgress()).isEqualTo(3);
        assertThat(data.ordersReady()).isEqualTo(2);
        assertThat(data.status()).isEqualTo("PAUSED");
    }
}
