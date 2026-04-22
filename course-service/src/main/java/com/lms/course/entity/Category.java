package com.lms.course.entity;

import com.lms.common.model.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {
    // ...
}