package co.edu.docurural.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed del usuario administrador inicial.
 *
 * <p>Se ejecuta tras el arranque del contexto Spring y crea (de forma idempotente)
 * la cuenta administradora a partir de las variables de entorno
 * {@code ADMIN_SEED_EMAIL} y {@code ADMIN_SEED_PASSWORD}.
 *
 * <p><b>Por qué JdbcTemplate y no UserRepository:</b> en la Fase 1 del Sprint 1
 * todavía no existen las entidades JPA ni los repositorios. Acoplar el seed a la
 * Fase 2 rompería la separación del plan, por lo que aquí se usa JDBC directo
 * contra la tabla {@code users} ya creada por la migración {@code V1}.
 *
 * <p><b>Seguridad:</b> la contraseña se hashea con {@link BCryptPasswordEncoder}
 * antes de persistirla. La clave en claro nunca se loggea.
 */
@Component
public class InitialAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSeeder.class);

    private static final String COUNT_BY_EMAIL_SQL =
            "SELECT COUNT(*) FROM users WHERE email = ?";

    private static final String INSERT_ADMIN_SQL =
            "INSERT INTO users (full_name, email, password_hash, role, status) "
                    + "VALUES (?, ?, ?, 'ADMIN', 'ACTIVE')";

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public InitialAdminSeeder(
            JdbcTemplate jdbcTemplate,
            @Value("${docurural.seed.admin.email:}") String adminEmail,
            @Value("${docurural.seed.admin.password:}") String adminPassword,
            @Value("${docurural.seed.admin.full-name:Administrador}") String adminFullName) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
    }

    @Override
    public void run(String... args) {
        if (isBlank(adminEmail) || isBlank(adminPassword)) {
            log.warn("Variables ADMIN_SEED_EMAIL/ADMIN_SEED_PASSWORD no definidas; "
                    + "se omite seed de administrador inicial.");
            return;
        }

        Long existingCount = jdbcTemplate.queryForObject(
                COUNT_BY_EMAIL_SQL, Long.class, adminEmail);

        if (existingCount != null && existingCount > 0) {
            log.info("Administrador inicial ya existe (email={}); no se inserta.", adminEmail);
            return;
        }

        String passwordHash = passwordEncoder.encode(adminPassword);
        jdbcTemplate.update(INSERT_ADMIN_SQL, adminFullName, adminEmail, passwordHash);

        log.info("Administrador inicial creado (email={}).", adminEmail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
