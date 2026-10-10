package com.lms.order.controller;

import com.lms.common.swagger.annotation.RequireInternalKey;
import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/api/internal/v1/orders")
@RequireInternalKey
@RequiredArgsConstructor
public class InternalOrderController {
    private final OrderService orderService;


}
