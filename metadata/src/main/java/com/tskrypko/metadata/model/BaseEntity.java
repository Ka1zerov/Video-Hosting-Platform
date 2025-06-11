package com.tskrypko.metadata.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

    @Column(updatable = false, nullable = false)
    @CreatedDate
    protected LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    protected LocalDateTime modifiedAt;

    @Column(updatable = false, nullable = false)
    @CreatedBy
    protected String createdBy;

    @Column(nullable = false)
    @LastModifiedBy
    protected String modifiedBy;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
} 