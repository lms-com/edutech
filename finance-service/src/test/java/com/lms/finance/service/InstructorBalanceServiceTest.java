package com.lms.finance.service;

import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.RevenueShare;
import com.lms.finance.enums.RevenueShareStatus;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.RevenueShareRepository;
import com.lms.finance.service.impl.InstructorBalanceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstructorBalanceServiceTest {
    @Mock
    private InstructorBalanceRepository balanceRepository;

    @Mock
    private BalanceHistoryRepository historyRepository;

    @Mock
    private RevenueShareRepository revenueShareRepository;

    @InjectMocks
    private InstructorBalanceServiceImpl balanceService;

    @Test
    public void test_releaseRevenue_Success () {
        // Tao du lieu gia
        String instructorId = "test_inst_01";
        // InstructorBalance
        InstructorBalance mockBalance = InstructorBalance.builder()
                .id("test_inst_bal_01")
                .instructorId(instructorId)
                .actualBalance(BigDecimal.valueOf(10000000L))
                .availableBalance(BigDecimal.valueOf(3900000L))
                .blockedBalance(BigDecimal.valueOf(4000000L))
                .pendingBalance(BigDecimal.valueOf(2100000L))
                .currencyCode("VND")
                .build();
        // RevenueShare
        List<RevenueShare> revenueShares = List.of(RevenueShare.builder()
                .id("test_revenue_01")
                .currencyCode("VND")
                .instructorId(instructorId)
                .commissionRate(BigDecimal.valueOf(0.7))
                .grossAmount(BigDecimal.valueOf(3000000L))
                .instructorAmount(BigDecimal.valueOf(2100000L))
                .platformFee(BigDecimal.valueOf(900000L))
                .orderId("blabla_order")
                .courseId("blabla_course")
                .status(RevenueShareStatus.HOLDING)
                .originalRevenueId(null)
                .idempotencyKey("blabla_itempotency")
                .releaseAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build()
        );

        // Dinh nghia hanh vi khi goi repository:
        when(balanceRepository.findByInstructorIdIn(Set.of(instructorId)))
                .thenReturn(List.of(mockBalance));

        // Tien hanh xu li release:
        balanceService.releaseRevenue(revenueShares);

        // Kiem tra:
        assertEquals(0, mockBalance.getPendingBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, mockBalance.getAvailableBalance().compareTo(BigDecimal.valueOf(6000000L)));
        assertEquals(RevenueShareStatus.RELEASED, revenueShares.getFirst().getStatus());

        // 5. Verify (Kiểm tra xem các hàm lưu DB có được gọi không)
        verify(balanceRepository, times(1)).saveAll(any());
        verify(historyRepository, times(1)).saveAll(any());
        verify(revenueShareRepository, times(1)).saveAll(any());
    }
}
