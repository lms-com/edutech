package com.lms.course.service;

import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.request.CourseUpdateRequest;
import com.lms.course.dto.response.CourseDetailResponse;
import com.lms.course.dto.response.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CourseService {
    CourseResponse createCourse(CourseRequest request, String instructorId);
    //lấy khóa học theo ID
    CourseDetailResponse getCourseById(String courseId);

    Page<CourseResponse> getAllCourses(Pageable pageable);
    //lấy tất cả khóa học của 1 giảng viên
    Page<CourseResponse> getAllCoursesByInstructorId(String instructorId, Pageable pageable);

    //lấy tất cả khóa học dành cho admin
    Page<CourseResponse> getAllCoursesForAdmin(Pageable pageable, String status, String instructorId);

    //lấy danh sách khóa học liên quan
    List<CourseResponse> getRelatedCourses(String courseId);

    // Cập nhật từng phần (PATCH)
    CourseResponse updateCourse(String courseId, CourseUpdateRequest request, String instructorId);

    // Cập nhật đầy đủ (PUT)
    CourseResponse updateCourseFull(String courseId, CourseRequest request, String instructorId);

    void deleteCourse(String courseId, String instructorId);

    // API 14: Nhân bản
    CourseResponse cloneCourse(String courseId, String instructorId);

    // API 15: Yêu cầu duyệt / Đổi trạng thái
    void changeCourseStatus(String courseId, String status, String instructorId);

    // API 16: Admin duyệt
    void approveCourse(String courseId);

    // API 17: Admin từ chối
    void rejectCourse(String courseId, String rejectionNote);

    // API 18: Admin cấu hình hoa hồng
    void setCourseCommission(String courseId, BigDecimal overrideCommissionRate);
}