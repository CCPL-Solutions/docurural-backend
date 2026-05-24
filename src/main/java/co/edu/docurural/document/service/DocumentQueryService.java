package co.edu.docurural.document.service;

import co.edu.docurural.document.dto.DocumentDetailResponseDto;

import java.util.Map;

public interface DocumentQueryService {

    DocumentDetailResponseDto findDetailById(Long id);

    /**
     * Conteos de documentos ACTIVE agrupados por categoría.
     * Usado por {@code CategoryService} para enriquecer el listado de categorías.
     *
     * @return mapa de {@code categoryId → count}; categorías sin documentos no aparecen.
     */
    Map<Long, Long> getActiveCountsByCategory();
}
