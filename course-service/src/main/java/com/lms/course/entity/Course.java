package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Course extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, nullable = false)
    String id;

    // Quan hệ N-1 với bảng Category trong cùng Database
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "instructor_id",length = 36, nullable = false)
    private String instructorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false,unique = true)
    private String slug;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(length = 50)
    private String level;

    // ... các trường phía trên giữ nguyên ...

    @Column(name = "base_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal basePrice;

    // Đã FIX: String thì dùng length, không dùng precision/scale
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(length = 50, nullable = false)
    private String status;

    @Column(name = "override_commission_rate", precision = 3, scale = 2)
    private BigDecimal overrideCommissionRate;

    // Đã FIX: Bỏ chữ "is" ở tên biến Java để Lombok sinh đúng setDeleted() và getDeleted()
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        if(this.basePrice == null) this.basePrice = BigDecimal.ZERO;
        if(this.currencyCode == null) this.currencyCode = "VND";
        if(this.status == null) this.status = "DRAFT";
        // Cập nhật lại tên biến trong hàm này
        if(this.deleted == null) this.deleted = false;
    }


}
