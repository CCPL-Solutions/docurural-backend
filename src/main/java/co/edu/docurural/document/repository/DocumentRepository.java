package co.edu.docurural.document.repository;

import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndStatus(Long id, DocumentStatus status);

    @Query("""
            SELECT d.category.id AS categoryId, COUNT(d) AS count
            FROM Document d
            WHERE d.status = :status
            GROUP BY d.category.id
            """)
    List<CategoryDocumentCount> countActiveByCategoryId(@Param("status") DocumentStatus status);
}
