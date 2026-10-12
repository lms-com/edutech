package com.lms.analytics.repository;

import com.lms.analytics.dto.response.CoursePerformanceDto;
import com.lms.analytics.dto.response.DailyChartDto;
import com.lms.analytics.dto.response.InstructorPerformanceDto;
import com.lms.analytics.model.DailyCourseStatistic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCourseStatisticRepository extends JpaRepository<DailyCourseStatistic, Long> {

    // Phục vụ Chặng 3: Tìm record để cộng dồn tiền khi có Event mua hàng
    Optional<DailyCourseStatistic> findByCourseIdAndRecordDate(String courseId, LocalDate recordDate);

    // Phục vụ Chặng 4: Lấy danh sách thống kê của 1 giảng viên trong khoảng thời gian để vẽ biểu đồ
    List<DailyCourseStatistic> findByInstructorIdAndRecordDateBetweenOrderByRecordDateAsc(
            String instructorId, LocalDate startDate, LocalDate endDate);

    // Phục vụ Chặng 4: Admin lấy dữ liệu toàn sàn trong khoảng thời gian để vẽ biểu đồ
    List<DailyCourseStatistic> findByRecordDateBetweenOrderByRecordDateAsc(
            LocalDate startDate, LocalDate endDate);

    // Tính tổng doanh thu của Giảng viên
    @Query("SELECT COALESCE(SUM(d.instructorRevenue), 0) FROM DailyCourseStatistic d WHERE d.instructorId = :instructorId")
    BigDecimal sumTotalRevenueByInstructor(@Param("instructorId") String instructorId);

    // 2. Phục vụ Overview: Tính doanh thu trong tháng (hoặc khoảng thời gian)
    @Query("SELECT COALESCE(SUM(d.instructorRevenue), 0) FROM DailyCourseStatistic d " +
            "WHERE d.instructorId = :instructorId AND d.recordDate >= :startDate AND d.recordDate <= :endDate")
    BigDecimal sumInstructorRevenueByDateRange(@Param("instructorId") String instructorId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // Tính tổng học viên của Giảng viên
    @Query("SELECT COALESCE(SUM(d.dailyEnrollments), 0) FROM DailyCourseStatistic d WHERE d.instructorId = :instructorId")
    Integer sumTotalEnrollmentsByInstructor(@Param("instructorId") String instructorId);

    // Lấy dữ liệu vẽ biểu đồ: Gom nhóm theo ngày (vì 1 ngày có thể có nhiều khóa học được bán)
    @Query("SELECT new com.lms.analytics.dto.response.DailyChartDto(d.recordDate, SUM(d.instructorRevenue), SUM(d.dailyEnrollments)) " +
            "FROM DailyCourseStatistic d " +
            "WHERE d.instructorId = :instructorId AND d.recordDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.recordDate ORDER BY d.recordDate ASC")
    List<DailyChartDto> getChartDataByInstructor(@Param("instructorId") String instructorId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    // 5. Phục vụ Bảng xếp hạng: Lấy Top khóa học có doanh thu cao nhất
    @Query("SELECT new com.lms.analytics.dto.response.CoursePerformanceDto(d.courseId, SUM(d.instructorRevenue), SUM(d.dailyEnrollments)) " +
            "FROM DailyCourseStatistic d " +
            "WHERE d.instructorId = :instructorId " +
            "GROUP BY d.courseId ORDER BY SUM(d.instructorRevenue) DESC")
    List<CoursePerformanceDto> findTopCoursesByInstructor(@Param("instructorId") String instructorId, Pageable pageable);


    // ADMIN:
    // 1. Tổng GMV toàn sàn (Tổng tiền học viên đã trả)
    @Query("SELECT COALESCE(SUM(d.totalOrderAmount), 0) FROM DailyCourseStatistic d WHERE d.recordDate >= :startDate AND d.recordDate <= :endDate")
    BigDecimal sumPlatformGmvByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 2. Tổng doanh thu thực của Sàn (Net Revenue = Tổng phí nền tảng)
    @Query("SELECT COALESCE(SUM(d.platformFee), 0) FROM DailyCourseStatistic d WHERE d.recordDate >= :startDate AND d.recordDate <= :endDate")
    BigDecimal sumPlatformNetRevenueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 3. Top Khóa học bán chạy nhất TOÀN SÀN
    @Query("SELECT new com.lms.analytics.dto.response.CoursePerformanceDto(d.courseId, SUM(d.totalOrderAmount), SUM(d.dailyEnrollments)) " +
            "FROM DailyCourseStatistic d " +
            "GROUP BY d.courseId ORDER BY SUM(d.totalOrderAmount) DESC")
    List<CoursePerformanceDto> findTopCoursesSystemWide(Pageable pageable);

    // 4. Top Giảng viên mang lại doanh thu cao nhất
    @Query("SELECT new com.lms.analytics.dto.response.InstructorPerformanceDto(d.instructorId, SUM(d.totalOrderAmount), SUM(d.dailyEnrollments)) " +
            "FROM DailyCourseStatistic d " +
            "GROUP BY d.instructorId ORDER BY SUM(d.totalOrderAmount) DESC")
    List<InstructorPerformanceDto> findTopInstructorsSystemWide(Pageable pageable);


    // 5. Lay Daily Statistic data cho admin

    
}

