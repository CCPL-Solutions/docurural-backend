package co.edu.docurural.config.security;

/**
 * Constantes compartidas entre los componentes de seguridad.
 */
public final class SecurityConstants {

    /**
     * Nombre del atributo en el {@code HttpServletRequest} donde el filtro JWT
     * registra la excepción que produjo un token inválido/expirado, para que el
     * {@code AuthenticationEntryPoint} pueda elegir el mensaje de error adecuado.
     */
    public static final String JWT_ERROR_ATTRIBUTE = "co.edu.docurural.security.jwtError";

    private SecurityConstants() {
        // util class
    }
}
