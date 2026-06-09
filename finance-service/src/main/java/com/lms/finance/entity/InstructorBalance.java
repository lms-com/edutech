package com.lms.finance.entity;

import com.lms.common.exception.AppException;
import com.lms.finance.exception.FinanceErrorCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "instructor_balances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_instructor_balances_instructor_id",
                        columnNames = "instructor_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class InstructorBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_id", length = 36, nullable = false, unique = true)
    String instructorId;

    @Column(name = "currency_code", length = 3, nullable = false)
    String currencyCode = "VND";

    /**
     * Tổng so du cua giang vien ma he thong dang giu
     * actualBalance = availableBalance + blockedBalance + pendingBalance
     */
    @Column(name = "actual_balance", precision = 15, scale = 2, nullable = false)
    BigDecimal actualBalance = BigDecimal.ZERO;

    /**
     * Số dư có thể rút. Đơn vị: VND (BIGINT — không dùng DECIMAL tránh lỗi làm tròn).
     * DB CHECK constraint đảm bảo >= 0.
     */
    @Column(name = "available_balance", precision = 15, scale = 2, nullable = false)
    BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Số tiền đang bị đóng băng chờ Admin duyệt payout.
     * DB CHECK constraint đảm bảo >= 0.
     */
    @Column(name = "blocked_balance", precision = 15, scale = 2, nullable = false)
    BigDecimal blockedBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", precision = 15, scale = 2, nullable = false)
    BigDecimal pendingBalance = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    Instant updatedAt;

    @Column(nullable = false)
    Long version = 0L;


    public static InstructorBalance createNewWallet (String instructorId) {
        InstructorBalance wallet = new InstructorBalance();
        wallet.instructorId = instructorId;
        wallet.actualBalance = BigDecimal.ZERO;
        wallet.availableBalance = BigDecimal.ZERO;
        wallet.blockedBalance = BigDecimal.ZERO;
        wallet.pendingBalance = BigDecimal.ZERO;
        return wallet;
    }

    /**
     * Ham dong bang tien khi giang vient ao lenh rut
     **/
    public void blockFundsForPayout (BigDecimal amount) {
        // Kiem tra trong co du tien trong available:
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE);
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.blockedBalance = this.blockedBalance.add(amount);
        this.recalculateActualBalance();
    }

    /**
     * Ham tinh tien actualBalance
     */
    private void recalculateActualBalance () {
        this.actualBalance = this.availableBalance
                .add(this.pendingBalance)
                .add(this.blockedBalance);
    }

    /**
     * Ham cong tien cho giang vien sau khi khoa hoc duoc ban
     * Chu y, tien se vao pendingBalance de cho chinh sach 7 ngay sau moi vao available balance
     */
    public void depositComission (BigDecimal amount) {
        if (this.availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(FinanceErrorCode.INVALID_AMOUNT, "The amount must be greater than 0");
        }
        this.pendingBalance = this.pendingBalance.add(amount);
        this.recalculateActualBalance();
    }

    /**
     *  Hàm giải phóng tiền (Chạy tự động sau 7 ngày).
     * Chuyển tiền từ ví PENDING sang ví AVAILABLE để giảng viên được quyền rút.
     */
    public void releasePendingToAvailable(BigDecimal amount) {
        if (this.pendingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE,
                    "The pending balance is insufficient to complete the transaction");
        }
        this.pendingBalance = this.pendingBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.recalculateActualBalance();
    }

    /**
     *  Hàm xử lý khi đơn hàng bị hoàn tiền trong vòng 7 ngày (Học viên đòi REFUND).
     * Hệ thống sẽ khấu trừ trực tiếp vào ví PENDING của giảng viên.
     */
    public void deductRefundFromPending(BigDecimal amount) {
        if (this.pendingBalance.compareTo(amount) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE,
                    "The pending balance is insufficient to complete the transaction");
        }
        this.pendingBalance = this.pendingBalance.subtract(amount);
        this.recalculateActualBalance();
    }

    /**
     *  Hàm chạy khi Admin DUYỆT rút tiền THÀNH CÔNG.
     * Tiền sẽ thực sự biến mất khỏi hệ thống (ví BLOCKED giảm, tổng ACTUAL giảm).
     */
    public void completePayout(BigDecimal amount) {
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE,
                    "The blocked balance is insufficient to complete the transaction");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.recalculateActualBalance();
    }

    /**
     *  Hàm chạy khi Admin TỪ CHỐI lệnh rút tiền (Lý do sai số tài khoản, sai tên...).
     * Tiền bị đóng băng phải được trả lại ví AVAILABLE để họ thực hiện lại.
     */
    public void rejectPayout(BigDecimal amount) {
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new AppException(FinanceErrorCode.INSUFFICTION_BALANCE,
                    "The blocked balance is insufficient to complete the transaction");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.recalculateActualBalance();
    }
}
