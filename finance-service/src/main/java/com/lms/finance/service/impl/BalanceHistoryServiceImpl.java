package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.finance.dto.request.BalanceHistoryFilterRequest;
import com.lms.finance.dto.response.BalanceHistoryInstructorResponse;
import com.lms.finance.entity.BalanceHistory;
import com.lms.finance.enums.BalanceAction;
import com.lms.finance.enums.BalanceType;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.BalanceHistoryRepository;
import com.lms.finance.service.BalanceHistoryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceHistoryServiceImpl implements BalanceHistoryService {
    private final BalanceHistoryRepository historyRepository;

    @Override
    public Page<BalanceHistoryInstructorResponse> getMyBalanceHistories(String userId, BalanceHistoryFilterRequest filter) {
        // Khoi tao Pageable voi filter:
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by("createdAt").descending());

        // Lay timezone mac dinh:
        ZoneId zoneId = ZoneId.systemDefault();

        Specification<BalanceHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lay theo instructorId:
            predicates.add(cb.equal(root.get("instructorId"), userId));

            // Kiem tra filter va thuc hien theo balanceType:
            if (filter.getBalanceType() != null) {
                predicates.add(cb.equal(root.get("balanceType"), filter.getBalanceType()));
            }

            // Kiem tra filter va thuc hien theo referenceId:
            if (filter.getReferenceId() != null) {
                predicates.add(cb.equal(root.get("referenceId"), filter.getReferenceId()));
            }

            // Kiem tra filter va thuc hien theo startDate:
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay(zoneId).toInstant()));
            }

            // Kiem tra filter va thuc hien theo endDate:
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atStartOfDay(zoneId).toInstant()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return historyRepository.findAll(spec, pageable).map(this::convertToResponse);
    }

    private BalanceHistoryInstructorResponse convertToResponse (BalanceHistory entry) {
        BalanceHistoryInstructorResponse.Impact impact = new BalanceHistoryInstructorResponse.Impact();
        BalanceHistoryInstructorResponse.Impact secondImpact = null;
        LocalDateTime createdAt = entry.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime();

        switch(entry.getTransactionType()) {
            case DEPOSIT_FROM_ORDER:
                impact.setBalanceType(BalanceType.PENDING);
                impact.setAction(BalanceAction.INCREASE);
                impact.setAmountChanged(entry.getAmount());
                break;

            case RELEASE_REVENUE:
                impact.setBalanceType(BalanceType.AVAILABLE);
                impact.setAction(BalanceAction.INCREASE);
                impact.setAmountChanged(entry.getAmount());

                secondImpact = new BalanceHistoryInstructorResponse.Impact(
                        BalanceType.PENDING,
                        BalanceAction.DECREASE,
                        entry.getAmount()
                );
                break;

            case REFUND_DEDUCTION:
                impact.setBalanceType(BalanceType.PENDING);
                impact.setAction(BalanceAction.DECREASE);
                impact.setAmountChanged(entry.getAmount());
                break;

            case BLOCK_FOR_PAYOUT:
                impact.setBalanceType(BalanceType.AVAILABLE);
                impact.setAction(BalanceAction.DECREASE);
                impact.setAmountChanged(entry.getAmount());

                secondImpact = new BalanceHistoryInstructorResponse.Impact(
                        BalanceType.BLOCKED,
                        BalanceAction.INCREASE,
                        entry.getAmount()
                );
                break;

            case WITHDRAW_SUCCESS:
                impact.setBalanceType(BalanceType.BLOCKED);
                impact.setAction(BalanceAction.DECREASE);
                impact.setAmountChanged(entry.getAmount());
                break;

            case WITHDRAW_REJECTED:
                impact.setBalanceType(BalanceType.AVAILABLE);
                impact.setAction(BalanceAction.INCREASE);
                impact.setAmountChanged(entry.getAmount());

                secondImpact = new BalanceHistoryInstructorResponse.Impact(
                        BalanceType.BLOCKED,
                        BalanceAction.DECREASE,
                        entry.getAmount()
                );
                break;

            default:
                throw new AppException(FinanceErrorCode.UNKNOWN_TRANSACTION_TYPE);
        }

        return BalanceHistoryInstructorResponse.builder()
                .referenceId(entry.getReferenceId())
                .transactionType(entry.getTransactionType())
                .amount(entry.getAmount())
                .createdAt(createdAt)
                .impact(impact)
                .secondImpact(secondImpact)
                .build();
    }
}
