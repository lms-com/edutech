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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplStressTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private IamServiceClient iamServiceClient;

    @Mock
    private CourseServiceClient courseServiceClient;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Test
    @DisplayName("1. Re-activate REVOKED enrollment to ACTIVE")
    void enrollFromOrder_reactivatesRevokedEnrollment() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");
        OrderCompletedEvent event = new OrderCompletedEvent("ord-1", "learner-1", List.of(item));

        Enrollment existingRevoked = Enrollment.builder()
                .id("enr-revoked")
                .learnerId("learner-1")
                .courseId("course-101")
                .status(EnrollmentStatus.REVOKED)
                .completedRate(45)
                .build();

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-101"))
                .thenReturn(Optional.of(existingRevoked));

        enrollmentService.enrollFromOrder(event);

        verify(enrollmentRepository).save(existingRevoked);
        assertEquals(EnrollmentStatus.ACTIVE, existingRevoked.getStatus());
        assertEquals(45, existingRevoked.getCompletedRate(), "Existing progress should be retained");
    }

    @Test
    @DisplayName("2. Existing ACTIVE enrollment is skipped without repository save")
    void enrollFromOrder_skipsAlreadyActiveEnrollment() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-101");
        OrderCompletedEvent event = new OrderCompletedEvent("ord-1", "learner-1", List.of(item));

        Enrollment existingActive = Enrollment.builder()
                .id("enr-active")
                .learnerId("learner-1")
                .courseId("course-101")
                .status(EnrollmentStatus.ACTIVE)
                .completedRate(80)
                .build();

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-101"))
                .thenReturn(Optional.of(existingActive));

        enrollmentService.enrollFromOrder(event);

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("3. Multi-item order: mix of new, revoked, and already active enrollments")
    void enrollFromOrder_mixedEnrollmentsInSingleOrder() {
        OrderCompletedEvent.OrderItemDto item1 = new OrderCompletedEvent.OrderItemDto();
        item1.setCourseId("course-new");
        OrderCompletedEvent.OrderItemDto item2 = new OrderCompletedEvent.OrderItemDto();
        item2.setCourseId("course-revoked");
        OrderCompletedEvent.OrderItemDto item3 = new OrderCompletedEvent.OrderItemDto();
        item3.setCourseId("course-active");

        OrderCompletedEvent event = new OrderCompletedEvent("ord-multi", "learner-X", List.of(item1, item2, item3));

        // course-new: not found
        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-X", "course-new"))
                .thenReturn(Optional.empty());

        // course-revoked: exists as REVOKED
        Enrollment revokedEnr = Enrollment.builder()
                .id("enr-2")
                .learnerId("learner-X")
                .courseId("course-revoked")
                .status(EnrollmentStatus.REVOKED)
                .build();
        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-X", "course-revoked"))
                .thenReturn(Optional.of(revokedEnr));

        // course-active: exists as ACTIVE
        Enrollment activeEnr = Enrollment.builder()
                .id("enr-3")
                .learnerId("learner-X")
                .courseId("course-active")
                .status(EnrollmentStatus.ACTIVE)
                .build();
        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-X", "course-active"))
                .thenReturn(Optional.of(activeEnr));

        enrollmentService.enrollFromOrder(event);

        // Verification:
        // 1. course-new: new Enrollment saved
        ArgumentCaptor<Enrollment> newCaptor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository, times(2)).save(newCaptor.capture());

        List<Enrollment> savedEnrollments = newCaptor.getAllValues();
        assertEquals(2, savedEnrollments.size());

        // One is the reactivated revokedEnr
        assertTrue(savedEnrollments.contains(revokedEnr));
        assertEquals(EnrollmentStatus.ACTIVE, revokedEnr.getStatus());

        // One is the newly created enrollment
        Enrollment newlyCreated = savedEnrollments.stream()
                .filter(e -> "course-new".equals(e.getCourseId()))
                .findFirst()
                .orElseThrow();
        assertEquals("learner-X", newlyCreated.getLearnerId());
        assertEquals(EnrollmentStatus.ACTIVE, newlyCreated.getStatus());
        assertEquals(0, newlyCreated.getCompletedRate());

        // activeEnr was never saved again
        assertFalse(savedEnrollments.contains(activeEnr));
    }

    @Test
    @DisplayName("4. Graceful handling of null event and null items")
    void enrollFromOrder_nullEventAndNullItems() {
        assertDoesNotThrow(() -> enrollmentService.enrollFromOrder(null));
        assertDoesNotThrow(() -> enrollmentService.enrollFromOrder(new OrderCompletedEvent("ord-1", "l-1", null)));
        assertDoesNotThrow(() -> enrollmentService.enrollFromOrder(new OrderCompletedEvent("ord-1", "l-1", Collections.emptyList())));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("5. Graceful handling of items with null courseId")
    void enrollFromOrder_itemsWithNullCourseId() {
        OrderCompletedEvent.OrderItemDto validItem = new OrderCompletedEvent.OrderItemDto();
        validItem.setCourseId("course-valid");

        OrderCompletedEvent.OrderItemDto nullCourseItem = new OrderCompletedEvent.OrderItemDto();
        nullCourseItem.setCourseId(null);

        OrderCompletedEvent event = new OrderCompletedEvent("ord-null-course", "learner-1", List.of(validItem, nullCourseItem));

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-1", "course-valid"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> enrollmentService.enrollFromOrder(event));

        verify(enrollmentRepository, times(1)).findByLearnerIdAndCourseId(any(), any());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("6. Concurrency double-enrollment catch: DataIntegrityViolationException is swallowed gracefully")
    void enrollFromOrder_catchesDataIntegrityViolationException() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-race");
        OrderCompletedEvent event = new OrderCompletedEvent("ord-race", "learner-race", List.of(item));

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-race", "course-race"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key UK_learner_course"));

        assertDoesNotThrow(() -> enrollmentService.enrollFromOrder(event));
    }

    @Test
    @DisplayName("7. Non-integrity runtime exceptions are propagated for RabbitMQ retry/DLQ")
    void enrollFromOrder_propagatesUnexpectedExceptions() {
        OrderCompletedEvent.OrderItemDto item = new OrderCompletedEvent.OrderItemDto();
        item.setCourseId("course-fail");
        OrderCompletedEvent event = new OrderCompletedEvent("ord-fail", "learner-fail", List.of(item));

        when(enrollmentRepository.findByLearnerIdAndCourseId("learner-fail", "course-fail"))
                .thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(RuntimeException.class, () -> enrollmentService.enrollFromOrder(event));
    }
}
