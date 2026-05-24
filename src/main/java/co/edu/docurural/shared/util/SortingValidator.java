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
                                    Set<String> allowedFields,
                                    String defaultField, String defaultDir,
                                    int defaultPage, int defaultSize, int maxSize,
                                    String pageMsgKey, String sizeMsgKey,
                                    String fieldMsgKey, String dirMsgKey) {
        int resolvedPage = (page == null) ? defaultPage : page;
        int resolvedSize = (size == null) ? defaultSize : size;

        if (resolvedPage < 1) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(pageMsgKey));
        }
        if (resolvedSize < 1 || resolvedSize > maxSize) {
            throw new BusinessRuleException(BusinessErrorCode.INVALID_ARGUMENT,
                    messages.get(sizeMsgKey));
        }

        Sort sort = resolveSort(sortBy, sortDir, allowedFields, defaultField, defaultDir,
                fieldMsgKey, dirMsgKey);
        return PageRequest.of(resolvedPage - 1, resolvedSize, sort);
    }
}
