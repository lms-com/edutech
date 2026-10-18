package com.lms.order.service;

import com.lms.common.exception.AppException;
import com.lms.order.client.feign.course.CourseServiceFeignClient;
import com.lms.order.client.feign.enrollment.EnrollmentFeignClient;
import com.lms.order.client.feign.finance.FinanceServiceFeignClient;
import com.lms.order.config.RabbitMQConfig;
import com.lms.order.dto.message.OrderCompletedMessage;
import com.lms.order.exception.OrderErrorCode;
import com.lms.order.mapper.OrderMapper;
import com.lms.order.model.Order;
import com.lms.order.model.OrderDetail;
import com.lms.order.model.OrderStatus;
import com.lms.order.repository.OrderRepository;
import com.lms.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CourseServiceFeignClient courseClient;

    @Mock
    private PromotionService promotionService;

    @Mock
    private FinanceServiceFeignClient financeClient;

    @Mock
    private EnrollmentFeignClient enrollmentClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("1. markAsPaid transitions order from PENDING to PAID, increments promotions, and publishes OrderCompletedMessage")
    void markAsPaid_whenOrderPending_transitionsToPaidAndPublishesMessage() {
        String orderId = "order-test-001";
        String learnerId = "learner-test-888";

        OrderDetail item1 = OrderDetail.builder()
                .id("detail-1")
                .courseId("course-101")
                .instructorId("inst-201")
                .finalPrice(BigDecimal.valueOf(150000))
                .commissionRate(BigDecimal.valueOf(0.15))
                .promotionId("PROMO-DISCOUNT-10")
                .build();

        OrderDetail item2 = OrderDetail.builder()
                .id("detail-2")
                .courseId("course-102")
                .instructorId("inst-202")
                .finalPrice(BigDecimal.valueOf(250000))
                .commissionRate(null)
                .promotionId("PROMO-DISCOUNT-20")
                .build();

        Order order = Order.builder()
                .id(orderId)
                .learnerId(learnerId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(400000))
                .currencyCode("VND")
                .orderDetails(List.of(item1, item2))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderService.markAsPaid(orderId);

        // Assert status transition
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);

        // Assert promotion increment
        verify(promotionService, times(1)).increaseUsageCountBatch(List.of("PROMO-DISCOUNT-10", "PROMO-DISCOUNT-20"));

        // Assert message broadcast
        ArgumentCaptor<OrderCompletedMessage> messageCaptor = ArgumentCaptor.forClass(OrderCompletedMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_COMPLETED_ROUTING_KEY),
                messageCaptor.capture()
        );

        OrderCompletedMessage captured = messageCaptor.getValue();
        assertNotNull(captured);
        assertEquals(orderId, captured.getOrderId());
        assertEquals(learnerId, captured.getLearnerId());
        assertEquals(2, captured.getItems().size());

        OrderCompletedMessage.OrderItemDto capturedItem1 = captured.getItems().get(0);
        assertEquals("course-101", capturedItem1.getCourseId());
        assertEquals("inst-201", capturedItem1.getInstructorId());
        assertEquals(BigDecimal.valueOf(150000), capturedItem1.getFinalPrice());
        assertEquals(BigDecimal.valueOf(0.15), capturedItem1.getCommissionRate());
        assertEquals("VND", capturedItem1.getPaymentCurrency());

        OrderCompletedMessage.OrderItemDto capturedItem2 = captured.getItems().get(1);
        assertEquals("course-102", capturedItem2.getCourseId());
        assertEquals("inst-202", capturedItem2.getInstructorId());
        assertEquals(BigDecimal.valueOf(250000), capturedItem2.getFinalPrice());
        assertEquals(BigDecimal.ZERO, capturedItem2.getCommissionRate()); // null commission rate defaults to BigDecimal.ZERO
        assertEquals("VND", capturedItem2.getPaymentCurrency());
    }

    @Test
    @DisplayName("2. markAsPaid when order is already PAID returns immediately (idempotent, does NOT re-increment promotions nor publish)")
    void markAsPaid_whenOrderAlreadyPaid_returnsImmediately() {
        String orderId = "order-already-paid-002";
        String learnerId = "learner-test-999";

        OrderDetail item = OrderDetail.builder()
                .id("detail-3")
                .courseId("course-103")
                .instructorId("inst-203")
                .finalPrice(BigDecimal.valueOf(300000))
                .promotionId("PROMO-USED-ONCE")
                .build();

        Order order = Order.builder()
                .id(orderId)
                .learnerId(learnerId)
                .status(OrderStatus.PAID) // Already PAID
                .orderDetails(List.of(item))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        assertDoesNotThrow(() -> orderService.markAsPaid(orderId));

        // Assert: Early return, no save, no promotion increment, no message publication
        verify(orderRepository, never()).save(any());
        verify(promotionService, never()).increaseUsageCountBatch(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(Object.class));
    }

    @Test
    @DisplayName("3. markAsPaid when order does not exist throws AppException ORDER_NOT_FOUND")
    void markAsPaid_whenOrderNotFound_throwsAppException() {
        String nonExistentOrderId = "non-existent-order-id";
        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> orderService.markAsPaid(nonExistentOrderId));

        assertEquals(OrderErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getErrorMessage().contains(nonExistentOrderId));

        // Verify side effects never triggered
        verify(orderRepository, never()).save(any());
        verify(promotionService, never()).increaseUsageCountBatch(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(Object.class));
    }

    @Test
    @DisplayName("4. markAsPaid with duplicate consecutive calls is strictly idempotent (promotions incremented exactly once)")
    void markAsPaid_duplicateCalls_isStrictlyIdempotent() {
        String orderId = "order-dup-call-004";
        String learnerId = "learner-dup-004";

        OrderDetail item = OrderDetail.builder()
                .id("detail-4")
                .courseId("course-104")
                .instructorId("inst-204")
                .finalPrice(BigDecimal.valueOf(200000))
                .promotionId("PROMO-DUP-CHECK")
                .build();

        // First call order object is PENDING
        Order order = Order.builder()
                .id(orderId)
                .learnerId(learnerId)
                .status(OrderStatus.PENDING)
                .orderDetails(List.of(item))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // 1st invocation: Transitions from PENDING -> PAID
        orderService.markAsPaid(orderId);

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);
        verify(promotionService, times(1)).increaseUsageCountBatch(List.of("PROMO-DUP-CHECK"));
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.ORDER_EXCHANGE), eq(RabbitMQConfig.ORDER_COMPLETED_ROUTING_KEY), any(OrderCompletedMessage.class));

        // 2nd invocation (duplicate callback arrives, order is already PAID in memory)
        orderService.markAsPaid(orderId);

        // Verify counts did NOT increase
        verify(orderRepository, times(1)).save(order); // still 1 time
        verify(promotionService, times(1)).increaseUsageCountBatch(any()); // still 1 time
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), any(Object.class)); // still 1 time
    }

    @Test
    @DisplayName("5. markAsPaid when order has no promotions does not call promotionService")
    void markAsPaid_whenOrderHasNoPromotions_doesNotCallPromotionService() {
        String orderId = "order-no-promos-005";
        String learnerId = "learner-test-005";

        OrderDetail itemWithoutPromo = OrderDetail.builder()
                .id("detail-5")
                .courseId("course-105")
                .instructorId("inst-205")
                .finalPrice(BigDecimal.valueOf(180000))
                .promotionId(null) // No promo
                .build();

        Order order = Order.builder()
                .id(orderId)
                .learnerId(learnerId)
                .status(OrderStatus.PENDING)
                .orderDetails(List.of(itemWithoutPromo))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderService.markAsPaid(orderId);

        // Assert
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, times(1)).save(order);
        verify(promotionService, never()).increaseUsageCountBatch(any());
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.ORDER_EXCHANGE), eq(RabbitMQConfig.ORDER_COMPLETED_ROUTING_KEY), any(OrderCompletedMessage.class));
    }
}
