package co.edu.docurural.config;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.UserRole;
import co.edu.docurural.domain.enums.enums.UserStatus;
import co.edu.docurural.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed del usuario administrador inicial.
 *
 * <p>Se ejecuta tras el arranque del contexto Spring y crea (de forma idempotente)
 * la cuenta administradora a partir de las variables de entorno
 * {@code ADMIN_SEED_EMAIL} y {@code ADMIN_SEED_PASSWORD}.
 *
 * <p><b>Seguridad:</b> la contraseña se hashea con {@link BCryptPasswordEncoder}
 * antes de persistirla. La clave en claro nunca se loggea.
 */
@Component
public class InitialAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSeeder.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public InitialAdminSeeder(
            UserRepository userRepository,
            @Value("${docurural.seed.admin.email:}") String adminEmail,
            @Value("${docurural.seed.admin.password:}") String adminPassword,
            @Value("${docurural.seed.admin.full-name:Administrador}") String adminFullName) {
        this.userRepository = userRepository;
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

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Administrador inicial ya existe (email={}); no se inserta.", adminEmail);
            return;
        }

        User admin = User.builder()
                .fullName(adminFullName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Administrador inicial creado (email={}).", adminEmail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
