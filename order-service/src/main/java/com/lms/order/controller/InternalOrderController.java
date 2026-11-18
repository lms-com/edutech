package com.lms.order.controller;

import com.lms.common.swagger.annotation.RequireInternalKey;
import com.lms.order.dto.response.InternalOrderCourseResponse;
import com.lms.order.dto.response.InternalOrderResponse;
import com.lms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/v1/orders")
@RequireInternalKey
@RequiredArgsConstructor
public class InternalOrderController {
    private final OrderService orderService;

    @GetMapping("/{id}")
    public InternalOrderResponse getOrderById(@PathVariable("id") String id) {
        return orderService.getInternalOrderById(id);
    }

    @GetMapping("/{id}/courses")
    public List<InternalOrderCourseResponse> getCoursesByOrderId(@PathVariable("id") String id) {
        return orderService.getInternalOrderCourses(id);
    }
}
