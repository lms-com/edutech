package com.lsm.order.service;

import com.lms.order.client.feign.course.CourseServiceFeignClient;
import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.client.listener.PaymentEventListener;
import com.lms.order.dto.message.PaymentProcessMessage;
import com.lms.order.dto.request.CreateOrderRequest;
import com.lms.order.dto.response.PendingOrderResponse;
import com.lms.order.model.Order;
import com.lms.order.model.OrderStatus;
import com.lms.order.repository.OrderRepository;
import com.lms.order.service.OrderService;
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
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderFlowIntegrationTest {

    @Autowired
    private OrderService orderServiceImpl; // Class chứa hàm createOrder của bạn

    @Autowired
    private PaymentEventListener paymentEventListener; // Listener hứng RabbitMQ

    @Autowired
    private OrderRepository orderRepository;

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
        mockCourse.setCurrentPrice(new BigDecimal("1000000"));
        mockCourse.setInstructorId("INSTRUCTOR-99");
        mockCourse.setCommissionRate(new BigDecimal("0.2"));

        Mockito.when(courseClient.getCoursesById(List.of(courseId)))
                .thenReturn(List.of(mockCourse));

        // Build Request tạo đơn
        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.CartItemRequest item = new CreateOrderRequest.CartItemRequest();
        item.setCourseId(courseId);
        request.setItems(List.of(item));

        // --- 2. BƯỚC 1: TẠO ĐƠN HÀNG (PENDING) ---
        PendingOrderResponse orderResponse = orderService.createOrder(request, learnerId);
        String createdOrderId = orderResponse.getOrderId();

        // Kiểm tra trong DB: Đơn vừa tạo phải là PENDING
        Order initialOrder = orderRepository.findById(createdOrderId).orElseThrow();
        Assertions.assertEquals(OrderStatus.PENDING, initialOrder.getStatus());
        System.out.println("1️⃣ Đã tạo đơn hàng PENDING thành công với ID: " + createdOrderId);

        // --- 3. BƯỚC 2: GIẢ LẬP FINANCE SERVICE BẮN RABBITMQ TỚI ---
        PaymentProcessMessage mockFinanceMessage = PaymentProcessMessage.builder()
                .orderId(createdOrderId)
                .learnerId(learnerId)
                .amount(1000000L)
                .status("SUCCESS")
                .transactionNo("VNP12345678")
                .paidAt(Instant.now())
                .build();

        // Chạy Listener (Mô phỏng RabbitMQ nhận được tin và gọi hàm handle)
        paymentSuccessListener.handlePaymentSuccess(mockFinanceMessage);

        // --- 4. BƯỚC 3: KIỂM TRA KẾT QUẢ CUỐI CÙNG (PAID) ---
        Order updatedOrder = orderRepository.findById(createdOrderId).orElseThrow();

        System.out.println("2️⃣ Trạng thái đơn hàng trong DB sau khi nhận tin: " + updatedOrder.getStatus());

        // Assert xem trạng thái đã thành PAID chưa
        Assertions.assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
    }
}
