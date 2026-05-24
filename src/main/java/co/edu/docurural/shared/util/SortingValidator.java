package co.edu.docurural.shared.util;

import co.edu.docurural.shared.exception.BusinessErrorCode;
import co.edu.docurural.shared.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Centraliza la resolución de parámetros de ordenamiento y paginación.
 * Los callers pasan claves de mensaje (no mensajes ya resueltos), reduciendo el
 * boilerplate de {@link MessageResolver} en cada servicio.
 */
@Component
@RequiredArgsConstructor
public class SortingValidator {

    private final MessageResolver messages;

    /**
     * Encapsula la configuración fija de paginación para un endpoint concreto,
     * eliminando el long parameter list de {@link #resolvePageable}.
     */
    public record PageableConfig(
            Set<String> allowedFields,
            String defaultSortBy,
            String defaultSortDir,
            int defaultPage,
            int defaultSize,
            int maxSize,
            String pageMsgKey,
            String sizeMsgKey,
            String fieldMsgKey,
            String dirMsgKey
    ) {}

    public Sort resolveSort(String sortBy, String sortDir,
                            Set<String> allowedFields,
                            String defaultField, String defaultDir,
                            String fieldMsgKey, String dirMsgKey) {
        String field = (sortBy == null || sortBy.isBlank()) ? defaultField : sortBy;
        String dir = (sortDir == null || sortDir.isBlank()) ? defaultDir : sortDir;

        if (!allowedFields.contains(field)) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(fieldMsgKey, field));
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(dir.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(dirMsgKey, dir));
        }
        return Sort.by(direction, field);
    }

    public Pageable resolvePageable(Integer page, Integer size,
                                    String sortBy, String sortDir,
                                    PageableConfig config) {
        int resolvedPage = (page == null) ? config.defaultPage() : page;
        int resolvedSize = (size == null) ? config.defaultSize() : size;

        if (resolvedPage < 1) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(config.pageMsgKey()));
        }
        if (resolvedSize < 1 || resolvedSize > config.maxSize()) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(config.sizeMsgKey()));
        }

        Sort sort = resolveSort(sortBy, sortDir, config.allowedFields(),
                config.defaultSortBy(), config.defaultSortDir(),
                config.fieldMsgKey(), config.dirMsgKey());
        return PageRequest.of(resolvedPage - 1, resolvedSize, sort);
    }
}
