package co.edu.docurural.dashboard.service;

import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.dashboard.dto.CategoryDistributionItemResponse;
import co.edu.docurural.dashboard.dto.DashboardStatsResponse;
import co.edu.docurural.dashboard.dto.RecentDocumentResponse;
import co.edu.docurural.dashboard.dto.SummaryResponse;
import co.edu.docurural.dashboard.dto.TopCategoryResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agrega y expone los datos del dashboard (DSH-01).
 *
 * <p>Resuelve en una sola transacción de solo lectura: totales del repositorio,
 * distribución por categoría y los 10 documentos más recientes (HU-24, HU-25, HU-26).
 * El endpoint no registra en {@code activity_log} (consulta de solo lectura sin auditoría).
 *
 * <p>Nota de zona horaria: {@link LocalDate#now()} usa la zona del sistema. Esto es
 * coherente con {@code created_at} que se almacena como {@code TIMESTAMP} sin zona
 * (también fijado con {@code LocalDateTime.now()} del servidor en {@link Document#onCreate()}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Retorna todos los datos necesarios para renderizar el dashboard en una sola llamada.
     *
     * @return agregado con resumen estadístico, distribución por categoría y documentos recientes
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalActive = documentRepository.countByStatus(DocumentStatus.ACTIVE);

        LocalDateTime firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long thisMonth = documentRepository.countUploadedSince(DocumentStatus.ACTIVE, firstOfMonth);

        List<CategoryDocumentCount> countRows =
                documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE);
        Map<Long, Long> countByCategory = countRows.stream()
                .collect(Collectors.toMap(CategoryDocumentCount::getCategoryId,
                                          CategoryDocumentCount::getCount));

        Map<Long, String> nameByCategory = categoryRepository
                .findAllBy(Sort.by("name")).stream()
                .collect(Collectors.toMap(CategoryNameView::getId, CategoryNameView::getName));

        TopCategoryResponse topCategory = buildTopCategory(countByCategory, nameByCategory, totalActive);
        List<CategoryDistributionItemResponse> distribution =
                buildDistribution(countByCategory, nameByCategory, totalActive);

        List<Document> recentDocs =
                documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE);
        List<RecentDocumentResponse> recentDocuments = recentDocs.stream()
                .map(DashboardService::toRecentDocumentResponse)
                .toList();

        SummaryResponse summary = new SummaryResponse(totalActive, thisMonth, topCategory);
        return new DashboardStatsResponse(summary, distribution, recentDocuments);
    }

    private static TopCategoryResponse buildTopCategory(Map<Long, Long> countByCategory,
                                                        Map<Long, String> nameByCategory,
                                                        long totalActive) {
        if (totalActive == 0 || countByCategory.isEmpty()) {
            return null;
        }
        return countByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> new TopCategoryResponse(
                        nameByCategory.getOrDefault(e.getKey(), ""),
                        e.getValue()))
                .orElse(null);
    }

    private static List<CategoryDistributionItemResponse> buildDistribution(
            Map<Long, Long> countByCategory,
            Map<Long, String> nameByCategory,
            long totalActive) {

        if (totalActive == 0 || countByCategory.isEmpty()) {
            return List.of();
        }
        return countByCategory.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Comparator.comparingLong(Map.Entry<Long, Long>::getValue).reversed())
                .map(e -> {
                    double percentage = Math.round(e.getValue() * 10000.0 / totalActive) / 100.0;
                    return new CategoryDistributionItemResponse(
                            nameByCategory.getOrDefault(e.getKey(), ""),
                            e.getValue(),
                            percentage);
                })
                .toList();
    }

    private static RecentDocumentResponse toRecentDocumentResponse(Document doc) {
        return new RecentDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory().getName(),
                doc.getResponsibleArea(),
                doc.getFileFormat(),
                doc.getCreatedAt());
    }
}
