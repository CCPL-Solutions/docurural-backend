package co.edu.docurural.shared.util;

import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

/**
 * Centraliza la resolución de parámetros de ordenamiento y paginación,
 * eliminando la lógica duplicada en {@code DocumentService}, {@code UserService}
 * y {@code CategoryService}.
 *
 * <p>Los mensajes de error se pasan ya resueltos (vía {@link MessageResolver})
 * para que la utilidad no dependa de Spring i18n.
 */
public final class SortingValidator {

    private SortingValidator() {
    }

    /**
     * Valida y construye un {@link Sort} a partir de parámetros de ordenamiento.
     *
     * @param sortBy        campo de ordenamiento recibido (puede ser null/blank).
     * @param sortDir       dirección asc/desc recibida (puede ser null/blank).
     * @param allowedFields conjunto de campos permitidos.
     * @param defaultField  campo por defecto.
     * @param defaultDir    dirección por defecto.
     * @param fieldErrorMsg mensaje de error ya resuelto para campo inválido.
     * @param dirErrorMsg   mensaje de error ya resuelto para dirección inválida.
     */
    public static Sort resolveSort(String sortBy, String sortDir,
                                   Set<String> allowedFields,
                                   String defaultField, String defaultDir,
                                   String fieldErrorMsg, String dirErrorMsg) {
        String field = (sortBy == null || sortBy.isBlank()) ? defaultField : sortBy;
        String dir = (sortDir == null || sortDir.isBlank()) ? defaultDir : sortDir;

        if (!allowedFields.contains(field)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, fieldErrorMsg);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(dir.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, dirErrorMsg);
        }
        return Sort.by(direction, field);
    }

    /**
     * Valida y construye un {@link Pageable} con página, tamaño y ordenamiento.
     *
     * @param page          número de página 1-based (null → defaultPage).
     * @param size          tamaño de página (null → defaultSize).
     * @param sortBy        campo de ordenamiento (null → defaultField).
     * @param sortDir       dirección asc/desc (null → defaultDir).
     * @param allowedFields campos permitidos.
     * @param defaultField  campo por defecto.
     * @param defaultDir    dirección por defecto.
     * @param defaultPage   página por defecto (1-based).
     * @param defaultSize   tamaño por defecto.
     * @param maxSize       tamaño máximo permitido.
     * @param pageErrorMsg  mensaje de error ya resuelto para página inválida.
     * @param sizeErrorMsg  mensaje de error ya resuelto para tamaño inválido.
     * @param fieldErrorMsg mensaje de error ya resuelto para campo inválido.
     * @param dirErrorMsg   mensaje de error ya resuelto para dirección inválida.
     */
    public static Pageable resolvePageable(Integer page, Integer size,
                                           String sortBy, String sortDir,
                                           Set<String> allowedFields,
                                           String defaultField, String defaultDir,
                                           int defaultPage, int defaultSize, int maxSize,
                                           String pageErrorMsg, String sizeErrorMsg,
                                           String fieldErrorMsg, String dirErrorMsg) {
        int resolvedPage = (page == null) ? defaultPage : page;
        int resolvedSize = (size == null) ? defaultSize : size;

        if (resolvedPage < 1) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, pageErrorMsg);
        }
        if (resolvedSize < 1 || resolvedSize > maxSize) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT, sizeErrorMsg);
        }

        Sort sort = resolveSort(sortBy, sortDir, allowedFields, defaultField, defaultDir,
                fieldErrorMsg, dirErrorMsg);
        return PageRequest.of(resolvedPage - 1, resolvedSize, sort);
    }
}
