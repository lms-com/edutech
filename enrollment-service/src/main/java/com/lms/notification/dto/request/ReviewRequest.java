package com.lms.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Star must not be null")
    @Min(value = 1, message = "Star must be at least 1 star")
    @Max(value = 5, message = "Star must be at most 5 stars")
    private Integer star;

    @Size(max = 1000, message = "Review comment must not exceed 1000 characters")
    private String comment;
}
