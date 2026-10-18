package com.lms.order.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.common.swagger.annotation.RequireJwt;
import com.lms.order.dto.request.CreatePromotionRequest;
import com.lms.order.dto.response.PromotionResponse;
import com.lms.order.dto.response.PromotionValidateResponse;
import com.lms.order.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions")
@RequireJwt
@Tag(name = "Promotion Controller", description = "Quản lý mã giảm giá & voucher")
public class PromotionController {

    private final PromotionService promotionService;

    // ----------------------------------------------------------------
    // Học viên: Kiểm tra mã giảm giá trước khi đặt hàng
    // ----------------------------------------------------------------

    @Operation(
        summary = "Kiểm tra mã giảm giá",
        description = "Học viên nhập mã + courseId + giá khóa học để xem có hợp lệ không và số tiền tiết kiệm được"
    )
    @GetMapping("/validate")
    public ApiResponse<PromotionValidateResponse> validateCode(
            @RequestParam String code,
            @RequestParam String courseId,
            @RequestParam BigDecimal coursePrice
    ) {
        return ApiResponse.success(promotionService.validateCode(code, courseId, coursePrice));
    }

    // ----------------------------------------------------------------
    // Giảng viên: Quản lý mã giảm giá của bản thân
    // ----------------------------------------------------------------

    @Operation(summary = "Giảng viên / Admin: Xem danh sách mã giảm giá của tôi")
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<Page<PromotionResponse>> getMyPromotions(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(promotionService.getMyPromotions(userId, pageable));
    }

    @Operation(summary = "Giảng viên / Admin: Tạo mã giảm giá mới")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<PromotionResponse> createPromotion(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePromotionRequest request
    ) {
        return ApiResponse.success(promotionService.createPromotion(request, userId));
    }

    @Operation(summary = "Giảng viên / Admin: Cập nhật mã giảm giá")
    @PutMapping("/{promotionId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<PromotionResponse> updatePromotion(
            @PathVariable String promotionId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePromotionRequest request
    ) {
        return ApiResponse.success(promotionService.updatePromotion(promotionId, request, userId));
    }

    @Operation(summary = "Giảng viên / Admin: Xóa mã giảm giá")
    @DeleteMapping("/{promotionId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<Void> deletePromotion(
            @PathVariable String promotionId,
            @RequestHeader("X-User-Id") String userId
    ) {
        promotionService.deletePromotion(promotionId, userId);
        return ApiResponse.success(null);
    }

    @Operation(
        summary = "Giảng viên / Admin: Gắn mã giảm giá vào khóa học",
        description = "Một mã có thể gắn vào nhiều khóa học"
    )
    @PostMapping("/{promotionId}/courses/{courseId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<PromotionResponse> attachToCourse(
            @PathVariable String promotionId,
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ApiResponse.success(promotionService.attachToAnyCourse(promotionId, courseId, userId));
    }

    @Operation(summary = "Giảng viên / Admin: Gỡ mã giảm giá khỏi khóa học")
    @DeleteMapping("/{promotionId}/courses/{courseId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<PromotionResponse> detachFromCourse(
            @PathVariable String promotionId,
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ApiResponse.success(promotionService.detachFromCourse(promotionId, courseId, userId));
    }

    // ----------------------------------------------------------------
    // Admin only: Xem toàn bộ mã trong hệ thống
    // ----------------------------------------------------------------

    @Operation(summary = "Admin: Xem tất cả mã giảm giá trong hệ thống")
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Page<PromotionResponse>> getAllPromotions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(promotionService.getAllPromotions(pageable));
    }
}
