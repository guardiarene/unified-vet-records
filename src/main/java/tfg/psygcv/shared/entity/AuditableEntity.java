package tfg.psygcv.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("active = true")
public abstract class AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Long id;

  @CreatedDate
  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "UPDATED_AT")
  private LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "CREATED_BY", nullable = false, updatable = false)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "UPDATED_BY")
  private String updatedBy;

  @Column(name = "DELETED_AT")
  private LocalDateTime deletedAt;

  @Column(name = "DELETED_BY")
  private String deletedBy;

  @NotNull
  @Column(name = "ACTIVE", nullable = false)
  private Boolean active = true;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  @PrePersist
  protected void onCreate() {
    if (active == null) {
      active = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    if (Boolean.FALSE.equals(active) && deletedAt == null) {
      // AuditingEntityListener runs before this callback, so updatedAt already holds the Clock time
      deletedAt = updatedAt;
      if (isBlank(deletedBy)) {
        deletedBy = !isBlank(updatedBy) ? updatedBy : createdBy;
      }
    }
    if (Boolean.TRUE.equals(active) && deletedAt != null) {
      deletedAt = null;
      deletedBy = null;
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuditableEntity other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
