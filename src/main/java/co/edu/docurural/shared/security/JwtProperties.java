package co.edu.docurural.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración JWT leída desde {@code application.yaml} bajo el prefijo
 * {@code docurural.security.jwt}.
 *
 * <p>Campos:
 * <ul>
 *   <li>{@code secret} — clave HMAC usada para firmar y verificar los tokens HS256.
 *       Nunca debe ir hardcodeada; siempre viene de variable de entorno.</li>
 *   <li>{@code expirationMs} — duración del token en milisegundos (default 1 800 000 ms = 30 min).</li>
 *   <li>{@code issuer} — valor del claim {@code iss} emitido y validado en los tokens.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "docurural.security.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 1_800_000L;
    private String issuer = "docurural";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
