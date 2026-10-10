package com.lms.finance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.lms.common.util.TimeConverter;
import com.lms.finance.dto.message.PayoutEventMessage;
import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.dto.request.ProcessPayoutRequestDto;
import com.lms.finance.dto.response.PayoutRequestResponse;
import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.entity.BankAccount;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.PayoutRequest;
import com.lms.finance.enums.PayoutStatus;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.repository.BankAccountRepository;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.PayoutRequestRepository;
import com.lms.finance.service.impl.PayoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private PayoutRequestRepository repository;
    @Mock private InstructorBalanceRepository balanceRepository;
    @Mock private BalanceHistoryRepository historyRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private PayoutRequest sampleEntity;
    private PayoutRequestResponse sampleResponse;
    private ProcessPayoutRequestDto processDto;
    private InstructorBalance sampleBalance;

    private final String PAYOUT_EXCHANGE = "payout.exchange";
    private final String PAYOUT_SUCCESS_ROUTING_KEY = "payout.success.routing.key";

    @BeforeEach
    void setUp() {
        // Khởi tạo dữ liệu mẫu chuẩn cho các test case
        sampleEntity = PayoutRequest.builder()
                .id("payout-001")
                .instructorId("instr-0006-0010-2026")
                .amount(new BigDecimal("500000"))
                .status(PayoutStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        sampleResponse = PayoutRequestResponse.builder()
                .payoutId("payout-001")
                .amount(new BigDecimal("500000"))
                .status("PENDING")
                .build();

        processDto = new ProcessPayoutRequestDto();
        processDto.setPayoutId("payout-001");
        processDto.setTransferAt(TimeConverter.toLocalDateTime(Instant.now(), ZoneId.systemDefault()));
        processDto.setBankReferenceNo("bank-ref-001");

        sampleBalance = new InstructorBalance();
        sampleBalance.setInstructorId("instr-0006-0010-2026");
        sampleBalance.setAvailableBalance(new BigDecimal("2000000"));
        sampleBalance.setPendingBalance(new BigDecimal("1000000"));
        sampleBalance.setBlockedBalance(new BigDecimal("1500000"));

        // Ép kiểu gán giá trị trực tiếp vào biến private MIN_PAYOUT_AMOUNT trong payoutService
        ReflectionTestUtils.setField(payoutService, "MIN_PAYOUT_AMOUNT", "500000");
        ReflectionTestUtils.setField(payoutService, "DEFAULT_CURRENCY", "VND");
    }

    // ========================================================================
    // TRƯỜNG HỢP TEST 1: CHỨC NĂNG TẠO MỚI YÊU CẦU (CREATE PAYOUT REQUEST)
    // ========================================================================
    @Nested
    @DisplayName("1. Test hàm createPayoutRequest")
    class CreatePayoutRequestTests {

        @Test
        @DisplayName("Nên tạo mới thành công và trả về đúng DTO phản hồi")
        void createPayoutRequest_Success() {
            // --- 1. GIVEN (Cấu hình Mock đầy đủ cho các Repository) ---
            String instructorId = "instr-0006-0010-2026";
            BigDecimal amount = new BigDecimal("500000");

            // Đảm bảo entity mẫu có sẵn createdAt để không bị NullPointerException khi gọi .atZone()
            sampleEntity.setCreatedAt(Instant.now());

            // Khởi tạo thực thể BankAccount mẫu
            BankAccount sampleBankAccount = new BankAccount();
            sampleBankAccount.setBankCode("VCB");
            sampleBankAccount.setAccountNumber("123456789");
            sampleBankAccount.setAccountName("NGUYEN VAN A");

            // Mock quy trình kiểm tra dữ liệu của Service
            when(balanceRepository.findByInstructorId(instructorId)).thenReturn(Optional.of(sampleBalance));
            when(bankAccountRepository.findByInstructorIdAndIsPrimaryTrue(instructorId)).thenReturn(Optional.of(sampleBankAccount));
            when(repository.save(any(PayoutRequest.class))).thenReturn(sampleEntity);

            // --- 2. WHEN (Thực thi hàm cần test) ---
            PayoutRequestResponse result = payoutService.createPayoutRequest(instructorId, amount);

            // --- 3. THEN (Xác thực kết quả) ---
            assertNotNull(result);
            assertEquals("payout-001", result.getPayoutId());

            // Xác minh toàn bộ các Repository liên quan được gọi lưu đúng quy trình:
            verify(balanceRepository, times(1)).save(sampleBalance);
            verify(repository, times(1)).save(any(PayoutRequest.class));
            verify(historyRepository, times(1)).save(any());

            // Xác minh tin nhắn PENDING được gửi đi thành công qua RabbitMQ
            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(PAYOUT_EXCHANGE),
                    eq("payout.pending"), // Khớp với routing key PENDING của bạn
                    any(PayoutEventMessage.class)
            );
        }
    }


    // ========================================================================
    // TRƯỜNG HỢP TEST 2: CHỨC NĂNG PHÊ DUYỆT (APPROVE PAYOUT REQUEST)
    // ========================================================================
    @Nested
    @DisplayName("2. Test hàm approvePayoutRequest")
    class ApprovePayoutRequestTests {

        @Test
        @DisplayName("Nên duyệt thành công: Đổi trạng thái thực thể sang SUCCESS, ghi log lịch sử và bắn RabbitMQ")
        void approvePayoutRequest_Success() {
            // Given (Chuẩn bị các Mock cần thiết)
            String adminId = "admin-001";

            // Giả lập tìm thấy Payout Request đang ở trạng thái PENDING
            when(repository.findById("payout-001")).thenReturn(Optional.of(sampleEntity));
            // Giả lập tìm thấy số dư của giảng viên liên quan
            when(balanceRepository.findByInstructorId("instr-0006-0010-2026")).thenReturn(Optional.of(sampleBalance));

            // When (Thực thi hành động duyệt của Admin)
            payoutService.approvePayoutRequest(adminId, processDto);

            // Then (Xác thực kết quả thay đổi trạng thái)
            assertEquals(PayoutStatus.SUCCESS, sampleEntity.getStatus()); // Khớp với Enum PayoutStatus.SUCCESS của bạn
            assertNotNull(sampleEntity.getProcessedAt()); // Thời gian xử lý phải được ghi nhận

            // Xác minh dữ liệu được lưu xuống Database
            verify(repository, times(1)).save(sampleEntity);
            verify(historyRepository, times(1)).save(any(BalanceHistory.class));

            // Xác minh sự kiện thành công gửi lên RabbitMQ đúng Routing Key "payout.success"
            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(PAYOUT_EXCHANGE),
                    eq("payout.success"), // Hãy đảm bảo routing key này khớp với code thực tế của bạn
                    any(PayoutEventMessage.class)
            );
        }
    }

    // ========================================================================
    // TRƯỜNG HỢP TEST 3: CHỨC NĂNG TỪ CHỐI (REJECT PAYOUT REQUEST)
    // ========================================================================
    @Nested
    @DisplayName("3. Test hàm rejectPayoutRequest")
    class RejectPayoutRequestTests {

        @Test
        @DisplayName("Nên từ chối thành công: Đổi trạng thái thực thể sang REJECTED, hoàn tiền và bắn RabbitMQ")
        void rejectPayoutRequest_Success() {
            // Given (Chuẩn bị Mock tương tự approve)
            String adminId = "admin-001";
            sampleEntity.setStatus(PayoutStatus.PENDING); // Đảm bảo trạng thái ban đầu là PENDING

            when(repository.findById("payout-001")).thenReturn(Optional.of(sampleEntity));
            when(balanceRepository.findByInstructorId("instr-0006-0010-2026")).thenReturn(Optional.of(sampleBalance));

            // Thay đổi một chút note để phân biệt với Approve nếu cần
            processDto.setBankReferenceNo("bank-reject-001");

            // When (Thực thi hành động từ chối từ Admin)
            payoutService.rejectPayoutRequest(adminId, processDto);

            // Then (Xác thực kết quả thực thể bị từ chối)
            assertEquals(PayoutStatus.REJECTED, sampleEntity.getStatus()); // Đảm bảo chuyển sang Enum REJECTED
            assertNotNull(sampleEntity.getProcessedAt());

            // Xác minh lưu trữ dữ liệu
            verify(repository, times(1)).save(sampleEntity);
            verify(historyRepository, times(1)).save(any(BalanceHistory.class));

            // Xác minh sự kiện từ chối gửi lên RabbitMQ đúng Routing Key "payout.reject"
            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(PAYOUT_EXCHANGE),
                    eq("payout.rejected"), // Hãy đảm bảo routing key này khớp với code thực tế của bạn
                    any(PayoutEventMessage.class)
            );
        }
    }

    // ========================================================================
    // TRƯỜNG HỢP TEST 4: XEM VÀ LỌC CỦA INSTRUCTOR (GET MY PAYOUT REQUESTS)
    // ========================================================================
    @Nested
    @DisplayName("4. Test hàm getMyPayoutRequests")
    class GetMyPayoutRequestsTests {

        @Test
        @DisplayName("Bảo mật: Phải ghi đè instructorId của filter bằng ID đăng nhập và build Pageable đúng cấu hình")
        void getMyPayoutRequests_ShouldOverrideInstructorIdAndBuildCorrectPageable() {
            // Given (Chuẩn bị dữ liệu đầu vào và cấu hình giả lập Repo)
            String loginInstructorId = "instr-0006-0010-2026";

            PayoutFilterRequest filterRequest = new PayoutFilterRequest();
            filterRequest.setInstructorId("hacker-id-999"); // Kẻ gian cố tình truyền ID khác vào URL nhằm xem lén
            filterRequest.setPage(0);
            filterRequest.setSize(20);
            filterRequest.setSortBy("createdDate");
            filterRequest.setSortDir("desc");

            // Tạo đối tượng Pageable mong muốn dựa trên cấu hình DTO
            Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));
            // Giả lập kết quả trả về từ DB chứa 1 bản ghi mẫu
            Page<PayoutRequest> mockPage = new PageImpl<>(java.util.List.of(sampleEntity), expectedPageable, 1);

            // Mock hàm findAll nhận Specification động của JpaSpecificationExecutor
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

            // When (Thực thi logic tìm kiếm của Instructor)
            Page<PayoutRequestResponse> result = payoutService.getMyPayoutRequests(loginInstructorId, filterRequest);

            // Then (Xác thực tính an toàn hệ thống)
            assertNotNull(result);

            // ĐIỂM QUAN TRỌNG: ID trong filter bắt buộc phải bị ghi đè bằng id của người đăng nhập
            assertEquals(loginInstructorId, filterRequest.getInstructorId());

            // Sử dụng ArgumentCaptor để "bắt" đối tượng Pageable thực tế được truyền xuống repo xem có đúng cấu hình sort không
            org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable capturedPageable = pageableCaptor.getValue();
            assertEquals(20, capturedPageable.getPageSize());
            assertEquals(0, capturedPageable.getPageNumber());
            assertTrue(capturedPageable.getSort().getOrderFor("createdDate").isDescending());
        }
    }

    // ========================================================================
    // TRƯỜNG HỢP TEST 5: XEM VÀ LỌC TOÀN QUYỀN CỦA ADMIN (GET FOR ADMIN)
    // ========================================================================
    @Nested
    @DisplayName("5. Test hàm getPayoutRequestsForAdmin")
    class GetPayoutRequestsForAdminTests {

        @Test
        @DisplayName("Toàn quyền: Giữ nguyên tất cả các filter lọc do Admin chủ động truyền lên")
        void getPayoutRequestsForAdmin_ShouldKeepFilters() {
            // Given (Admin chủ động lọc chi tiết một giảng viên cụ thể với trạng thái SUCCESS)
            PayoutFilterRequest filterRequest = new PayoutFilterRequest();
            filterRequest.setInstructorId("instr-specific-789");
            filterRequest.setStatus("SUCCESS");
            filterRequest.setPage(1); // Trang số 2
            filterRequest.setSize(10);
            filterRequest.setSortBy("amount");
            filterRequest.setSortDir("asc");

            Pageable expectedPageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "amount"));
            Page<PayoutRequest> mockPage = new PageImpl<>(java.util.Collections.emptyList(), expectedPageable, 0);

            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

            // When (Thực thi logic tìm kiếm toàn quyền của Admin)
            Page<PayoutRequestResponse> result = payoutService.getPayoutRequestsForAdmin(filterRequest);

            // Then (Xác thực dữ liệu lọc)
            assertNotNull(result);
            // Tuyệt đối không được ghi đè, Admin phải được quyền giữ nguyên filter lọc target giảng viên này
            assertEquals("instr-specific-789", filterRequest.getInstructorId());
            assertEquals("SUCCESS", filterRequest.getStatus());

            // Kiểm tra phân trang được truyền xuống repo có đúng tham số Admin yêu cầu không
            org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable capturedPageable = pageableCaptor.getValue();
            assertEquals(1, capturedPageable.getPageNumber());
            assertEquals(10, capturedPageable.getPageSize());
            assertTrue(capturedPageable.getSort().getOrderFor("amount").isAscending());
        }
    }


}

