package co.edu.docurural.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración CORS global. El {@link CorsConfigurationSource} expuesto aquí es
 * consumido por Spring Security vía {@code cors(Customizer.withDefaults())} en
 * {@code SecurityConfig}.
 *
 * <p>La lista de orígenes permitidos se lee de la propiedad
 * {@code docurural.cors.allowed-origins} (CSV). Si la propiedad no está presente,
 * el default es el dev server de Angular: {@code http://localhost:4200}.
 *
 * <p>Se permite envío de credenciales (cookies/Authorization) y se expone el header
 * {@code Authorization} para que el frontend pueda leer el token si llegase a
 * recibirlo en la respuesta.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${docurural.cors.allowed-origins:http://localhost:4200}") String[] allowedOrigins) {
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
