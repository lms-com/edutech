package com.lms.finance.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "bank_accounts", indexes = {
        @Index(name = "idx_bank_instructor", columnList = "instructor_id"),
        @Index(name = "idx_bank_instructor_primary", columnList = "instructor_id, is_primary")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    @Column(name = "instructor_id", length = 36, nullable = false)
    String instructorId;

    /** Mã ngân hàng: VCB, TCB, MB, ACB... */
    @Column(name = "bank_code", length = 20, nullable = false)
    String bankCode;

    @Column(name = "account_number", length = 50, nullable = false)
    String accountNumber;

    /** Tên chủ tài khoản — phải khớp chính xác với tên tại ngân hàng */
    @Column(name = "account_name", length = 200, nullable = false)
    String accountName;

    /**
     * Tài khoản mặc định để rút tiền (1 = primary).
     * MySQL không hỗ trợ Partial Unique Index natively
     * → enforce "chỉ 1 primary per instructor" ở Service layer.
     */
    @Column(name = "is_primary", nullable = false)
    Boolean primary;

    @Column(name = "is_deleted", nullable = false)
    Boolean deleted;

    @PrePersist
    public void prePersist() {
        if (this.primary == null) this.primary = false;
        if (this.deleted == null) this.deleted = false;
    }
}
