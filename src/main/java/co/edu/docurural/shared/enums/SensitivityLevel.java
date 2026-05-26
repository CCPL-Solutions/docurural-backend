package co.edu.docurural.shared.enums;

/**
 * Niveles de sensibilidad para documentos y categorías (HU-28 / HU-28B).
 *
 * <p>El orden de declaración es significativo: {@code INTERNAL < RESTRICTED < CONFIDENTIAL}.
 * Las comparaciones con {@code compareTo} o {@code ordinal()} reflejan esa jerarquía.
 */
public enum SensitivityLevel {

    INTERNAL,
    RESTRICTED,
    CONFIDENTIAL;

    /** Retorna {@code true} si este nivel es igual o superior al nivel indicado. */
    public boolean isAtLeast(SensitivityLevel other) {
        return this.compareTo(other) >= 0;
    }
}
