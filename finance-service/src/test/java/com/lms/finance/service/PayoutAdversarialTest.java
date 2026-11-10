package com.lms.finance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import com.lms.common.exception.AppException;
import com.lms.common.util.TimeConverter;
import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.request.ProcessPayoutRequestDto;
import com.lms.finance.dto.response.InstructorAnalyticsOverviewResponse;
import com.lms.finance.dto.response.PayoutRequestResponse;
import com.lms.finance.entity.BankAccount;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.PayoutRequest;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.enums.PayoutStatus;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.BankAccountRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.PayoutRequestRepository;
import com.lms.finance.repository.PayoutRequestSpecification;
import com.lms.finance.service.impl.PayoutServiceImpl;
import jakarta.persistence.criteria.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PayoutAdversarialTest {

    @Mock private PayoutRequestRepository repository;
    @Mock private InstructorBalanceRepository balanceRepository;
    @Mock private BalanceHistoryRepository historyRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        ReflectionTestUtils.setField(payoutService, "MIN_PAYOUT_AMOUNT", "500000");
        ReflectionTestUtils.setField(payoutService, "DEFAULT_CURRENCY", "VND");
    }

    // ========================================================================
    // 1. ADVERSARIAL STATE MACHINE TRANSITION CHALLENGES
    // ========================================================================
    @Nested
    @DisplayName("Adversarial: State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Edge Case: Approving an already SUCCESS request must fail with INVALID_PAYOUT_REQUEST_STATUS")
        void approve_WhenAlreadySuccess_ThrowsException() {
            PayoutRequest payout = PayoutRequest.builder()
                    .id("payout-101")
                    .instructorId("instr-01")
                    .amount(new BigDecimal("1000000"))
                    .status(PayoutStatus.SUCCESS)
                    .build();

            when(repository.findById("payout-101")).thenReturn(Optional.of(payout));
            when(balanceRepository.findByInstructorId("instr-01")).thenReturn(Optional.of(new InstructorBalance()));

            ProcessPayoutRequestDto dto = new ProcessPayoutRequestDto();
            dto.setPayoutId("payout-101");
            dto.setBankReferenceNo("REF-101");

            AppException ex = assertThrows(AppException.class, () ->
                    payoutService.approvePayoutRequest("admin-1", dto)
            );
            assertEquals(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Edge Case: Approving an already REJECTED request must fail with INVALID_PAYOUT_REQUEST_STATUS")
        void approve_WhenAlreadyRejected_ThrowsException() {
            PayoutRequest payout = PayoutRequest.builder()
                    .id("payout-102")
                    .instructorId("instr-01")
                    .amount(new BigDecimal("1000000"))
                    .status(PayoutStatus.REJECTED)
                    .build();

            when(repository.findById("payout-102")).thenReturn(Optional.of(payout));
            when(balanceRepository.findByInstructorId("instr-01")).thenReturn(Optional.of(new InstructorBalance()));

            ProcessPayoutRequestDto dto = new ProcessPayoutRequestDto();
            dto.setPayoutId("payout-102");
            dto.setBankReferenceNo("REF-102");

            AppException ex = assertThrows(AppException.class, () ->
                    payoutService.approvePayoutRequest("admin-1", dto)
            );
            assertEquals(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Edge Case: Rejecting an already SUCCESS request must fail with INVALID_PAYOUT_REQUEST_STATUS")
        void reject_WhenAlreadySuccess_ThrowsException() {
            PayoutRequest payout = PayoutRequest.builder()
                    .id("payout-103")
                    .instructorId("instr-01")
                    .amount(new BigDecimal("1000000"))
                    .status(PayoutStatus.SUCCESS)
                    .build();

            when(repository.findById("payout-103")).thenReturn(Optional.of(payout));

            ProcessPayoutRequestDto dto = new ProcessPayoutRequestDto();
            dto.setPayoutId("payout-103");
            dto.setRejectReason("Duplicate");

            AppException ex = assertThrows(AppException.class, () ->
                    payoutService.rejectPayoutRequest("admin-1", dto)
            );
            assertEquals(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Edge Case: Rejecting an already REJECTED request must fail with INVALID_PAYOUT_REQUEST_STATUS")
        void reject_WhenAlreadyRejected_ThrowsException() {
            PayoutRequest payout = PayoutRequest.builder()
                    .id("payout-104")
                    .instructorId("instr-01")
                    .amount(new BigDecimal("1000000"))
                    .status(PayoutStatus.REJECTED)
                    .build();

            when(repository.findById("payout-104")).thenReturn(Optional.of(payout));

            ProcessPayoutRequestDto dto = new ProcessPayoutRequestDto();
            dto.setPayoutId("payout-104");
            dto.setRejectReason("Duplicate");

            AppException ex = assertThrows(AppException.class, () ->
                    payoutService.rejectPayoutRequest("admin-1", dto)
            );
            assertEquals(FinanceErrorCode.INVALID_PAYOUT_REQUEST_STATUS, ex.getErrorCode());
        }
    }

    // ========================================================================
    // 2. DTO CONTRACT & FIELD INTEGRITY CHALLENGES
    // ========================================================================
    @Nested
    @DisplayName("Adversarial: DTO Contract & Fields Integrity")
    class DtoContractTests {

        @Test
        @DisplayName("Check PayoutRequestResponse has instructorId, rejectReason, bankReferenceNo")
        void testPayoutRequestResponseFields() {
            PayoutRequestResponse resp = PayoutRequestResponse.builder()
                    .payoutId("p-001")
                    .instructorId("instr-999")
                    .amount(new BigDecimal("600000"))
                    .status("PENDING")
                    .bankCode("VCB")
                    .accountNumber("0123456789")
                    .accountName("TRAN VAN TEST")
                    .rejectReason("Sai thông tin")
                    .bankReferenceNo("FT260000123")
                    .createdAt(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();

            assertEquals("instr-999", resp.getInstructorId());
            assertEquals("Sai thông tin", resp.getRejectReason());
            assertEquals("FT260000123", resp.getBankReferenceNo());
            assertEquals(new BigDecimal("600000"), resp.getAmount());
            assertEquals("VCB", resp.getBankCode());
        }

        @Test
        @DisplayName("Check InstructorAnalyticsOverviewResponse has averageRating and totalReviews")
        void testInstructorAnalyticsOverviewFields() {
            InstructorAnalyticsOverviewResponse overview = InstructorAnalyticsOverviewResponse.builder()
                    .totalStudents(120)
                    .totalCourses(5)
                    .thisMonthRevenue(new BigDecimal("15000000"))
                    .lastMonthRevenue(new BigDecimal("10000000"))
                    .growthRate(50.0)
                    .thisMonthSales(30)
                    .availableBalance(new BigDecimal("8000000"))
                    .pendingBalance(new BigDecimal("2000000"))
                    .actualBalance(new BigDecimal("10000000"))
                    .allTimeRevenue(new BigDecimal("50000000"))
                    .averageRating(4.9)
                    .totalReviews(88L)
                    .build();

            assertEquals(4.9, overview.getAverageRating());
            assertEquals(88L, overview.getTotalReviews());
            assertEquals(120, overview.getTotalStudents());
            assertEquals(5, overview.getTotalCourses());
        }
    }

    // ========================================================================
    // 3. FILTER VALIDATION & SPECIFICATION STATUS MAPPING CHALLENGES
    // ========================================================================
    @Nested
    @DisplayName("Adversarial: PayoutFilterRequest & Specification")
    class FilterAndSpecificationTests {

        @Test
        @DisplayName("Validation: Status allows PENDING, SUCCESS, APPROVED, REJECTED")
        void testValidStatuses() {
            String[] valids = {"PENDING", "SUCCESS", "APPROVED", "REJECTED"};
            for (String s : valids) {
                PayoutFilterRequest req = new PayoutFilterRequest();
                req.setStatus(s);
                Set<ConstraintViolation<PayoutFilterRequest>> violations = validator.validate(req);
                assertTrue(violations.isEmpty(), "Status '" + s + "' should be valid but had violations: " + violations);
            }
        }

        @Test
        @DisplayName("Validation: Invalid status is rejected by Jakarta Validation")
        void testInvalidStatuses() {
            String[] invalids = {"CANCELLED", "FAILED", "PROCESSING", "approved", "pending"};
            for (String s : invalids) {
                PayoutFilterRequest req = new PayoutFilterRequest();
                req.setStatus(s);
                Set<ConstraintViolation<PayoutFilterRequest>> violations = validator.validate(req);
                assertFalse(violations.isEmpty(), "Status '" + s + "' should be invalid but was accepted!");
                assertEquals("Invalid Status", violations.iterator().next().getMessage());
            }
        }

        @Test
        @DisplayName("Specification: 'APPROVED' maps to PayoutStatus.SUCCESS in hasStatus()")
        void testSpecificationStatusMapping() {
            Root<PayoutRequest> root = mock(Root.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            Path<Object> statusPath = mock(Path.class);
            Predicate dummyPredicate = mock(Predicate.class);

            when(root.get("status")).thenReturn(statusPath);
            when(cb.equal(statusPath, PayoutStatus.SUCCESS)).thenReturn(dummyPredicate);

            // Test 1: EXACT "APPROVED"
            Specification<PayoutRequest> specApproved = PayoutRequestSpecification.hasStatus("APPROVED");
            Predicate p1 = specApproved.toPredicate(root, query, cb);
            assertNotNull(p1);
            verify(cb).equal(statusPath, PayoutStatus.SUCCESS);

            // Test 2: Lowercase "approved" with spaces "  approved  "
            Specification<PayoutRequest> specTrimmed = PayoutRequestSpecification.hasStatus("  approved  ");
            Predicate p2 = specTrimmed.toPredicate(root, query, cb);
            assertNotNull(p2);

            // Test 3: Null status returns conjunction (no filtering)
            Predicate conjunction = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjunction);
            Specification<PayoutRequest> specNull = PayoutRequestSpecification.hasStatus(null);
            assertEquals(conjunction, specNull.toPredicate(root, query, cb));

            // Test 4: Blank status returns conjunction
            Specification<PayoutRequest> specBlank = PayoutRequestSpecification.hasStatus("   ");
            assertEquals(conjunction, specBlank.toPredicate(root, query, cb));
        }
    }
}
