package com.lms.order.service;

import com.lms.order.model.Promotion;
import com.lms.order.repository.CoursePromotionRepository;
import com.lms.order.repository.PromotionRepository;
import com.lms.order.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private CoursePromotionRepository coursePromotionRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @Test
    @DisplayName("1. increaseUsageCountBatch with NULL list returns early without calling repository")
    void increaseUsageCountBatch_withNullList() {
        assertDoesNotThrow(() -> promotionService.increaseUsageCountBatch(null));
        verify(promotionRepository, never()).findAllByIds(any());
        verify(promotionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("2. increaseUsageCountBatch with EMPTY list returns early without calling repository")
    void increaseUsageCountBatch_withEmptyList() {
        assertDoesNotThrow(() -> promotionService.increaseUsageCountBatch(Collections.emptyList()));
        verify(promotionRepository, never()).findAllByIds(any());
        verify(promotionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("3. increaseUsageCountBatch with SINGLE promotion ID increments count by 1")
    void increaseUsageCountBatch_withSingleId() {
        String promoId = "PROMO-001";
        Promotion promo = Promotion.builder()
                .id(promoId)
                .code("DISCOUNT10")
                .usageCount(5)
                .build();

        when(promotionRepository.findAllByIds(List.of(promoId))).thenReturn(List.of(promo));

        promotionService.increaseUsageCountBatch(List.of(promoId));

        verify(promotionRepository).findAllByIds(List.of(promoId));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Promotion>> captor = ArgumentCaptor.forClass(List.class);
        verify(promotionRepository).saveAll(captor.capture());

        List<Promotion> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(6, saved.getFirst().getUsageCount());
    }

    @Test
    @DisplayName("4. increaseUsageCountBatch with MULTIPLE distinct promotion IDs increments each by 1")
    void increaseUsageCountBatch_withMultipleDistinctIds() {
        Promotion promo1 = Promotion.builder().id("P-1").code("CODE1").usageCount(10).build();
        Promotion promo2 = Promotion.builder().id("P-2").code("CODE2").usageCount(0).build();
        Promotion promo3 = Promotion.builder().id("P-3").code("CODE3").usageCount(42).build();

        List<String> ids = List.of("P-1", "P-2", "P-3");
        when(promotionRepository.findAllByIds(ids)).thenReturn(List.of(promo1, promo2, promo3));

        promotionService.increaseUsageCountBatch(ids);

        assertEquals(11, promo1.getUsageCount());
        assertEquals(1, promo2.getUsageCount());
        assertEquals(43, promo3.getUsageCount());
        verify(promotionRepository).saveAll(any());
    }

    @Test
    @DisplayName("5. increaseUsageCountBatch with DUPLICATE promotion IDs in list increments count proportionally")
    void increaseUsageCountBatch_withDuplicateIds() {
        Promotion promo1 = Promotion.builder().id("P-1").code("CODE1").usageCount(5).build();
        Promotion promo2 = Promotion.builder().id("P-2").code("CODE2").usageCount(10).build();

        // P-1 appears twice, P-2 appears once
        List<String> ids = List.of("P-1", "P-1", "P-2");
        when(promotionRepository.findAllByIds(ids)).thenReturn(List.of(promo1, promo2));

        promotionService.increaseUsageCountBatch(ids);

        // promo1 should be incremented by 2 (5 + 2 = 7)
        assertEquals(7, promo1.getUsageCount());
        // promo2 should be incremented by 1 (10 + 1 = 11)
        assertEquals(11, promo2.getUsageCount());
        verify(promotionRepository).saveAll(any());
    }

    @Test
    @DisplayName("6. increaseUsageCountBatch when entity usageCount is NULL initializes to 1")
    void increaseUsageCountBatch_withNullUsageCountInEntity() {
        Promotion promo = Promotion.builder()
                .id("P-NULL")
                .code("NULL_COUNT")
                .usageCount(null) // null in DB
                .build();

        when(promotionRepository.findAllByIds(List.of("P-NULL"))).thenReturn(List.of(promo));

        promotionService.increaseUsageCountBatch(List.of("P-NULL"));

        assertEquals(1, promo.getUsageCount());
        verify(promotionRepository).saveAll(any());
    }

    @Test
    @DisplayName("7. increaseUsageCountBatch when NO promotions found in DB logs warning and does not crash")
    void increaseUsageCountBatch_withNoPromotionsFound() {
        when(promotionRepository.findAllByIds(List.of("NOT_FOUND"))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> promotionService.increaseUsageCountBatch(List.of("NOT_FOUND")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Promotion>> captor = ArgumentCaptor.forClass(List.class);
        verify(promotionRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("8. increaseUsageCountBatch with PARTIAL match updates only existing promotions")
    void increaseUsageCountBatch_withPartialMatches() {
        Promotion promo1 = Promotion.builder().id("P-EXISTS").code("EXISTS").usageCount(3).build();

        List<String> ids = List.of("P-EXISTS", "P-NONEXISTENT");
        when(promotionRepository.findAllByIds(ids)).thenReturn(List.of(promo1));

        promotionService.increaseUsageCountBatch(ids);

        assertEquals(4, promo1.getUsageCount());
        verify(promotionRepository).saveAll(List.of(promo1));
    }

    @Test
    @DisplayName("9. increaseUsageCountBatch with list containing null element")
    void increaseUsageCountBatch_withListContainingNullElement() {
        Promotion promo1 = Promotion.builder().id("P-VALID").code("VALID").usageCount(1).build();

        List<String> ids = new ArrayList<>(Arrays.asList("P-VALID", null));
        when(promotionRepository.findAllByIds(ids)).thenReturn(List.of(promo1));

        assertDoesNotThrow(() -> promotionService.increaseUsageCountBatch(ids));

        assertEquals(2, promo1.getUsageCount());
        verify(promotionRepository).saveAll(List.of(promo1));
    }
}
