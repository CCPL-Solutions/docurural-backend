package co.edu.docurural.dashboard.service;

import co.edu.docurural.category.repository.CategoryRepository;
import co.edu.docurural.category.repository.projection.CategoryNameView;
import co.edu.docurural.dashboard.dto.CategoryDistributionItemResponse;
import co.edu.docurural.dashboard.dto.DashboardStatsResponse;
import co.edu.docurural.document.entity.Document;
import co.edu.docurural.document.enums.DocumentStatus;
import co.edu.docurural.document.repository.DocumentRepository;
import co.edu.docurural.document.repository.projection.CategoryDocumentCount;
import co.edu.docurural.support.TestFixtures;
import co.edu.docurural.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    DocumentRepository documentRepository;
    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // Helpers de stub
    // -------------------------------------------------------------------------

    private void stubEmpty() {
        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(0L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(categoryRepository.findAllBy(any(Sort.class))).thenReturn(List.of());
    }

    /** Projection anónima para simular el resultado del GROUP BY. */
    private static CategoryDocumentCount countRow(Long categoryId, long count) {
        return new CategoryDocumentCount() {
            @Override public Long getCategoryId() { return categoryId; }
            @Override public Long getCount() { return count; }
        };
    }

    /** Projection anónima ligera id/name para el stub del dashboard. */
    private static CategoryNameView nameView(Long id, String name) {
        return new CategoryNameView() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
        };
    }

    // -------------------------------------------------------------------------
    // Repositorio vacío
    // -------------------------------------------------------------------------

    @Test
    void getStats_returnsZeroAndNullTop_whenRepositoryIsEmpty() {
        stubEmpty();

        DashboardStatsResponse result = dashboardService.getStats();

        assertThat(result.summary().totalActiveDocuments()).isZero();
        assertThat(result.summary().documentsUploadedThisMonth()).isZero();
        assertThat(result.summary().topCategory()).isNull();
        assertThat(result.categoryDistribution()).isEmpty();
        assertThat(result.recentDocuments()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Repositorio con datos
    // -------------------------------------------------------------------------

    @Test
    void getStats_returnsAggregateWithTopCategory_whenRepositoryHasDocuments() {
        User admin = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(1L, TestFixtures.categoryActive(1L, "Actas"), admin);

        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(29L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(countRow(1L, 18L), countRow(2L, 11L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of(doc));
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(nameView(1L, "Actas"), nameView(2L, "Informes")));

        DashboardStatsResponse result = dashboardService.getStats();

        assertThat(result.summary().totalActiveDocuments()).isEqualTo(29L);
        assertThat(result.summary().documentsUploadedThisMonth()).isEqualTo(5L);
        assertThat(result.summary().topCategory()).isNotNull();
        assertThat(result.summary().topCategory().name()).isEqualTo("Actas");
        assertThat(result.summary().topCategory().count()).isEqualTo(18L);
        assertThat(result.recentDocuments()).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Cálculo de porcentajes
    // -------------------------------------------------------------------------

    @Test
    void getStats_calculatesPercentagesWithTwoDecimals() {
        // Valores del ejemplo de la spec: {18,11,8,6,4}/47
        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(47L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(
                        countRow(1L, 18L), countRow(2L, 11L), countRow(3L, 8L),
                        countRow(4L, 6L), countRow(5L, 4L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(
                        nameView(1L, "Actas"), nameView(2L, "Circulares"),
                        nameView(3L, "Contratos"), nameView(4L, "Informes"), nameView(5L, "PRAE")));

        DashboardStatsResponse result = dashboardService.getStats();

        List<CategoryDistributionItemResponse> dist = result.categoryDistribution();
        // Primer elemento debe ser Actas (mayor count)
        assertThat(dist.get(0).categoryName()).isEqualTo("Actas");
        assertThat(dist.get(0).percentage()).isEqualTo(38.30);
        assertThat(dist.get(1).percentage()).isEqualTo(23.40);
        assertThat(dist.get(2).percentage()).isEqualTo(17.02);
        assertThat(dist.get(3).percentage()).isEqualTo(12.77);
        assertThat(dist.get(4).percentage()).isEqualTo(8.51);
    }

    // -------------------------------------------------------------------------
    // Categorías sin documentos no aparecen en la distribución
    // -------------------------------------------------------------------------

    @Test
    void getStats_excludesCategoriesWithoutActiveDocuments() {
        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(10L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        // Solo la categoría 1 tiene documentos
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(countRow(1L, 10L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(nameView(1L, "Actas"), nameView(2L, "Sin Documentos")));

        DashboardStatsResponse result = dashboardService.getStats();

        assertThat(result.categoryDistribution()).hasSize(1);
        assertThat(result.categoryDistribution().get(0).categoryName()).isEqualTo("Actas");
    }

    // -------------------------------------------------------------------------
    // Documentos ACTIVE cuya categoría está INACTIVE siguen contando
    // -------------------------------------------------------------------------

    @Test
    void getStats_includesDocumentsFromInactiveCategories() {
        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(3L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(countRow(99L, 3L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(nameView(99L, "Categoría Inactiva")));

        DashboardStatsResponse result = dashboardService.getStats();

        assertThat(result.categoryDistribution()).hasSize(1);
        assertThat(result.categoryDistribution().get(0).categoryName()).isEqualTo("Categoría Inactiva");
        assertThat(result.summary().topCategory()).isNotNull();
        assertThat(result.summary().topCategory().name()).isEqualTo("Categoría Inactiva");
    }

    // -------------------------------------------------------------------------
    // Documentos recientes — mapeo de campos
    // -------------------------------------------------------------------------

    @Test
    void getStats_recentDocumentsMapOnlySpecFields() {
        User admin = TestFixtures.userAdmin(10L);
        Document doc = TestFixtures.documentActive(42L, TestFixtures.categoryActive(1L, "Actas"), admin);

        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(1L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(countRow(1L, 1L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of(doc));
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(nameView(1L, "Actas")));

        DashboardStatsResponse result = dashboardService.getStats();

        assertThat(result.recentDocuments()).hasSize(1);
        var recent = result.recentDocuments().get(0);
        assertThat(recent.id()).isEqualTo(42L);
        assertThat(recent.title()).isEqualTo(doc.getTitle());
        assertThat(recent.category()).isEqualTo("Actas");
        assertThat(recent.responsibleArea()).isEqualTo(doc.getResponsibleArea());
        assertThat(recent.fileFormat()).isEqualTo(doc.getFileFormat());
        assertThat(recent.createdAt()).isEqualTo(TestFixtures.FIXED_CREATED_AT);
    }

    // -------------------------------------------------------------------------
    // Distribución ordenada por count DESC
    // -------------------------------------------------------------------------

    @Test
    void getStats_distributionOrderedByCountDesc() {
        when(documentRepository.countByStatus(DocumentStatus.ACTIVE)).thenReturn(15L);
        when(documentRepository.countUploadedSince(eq(DocumentStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(documentRepository.countActiveByCategoryId(DocumentStatus.ACTIVE))
                .thenReturn(List.of(countRow(3L, 7L), countRow(1L, 5L), countRow(2L, 3L)));
        when(documentRepository.findTop10ByStatusOrderByCreatedAtDesc(DocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(categoryRepository.findAllBy(any(Sort.class)))
                .thenReturn(List.of(nameView(1L, "A"), nameView(2L, "B"), nameView(3L, "C")));

        DashboardStatsResponse result = dashboardService.getStats();

        List<CategoryDistributionItemResponse> dist = result.categoryDistribution();
        assertThat(dist.get(0).categoryName()).isEqualTo("C");
        assertThat(dist.get(1).categoryName()).isEqualTo("A");
        assertThat(dist.get(2).categoryName()).isEqualTo("B");
    }
}
