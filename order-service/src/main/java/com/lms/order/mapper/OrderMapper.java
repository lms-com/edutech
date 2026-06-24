package com.lms.order.mapper;

import com.lms.order.dto.response.OrderDetailResponse;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.model.Order;
import com.lms.order.model.OrderDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail);
}
