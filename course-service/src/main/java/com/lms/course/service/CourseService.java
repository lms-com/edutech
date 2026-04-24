package com.lms.course.service;

import com.lms.course.dto.request.CourseRequest;
import com.lms.course.dto.response.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CourseService {
    CourseResponse createCourse(CourseRequest request, String instructorId);
    //lấy khóa học theo ID
    CourseResponse getCourseById(String courseId);

    Page<CourseResponse> getAllCourses(Pageable pageable);
    //lấy tất cả khóa học của 1 giảng viên
    Page<CourseResponse> getAllCoursesByInstructorId(String instructorId, Pageable pageable);



    CourseResponse updateCourse(String courseId, CourseRequest request, String instructorId);

    void deleteCourse(String courseId, String instructorId);

    void ChangeCourseStatus(String courseId, String status , String instructorId);

}