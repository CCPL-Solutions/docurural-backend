package co.edu.docurural.dashboard.service;

import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.category.service.CategoryService;
import co.edu.docurural.dashboard.dto.CategoryDistributionItemResponseDto;
import co.edu.docurural.dashboard.dto.DashboardStatsResponseDto;
import co.edu.docurural.dashboard.dto.RecentDocumentResponseDto;
import co.edu.docurural.dashboard.dto.SummaryResponseDto;
import co.edu.docurural.dashboard.dto.TopCategoryResponseDto;
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
 * Los nombres de categorías se obtienen a través de {@link CategoryService} para
 * respetar la independencia de módulos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final DocumentRepository documentRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponseDto getStats() {
        long totalActive = documentRepository.countByStatus(DocumentStatus.ACTIVE);

        LocalDateTime firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long thisMonth = documentRepository.countUploadedSince(DocumentStatus.ACTIVE, firstOfMonth);

        List<CategoryDocumentCount> countRows =
                documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE);
        Map<Long, Long> countByCategory = countRows.stream()
                .collect(Collectors.toMap(CategoryDocumentCount::getCategoryId,
                                          CategoryDocumentCount::getCount));

        Map<Long, String> nameByCategory = categoryService
                .findAllCategoryNames(Sort.by("name")).stream()
                .collect(Collectors.toMap(CategoryNameView::getId, CategoryNameView::getName));

        TopCategoryResponseDto topCategory = buildTopCategory(countByCategory, nameByCategory, totalActive);
        List<CategoryDistributionItemResponseDto> distribution =
                buildDistribution(countByCategory, nameByCategory, totalActive);

        List<Document> recentDocs =
                documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE);
        List<RecentDocumentResponseDto> recentDocuments = recentDocs.stream()
                .map(DashboardServiceImpl::toRecentDocumentResponse)
                .toList();

        SummaryResponseDto summary = new SummaryResponseDto(totalActive, thisMonth, topCategory);
        return new DashboardStatsResponseDto(summary, distribution, recentDocuments);
    }

    private static TopCategoryResponseDto buildTopCategory(Map<Long, Long> countByCategory,
                                                           Map<Long, String> nameByCategory,
                                                           long totalActive) {
        if (totalActive == 0 || countByCategory.isEmpty()) {
            return null;
        }
        return countByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> new TopCategoryResponseDto(
                        nameByCategory.getOrDefault(e.getKey(), ""),
                        e.getValue()))
                .orElse(null);
    }

    private static List<CategoryDistributionItemResponseDto> buildDistribution(
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
                    return new CategoryDistributionItemResponseDto(
                            nameByCategory.getOrDefault(e.getKey(), ""),
                            e.getValue(),
                            percentage);
                })
                .toList();
    }

    private static RecentDocumentResponseDto toRecentDocumentResponse(Document doc) {
        return new RecentDocumentResponseDto(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory().getName(),
                doc.getResponsibleArea(),
                doc.getFileFormat(),
                doc.getCreatedAt());
    }
}
