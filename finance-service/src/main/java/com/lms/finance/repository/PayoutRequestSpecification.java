package com.lms.finance.repository;

import com.lms.finance.dto.request.PayoutFilterRequest;
import com.lms.finance.entity.PayoutRequest;
import com.lms.finance.enums.PayoutStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public final class PayoutRequestSpecification {

    private PayoutRequestSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    private static final ZoneId zoneId = ZoneId.systemDefault();

    public static Specification<PayoutRequest> hasInstructorId (String instructorId) {
        return (root, query, cb) -> instructorId == null ?
                cb.conjunction() : cb.equal(root.get("instructorId"), instructorId);
    }

    public static Specification<PayoutRequest> hasAdminId (String adminId) {
        return (root, query, cb) -> adminId == null ?
                cb.conjunction() : cb.equal(root.get("processedBy"), adminId);
    }

    public static Specification<PayoutRequest> amountGreaterThanOrEqualTo (BigDecimal minAmount) {
        return (root, query, cb) -> minAmount == null ?
                cb.conjunction() : cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    public static Specification<PayoutRequest> amountLessThanOrEqualTo (BigDecimal maxAmount) {
        return (root, query, cb) -> maxAmount == null ?
                cb.conjunction() : cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    public static Specification<PayoutRequest> hasStatus (String status) {
        try {
            PayoutStatus enumStatus = PayoutStatus.valueOf(status.toUpperCase());
            return (root, query, cb) -> cb.equal(root.get("status"), enumStatus);
        } catch (Exception e) {
            return (root, query, cb) -> cb.conjunction();
        }
    }

    public static Specification<PayoutRequest> createdAtGreaterThanOrEqual (LocalDate createdFromDate) {
        if  (createdFromDate == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        Instant startOfDate = createdFromDate.atStartOfDay(zoneId).toInstant();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDate);
    }

    public static Specification<PayoutRequest> createdAtLessThanOrEqual (LocalDate createdToDate) {
        if (createdToDate == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        Instant endOfDate = createdToDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant();
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endOfDate);
    }

    public static Specification<PayoutRequest> processedAtGreaterThanOrEqual (LocalDate processedFromDate) {
        if (processedFromDate == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        Instant startOfDate = processedFromDate.atStartOfDay(zoneId).toInstant();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("processedAt"), startOfDate);
    }

    public static Specification<PayoutRequest> processedAtLessThanOrEqual (LocalDate processedToDate) {
        if (processedToDate == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        Instant endOfDate = processedToDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant();
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("processedAt"), endOfDate);
    }

    public static Specification<PayoutRequest> buildSpecification (PayoutFilterRequest filter) {
        return Specification.where(hasInstructorId(filter.getInstructorId()))
                .and(hasAdminId(filter.getAdminId()))
                .and(amountGreaterThanOrEqualTo(filter.getMinAmount()))
                .and(amountLessThanOrEqualTo(filter.getMaxAmount()))
                .and(hasStatus(filter.getStatus()))
                .and(createdAtGreaterThanOrEqual(filter.getCreatedFromDate()))
                .and(createdAtLessThanOrEqual(filter.getCreatedToDate()))
                .and(processedAtGreaterThanOrEqual(filter.getProcessedFromDate()))
                .and(processedAtLessThanOrEqual(filter.getProcessedToDate()));
    }
}
