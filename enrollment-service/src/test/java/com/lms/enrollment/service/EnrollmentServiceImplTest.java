package com.lms.enrollment.service;

import com.lms.enrollment.client.CourseServiceClient;
import com.lms.enrollment.client.IamServiceClient;
import com.lms.enrollment.dto.event.OrderCompletedEvent;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.enums.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private IamServiceClient iamServiceClient;

    @Mock
    private CourseServiceClient courseServiceClient;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Test
    @DisplayName("Ghi danh mới khi người học chưa có bản ghi enrollment")
    void enrollFromOrder_createsNewActiveEnrollment() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");
        OrderCompletedEvent event = new OrderCompletedEvent("order-1", "learner-1", List.of(item));

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-101"))
                .thenReturn(Optional.empty());

        enrollmentService.enrollFromOrder(event);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        Enrollment saved = captor.getValue();
        assertEquals("learner-1", saved.getLearnerId());
        assertEquals("course-101", saved.getCourseId());
        assertEquals(EnrollmentStatus.ACTIVE, saved.getStatus());
    }

    @Test
    @DisplayName("Kích hoạt lại (re-activate) bản ghi ghi danh khi trạng thái trước đó là REVOKED")
    void enrollFromOrder_reactivatesRevokedEnrollment() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");
        OrderCompletedEvent event = new OrderCompletedEvent("order-1", "learner-1", List.of(item));

        Enrollment existing = Enrollment.builder()
                .id("enr-1")
                .learnerId("learner-1")
                .courseId("course-101")
                .status(EnrollmentStatus.REVOKED)
                .build();

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-101"))
                .thenReturn(Optional.of(existing));

        enrollmentService.enrollFromOrder(event);

        verify(enrollmentRepository).save(existing);
        assertEquals(EnrollmentStatus.ACTIVE, existing.getStatus());
    }

    @Test
    @DisplayName("Bỏ qua khi người học đã có bản ghi ACTIVE")
    void enrollFromOrder_skipsWhenAlreadyActive() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");
        OrderCompletedEvent event = new OrderCompletedEvent("order-1", "learner-1", List.of(item));

        Enrollment existing = Enrollment.builder()
                .id("enr-1")
                .learnerId("learner-1")
                .courseId("course-101")
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-101"))
                .thenReturn(Optional.of(existing));

        enrollmentService.enrollFromOrder(event);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Bỏ qua khi learnerId là null hoặc rỗng")
    void enrollFromOrder_skipsWhenLearnerIdIsNullOrBlank() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");

        // Case 1: learnerId is null
        OrderCompletedEvent nullLearnerEvent = new OrderCompletedEvent("order-1", null, List.of(item));
        enrollmentService.enrollFromOrder(nullLearnerEvent);

        // Case 2: learnerId is empty string
        OrderCompletedEvent emptyLearnerEvent = new OrderCompletedEvent("order-2", "", List.of(item));
        enrollmentService.enrollFromOrder(emptyLearnerEvent);

        // Case 3: learnerId is blank whitespace
        OrderCompletedEvent blankLearnerEvent = new OrderCompletedEvent("order-3", "   ", List.of(item));
        enrollmentService.enrollFromOrder(blankLearnerEvent);

        verifyNoInteractions(enrollmentRepository);
    }
}
