package com.shopeasy.Domain.RVW;

import com.shopeasy.Domain.EventManager;
import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.RoleValidator;
import com.shopeasy.Domain.exceptions.NotFoundException;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.GroceryStoreRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RVWModuleCourierRatingTest {

    @Mock RatingRepository ratingRepository;
    @Mock OrderRepository orderRepository;
    @Mock GroceryStoreRepository groceryStoreRepository;
    @Mock CourierRepository courierRepository;
    @Mock ORDModule ordModule;
    @Mock RoleValidator roleValidator;
    @Mock EventManager eventManager;

    private RVWModule module;

    @BeforeEach
    void setUp() {
        module = new RVWModule(ratingRepository, orderRepository, groceryStoreRepository,
                courierRepository, ordModule, roleValidator, eventManager, new RatingRules(1, 5));
    }

    @Test
    void returnsRoundedAverageAndCountForExistingCourier() {
        when(courierRepository.existsById(12L)).thenReturn(true);
        when(ratingRepository.averageCourierScore(12L)).thenReturn(4.26);
        when(ratingRepository.countByCourier_AccountId(12L)).thenReturn(3L);

        RatingSummaryData summary = module.getCourierRating(12L);

        assertEquals(12L, summary.accountId());
        assertEquals(4.3, summary.averageScore(), 0.0001);
        assertEquals(3L, summary.ratingCount());
        assertEquals(5, summary.maxScore());
    }

    @Test
    void rejectsUnknownCourierId() {
        when(courierRepository.existsById(404L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> module.getCourierRating(404L));
        verifyNoInteractions(ratingRepository);
    }
}
