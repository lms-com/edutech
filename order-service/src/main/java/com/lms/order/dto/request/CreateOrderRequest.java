package com.lms.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    @NotEmpty(message = "List of courses cannot be empty")
    @Valid
    List<CartItemRequest> items;

    @NotBlank(message = "Currency code cannot be blank")
    String currencyCode;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CartItemRequest {
        @NotBlank(message = "Course id cannot be blank")
        String courseId;

        String promotionCode;
    }
}
