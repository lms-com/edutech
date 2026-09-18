package com.lms.order.service;

import com.lms.order.client.feign.course.CourseServiceFeignClient;
import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.client.listener.PaymentEventListener;
import com.lms.order.dto.message.PaymentProcessMessage;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.PendingOrderResponse;
import com.lms.order.model.CoursePromotion;
import com.lms.order.model.Order;
import com.lms.order.model.OrderStatus;
import com.lms.order.model.Promotion;
import com.lms.order.repository.CoursePromotionRepository;
import com.lms.order.repository.OrderRepository;
import com.lms.order.repository.PromotionRepository;
import com.lms.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderFlowIntegrationTest {

    @Autowired
    private OrderServiceImpl orderService; // Class chứa hàm createOrder của bạn

    @Autowired
    private PaymentEventListener paymentEventListener; // Listener hứng RabbitMQ

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CoursePromotionRepository coursePromotionRepository;

    @MockBean
    private CourseServiceFeignClient courseClient; // Mock FeignClient gọi sang Course Service

    @Test
    void testFullOrderFlow_FromPendingToPaid_Success() {
        // --- 1. PREPARE DATA & MOCK ---
        String learnerId = "LEARNER-001";
        String courseId = "COURSE-101";

        // Mock dữ liệu trả về từ Course Service
        CourseInternalRequest mockCourse = new CourseInternalRequest();
        mockCourse.setCourseId(courseId);
        mockCourse.setCourseName("Java Microservices");
        mockCourse.setCurrentPrice(new BigDecimal("1000000")); // Giá gốc: 1M
        mockCourse.setInstructorId("INSTRUCTOR-99");
        mockCourse.setCommissionRate(new BigDecimal("0.2"));

        Mockito.when(courseClient.getCoursesById(List.of(courseId)))
                .thenReturn(List.of(mockCourse));

        // Tạo đúng 1 promotion cần sử dụng (ĐÃ SỬA: Bỏ set version thủ công)
        Promotion promotion = Promotion.builder()
                .isActive(true)
                .startDate(LocalDateTime.now().minusMonths(3))
                .endDate(LocalDateTime.now().plusMonths(3))
                .code("P001")
                .usageCount(0)
                .usageLimit(10)
                .discountAmount(new BigDecimal("500000")) // Giảm 500k -> Còn phải trả 500k
                .build();

        // ĐÃ SỬA: Nhận lại object sau khi lưu để lấy ID UUID tự sinh từ DB
        promotion = promotionRepository.saveAndFlush(promotion);

        // Gắn promotion vào course (ĐÃ SỬA: Dùng saveAndFlush để đồng bộ khóa ngoại ngay)
        coursePromotionRepository.saveAndFlush(CoursePromotion.builder()
                .id("C-P-101-p_001")
                .courseId(courseId)
                .promotion(promotion)
                .build());

        // Build Request tạo đơn
        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.CartItemRequest item = new CreateOrderRequest.CartItemRequest();
        item.setCourseId(courseId);
        item.setPromotionCode(promotion.getCode());
        request.setItems(List.of(item));

        // --- 2. BƯỚC 1: TẠO ĐƠN HÀNG (PENDING) ---
        PendingOrderResponse orderResponse = orderService.createOrder(request, learnerId);
        String createdOrderId = orderResponse.getOrderId();

        // Kiểm tra trong DB: Đơn vừa tạo phải là PENDING
        Order initialOrder = orderRepository.findById(createdOrderId).orElseThrow();
        Assertions.assertEquals(OrderStatus.PENDING, initialOrder.getStatus());

        // GIỮ NGUYÊN: Dòng print kiểm tra trạng thái đơn hàng ban đầu của bạn
        System.out.println("1️⃣ Đã tạo đơn hàng PENDING thành công với ID: " + createdOrderId);


        // --- 3. BƯỚC 2: GIẢ LẬP FINANCE SERVICE BẮN RABBITMQ TỚI ---
        PaymentProcessMessage mockFinanceMessage = PaymentProcessMessage.builder()
                .orderId(createdOrderId)
                .learnerId(learnerId)
                .amount(500000L) // ĐÃ SỬA: 1M - 500k giảm giá = 500k thực trả để khớp logic nghiệp vụ
                .status("SUCCESS")
                .transactionNo("VNP12345678")
                .paidAt(Instant.now())
                .build();

        String promotionCode = request.getItems().getFirst().getPromotionCode();

        // GIỮ NGUYÊN: Dòng print kiểm tra lượt dùng TRƯỚC KHI THANH TOÁN của bạn
        System.out.println("🔢 Promotion " + promotionCode
                + " have had usage count = \033[7m"
                + promotionRepository.findByCode(promotionCode).orElseThrow().getUsageCount() + "\033[0m  Before Order Paid!");

        // Chạy Listener (Mô phỏng RabbitMQ nhận được tin và gọi hàm handle)
        paymentEventListener.onPaymentSuccess(mockFinanceMessage);

        // GIỮ NGUYÊN: Dòng print kiểm tra lượt dùng SAU KHI THANH TOÁN của bạn
        System.out.println("ℹ🔢🆗 Promotion " + promotionCode
                + " has usage count = \033[7m"
                + promotionRepository.findByCode(promotionCode).orElseThrow().getUsageCount() + "\033[0m  After Order Paid!");


        // --- 4. BƯỚC 3: KIỂM TRA KẾT QUẢ CUỐI CÙNG (PAID) ---
        Order updatedOrder = orderRepository.findById(createdOrderId).orElseThrow();

        // GIỮ NGUYÊN: Dòng print kiểm tra trạng thái đơn hàng cuối cùng của bạn
        System.out.println("2️⃣ Trạng thái đơn hàng trong DB sau khi nhận tin: " + updatedOrder.getStatus());

        // Assert xem trạng thái đã thành PAID chưa
        Assertions.assertEquals(OrderStatus.PAID, updatedOrder.getStatus());

        // BỔ SUNG BỞI SENIOR: Đảm bảo bài test tự động hóa việc check count mà không cần nhìn log bằng mắt
        Promotion updatedPromotion = promotionRepository.findByCode(promotion.getCode()).orElseThrow();
        Assertions.assertEquals(1, updatedPromotion.getUsageCount(), "Lượt sử dụng Promotion phải tăng lên 1!");
    }


}
