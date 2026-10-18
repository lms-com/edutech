package com.lms.finance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Query parameters for filtering and paginating payout request")
public class PayoutFilterRequest {

    @Schema(description = "Page number to retrieve (starts from 0)", example = "0")
    int page = 0;

    @Schema(description = "Number of records per page (max 100)", example = "20")
    int size = 20;

    @Schema(description = "Field to sort by (e.g., createdDate, amount, status)", example = "createdAt")
    String sortBy = "createdAt"; // đặt giá trị mặc định để tránh lỗi Null

    @Schema(description = "Sort direction (asc or desc)", example = "desc")
    String sortDir = "desc";       // đặt mặc định là desc để xem cái mới nhất trước

    @Schema(description = "Instructor Id", example = "instr-0006-0010-2026")
    String instructorId;

    @Schema(description = "Admin Id", example = "admin-0006-0010-2026")
    String adminId;

    @Schema(description = "Min amount", example = "500000")
    BigDecimal minAmount;

    @Schema(description = "Max amount", example = "1000000")
    BigDecimal maxAmount;

    @Schema(description = "Status of Payout request", example = "PENDING")
    @Pattern(regexp = "PENDING|SUCCESS|REJECTED", message = "Invalid Status")
    String status;

    @Schema(description = "Created Date of Payout request starts at", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate createdFromDate;

    @Schema(description = "Created Date of Payout request ends at", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate createdToDate;

    @Schema(description = "Processed Date of Payout request starts at", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate processedFromDate;

    @Schema(description = "Created Date of Payout request ends at", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate processedToDate;
}
