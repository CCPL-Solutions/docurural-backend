package co.edu.docurural.shared.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Acumula actualizaciones de campo sobre una entidad y registra los nombres de los
 * campos que efectivamente cambiaron. Evita duplicar el patrón
 * {@code if (!newValue.equals(current)) { setter.accept(newValue); list.add(name); }}
 * en múltiples services.
 *
 * <p>Uso:
 * <pre>
 * List&lt;String&gt; changed = FieldUpdater.of(entity)
 *     .setIfChanged("title", request.title(), entity::getTitle, entity::setTitle)
 *     .setIfPresent("description", request.description(), entity::getDescription, entity::setDescription)
 *     .changedFields();
 * </pre>
 */
public final class FieldUpdater<T> {

    private final T target;
    private final List<String> changedFields = new ArrayList<>();

    private FieldUpdater(T target) {
        this.target = target;
    }

    public static <T> FieldUpdater<T> of(T target) {
        return new FieldUpdater<>(target);
    }

    /**
     * Aplica {@code setter} si {@code newValue} difiere del valor actual según {@code getter}.
     * El {@code newValue} puede ser {@code null} (se compara normalmente con {@code Objects.equals}).
     */
    public <V> FieldUpdater<T> setIfChanged(String fieldName, V newValue,
                                             Supplier<V> getter, Consumer<V> setter) {
        if (!Objects.equals(newValue, getter.get())) {
            setter.accept(newValue);
            changedFields.add(fieldName);
        }
        return this;
    }

    /**
     * Aplica {@code setter} solo si {@code newValue} no es {@code null} y difiere del actual.
     * Útil para campos opcionales donde {@code null} significa "sin cambio".
     */
    public <V> FieldUpdater<T> setIfPresent(String fieldName, V newValue,
                                             Supplier<V> getter, Consumer<V> setter) {
        if (newValue != null && !Objects.equals(newValue, getter.get())) {
            setter.accept(newValue);
            changedFields.add(fieldName);
        }
        return this;
    }

    /** Devuelve una copia inmutable de los campos modificados. */
    public List<String> changedFields() {
        return List.copyOf(changedFields);
    }
}
