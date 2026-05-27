package co.edu.docurural.document.repository;

import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import co.edu.docurural.shared.enums.SensitivityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    Optional<Document> findByIdAndStatus(Long id, DocumentStatus status);

    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    Page<Document> findAll(Specification<Document> spec, Pageable pageable);

    @Query("""
            SELECT d.category.id AS categoryId, COUNT(d) AS count
            FROM Document d
            WHERE d.status = :status
            GROUP BY d.category.id
            """)
    List<CategoryDocumentCount> countActiveByCategoryId(@Param("status") DocumentStatus status);

    /** Total de documentos con el status indicado (DSH-01 / HU-24). */
    long countByStatus(DocumentStatus status);

    /**
     * Documentos cargados desde {@code from} (primer instante del mes en curso)
     * con el status indicado (DSH-01 / HU-24).
     */
    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE d.status = :status
              AND d.createdAt >= :from
            """)
    long countUploadedSince(@Param("status") DocumentStatus status,
                            @Param("from") LocalDateTime from);

    /** Últimos 10 documentos cargados con el status indicado (DSH-01 / HU-25). */
    @EntityGraph(attributePaths = {"category", "uploadedBy"})
    List<Document> findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus status);

    /**
     * Sube el sensitivity_level de los documentos activos de una categoría que estén
     * por debajo del nuevo mínimo (HU-28B criterio 5).
     *
     * @param levelsToUpdate niveles estrictamente inferiores al nuevo default.
     * @return número de registros actualizados.
     */
    @Modifying
    @Query("""
            UPDATE Document d
            SET d.sensitivityLevel = :newLevel
            WHERE d.category.id = :categoryId
              AND d.status = co.edu.docurural.document.enums.DocumentStatus.ACTIVE
              AND d.sensitivityLevel IN :levelsToUpdate
            """)
    int raiseSensitivityForActiveDocuments(@Param("categoryId") Long categoryId,
                                           @Param("newLevel") SensitivityLevel newLevel,
                                           @Param("levelsToUpdate") Collection<SensitivityLevel> levelsToUpdate);
}
