package co.edu.docurural.config.security;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.UserRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Emite y verifica los tokens JWT HS256 del sistema.
 *
 * <p>Los tokens llevan los claims:
 * <ul>
 *   <li>{@code sub} — id numerico del usuario.</li>
 *   <li>{@code email} — correo del usuario autenticado.</li>
 *   <li>{@code role} — rol del usuario ({@link UserRole}).</li>
 *   <li>{@code iss} — emisor configurado en {@link JwtProperties#getIssuer()}.</li>
 *   <li>{@code iat} / {@code exp} — timestamps estandar.</li>
 * </ul>
 *
 * <p>El {@code secret} se obtiene de configuracion y nunca se loggea.
 * Ante tokens expirados se lanza {@link CredentialsExpiredException} para que el
 * {@code AuthenticationEntryPoint} responda 401 con el mensaje de sesion expirada;
 * ante cualquier otra invalidez ({@link JWTVerificationException}) se lanza
 * {@link BadCredentialsException} ("Token invalido").
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    /**
     * Genera un token firmado HS256 a partir del usuario autenticado.
     *
     * @param user usuario dominio cuyo id, email y rol se incrustan en los claims.
     * @return token JWT compacto listo para enviar en el header {@code Authorization: Bearer}.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtProperties.getExpirationMs());

        return JWT.create()
                .withIssuer(jwtProperties.getIssuer())
                .withSubject(String.valueOf(user.getId()))
                .withClaim(CLAIM_EMAIL, user.getEmail())
                .withClaim(CLAIM_ROLE, user.getRole().name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(buildAlgorithm());
    }

    /**
     * Valida firma, emisor y expiracion del token y devuelve los claims resueltos.
     *
     * @throws CredentialsExpiredException si el token expiro.
     * @throws BadCredentialsException     si el token no es valido (firma invalida, mal formado, etc).
     */
    public ParsedJwt parseAndValidate(String token) {
        try {
            JWTVerifier verifier = JWT.require(buildAlgorithm())
                    .withIssuer(jwtProperties.getIssuer())
                    .build();
            DecodedJWT decoded = verifier.verify(token);

            Long userId = Long.parseLong(decoded.getSubject());
            String email = decoded.getClaim(CLAIM_EMAIL).asString();
            String roleName = decoded.getClaim(CLAIM_ROLE).asString();
            UserRole role = UserRole.valueOf(roleName);

            return new ParsedJwt(userId, email, role);
        } catch (TokenExpiredException ex) {
            log.debug("JWT expirado: {}", ex.getMessage());
            throw new CredentialsExpiredException(
                    "Su sesion ha expirado por inactividad. Por favor inicie sesion nuevamente", ex);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            log.debug("JWT invalido: {}", ex.getMessage());
            throw new BadCredentialsException("Token invalido", ex);
        }
    }

    private Algorithm buildAlgorithm() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad docurural.security.jwt.secret no esta configurada");
        }
        return Algorithm.HMAC256(secret);
    }

    /**
     * Claims relevantes extraidos de un JWT valido.
     */
    @Getter
    public static final class ParsedJwt {
        private final Long userId;
        private final String email;
        private final UserRole role;

        public ParsedJwt(Long userId, String email, UserRole role) {
            this.userId = userId;
            this.email = email;
            this.role = role;
        }
    }
}
