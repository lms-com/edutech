package com.lms.order.mapper;

import com.lms.order.client.feign.course.dto.CourseInternalRequest;
import com.lms.order.dto.response.OrderDetailResponse;
import com.lms.order.dto.response.OrderResponse;
import com.lms.order.model.Order;
import com.lms.order.model.OrderDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail);

    @Mapping(source = "finalPrice", target = "currentPrice")
    // VND is default throughout the entire system  ==>  COMMENT THIS LINE
    /*@Mapping(source = "originalCurrency", target = "currencyCode")*/
    CourseInternalRequest toCourseInternalDto (OrderDetail orderDetail);
}
