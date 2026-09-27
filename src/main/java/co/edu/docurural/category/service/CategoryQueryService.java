package co.edu.docurural.category.service;

/**
 * Consultas de solo lectura del módulo {@code category} para otros módulos.
 *
 * <p>No depende del módulo {@code document}: {@code CategoryServiceImpl} ya inyecta servicios de
 * {@code document}, así que exponer estas consultas desde {@link CategoryService} cerraría un ciclo
 * cuando {@code document} las necesite (desviación D-4 de la constitución).
 */
public interface CategoryQueryService {

    /**
     * Indica si los documentos que se carguen en la categoría deben pasar por el flujo de
     * aprobación (HU-31). Lee el valor vigente en cada llamada: los consumidores deben invocarlo
     * en cada carga, sin cachear el resultado.
     *
     * @throws co.edu.docurural.shared.exception.ResourceNotFoundException si la categoría no existe
     */
    boolean requiresApproval(Long categoryId);
}
