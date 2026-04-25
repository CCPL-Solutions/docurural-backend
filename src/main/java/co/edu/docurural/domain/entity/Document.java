package co.edu.docurural.domain.entity;

import co.edu.docurural.domain.enums.DocumentFormat;
import co.edu.docurural.domain.enums.DocumentStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Metadatos de un documento cargado al sistema (tabla {@code documents}).
 *
 * <p>La entidad queda completa aunque los endpoints operacionales sobre documentos
 * se implementan hasta Sprint 2; se define en Sprint 1 para dejar consistentes
 * las FKs y permitir que {@code ActivityLog} la referencie.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "responsible_area", nullable = false, length = 100)
    private String responsibleArea;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 20)
    private DocumentFormat fileFormat;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DocumentStatus.ACTIVE;
        }
    }
}
