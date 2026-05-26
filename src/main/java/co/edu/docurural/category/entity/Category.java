package co.edu.docurural.category.entity;

import co.edu.docurural.user.entity.User;
import co.edu.docurural.category.enums.CategoryStatus;
import co.edu.docurural.shared.enums.SensitivityLevel;
import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Categoría documental (tabla {@code categories}).
 *
 * <p>{@link #createdBy} es nullable para soportar el seed de categorías de Flyway
 * ({@code V2__seed_categories.sql}), que se ejecuta antes de existir cualquier
 * usuario administrador.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CategoryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_sensitivity_level", nullable = false, length = 20)
    private SensitivityLevel defaultSensitivityLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Garantiza que la categoría está ACTIVE antes de permitir edición. */
    public void assertEditable(String errorMessage) {
        if (this.status == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException(BusinessErrorCode.FORBIDDEN, errorMessage);
        }
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = CategoryStatus.ACTIVE;
        }
        if (defaultSensitivityLevel == null) {
            defaultSensitivityLevel = SensitivityLevel.INTERNAL;
        }
    }
}
