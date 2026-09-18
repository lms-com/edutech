package com.lms.order.dev.controller;

import com.lms.common.dto.response.ApiResponse;
import com.lms.order.dev.service.DevPromotionService;
import com.lms.order.dto.request.CoursePromotionRequest;
import com.lms.order.model.CoursePromotion;
import com.lms.order.model.Promotion;
import com.lms.order.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Promotion APIs for Dev")
@RestController
@RequestMapping("/dev/promotions")
@RequiredArgsConstructor
public class DevPromotionController {
    private final DevPromotionService devPromotionService;
    private final PromotionService promotionService;

    @Operation(summary = "Get all promotions")
    @GetMapping
    public ApiResponse<List<Promotion>> getAllPromotions(){
        ApiResponse<List<Promotion>> apiResponse = new ApiResponse<>();
        apiResponse.setData(devPromotionService.getAllPromotions());
        apiResponse.setCode(200);
        apiResponse.setMessage("success");
        return apiResponse;
    }

    @Operation(summary = "Get and Check Promotion by Code")
    @GetMapping("/{code}")
    public ApiResponse<Promotion> getPromotionByCode(@PathVariable String code){
        return ApiResponse.success(promotionService.getPromotionByCode(code));
    }

    @Operation(summary = "Set Promotion for Course")
    @PostMapping("/set/courses")
    public ApiResponse<CoursePromotion> setPromotionForCourse(
            @RequestBody CoursePromotionRequest req
            ) {
        return ApiResponse.success(
                devPromotionService.setPromotionForCourse(req));
    }

    @Operation(summary = "Get all Course-Promotion")
    @GetMapping("/courses")
    public ApiResponse<List<CoursePromotion>> getAllCoursePromotion(){
        return ApiResponse.success(devPromotionService.getAllCoursePromotions());
    }
}
