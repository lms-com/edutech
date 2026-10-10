package com.lms.order.dev.dev_service;

import com.lms.order.dev.dev_dto.DevOrderResponse;
import com.lms.order.model.Order;
import com.lms.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DevOrderService {
    private final OrderRepository orderRepository;

    /**
     * Get all Orders
     * @return
     */
    public List<Order> getAllOrders(){
        return orderRepository.findAll();
    }

    /**
     * Get Order and items
     * @param orderId
     * @return
     */
    public DevOrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        List<DevOrderResponse.Item> items = order.getOrderDetails().stream().map(od ->
                DevOrderResponse.Item.builder()
                        .itemId(od.getId())
                        .courseName(od.getCourseName())
                        .originalPrice(od.getOriginalPrice())
                        .discountAmount(od.getDiscountAmount())
                        .commissionRate(od.getCommissionRate())
                        .build()
                ).toList();
        return DevOrderResponse.builder()
                .id(orderId)
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .items(items)
                .build();
    }
}
