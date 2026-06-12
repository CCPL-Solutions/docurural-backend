package co.edu.docurural.shared.config;

import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Seed del usuario administrador inicial.
 *
 * <p>Se ejecuta tras el arranque del contexto Spring y crea (de forma idempotente)
 * la cuenta administradora a partir de las variables de entorno
 * {@code ADMIN_SEED_EMAIL} y {@code ADMIN_SEED_PASSWORD}.
 *
 * <p><b>Seguridad:</b> la contraseña se hashea con el bean {@link PasswordEncoder}
 * del contexto antes de persistirla. La clave en claro nunca se loggea.
 */
@Component
public class InitialAdminSeederConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminSeederConfig.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public InitialAdminSeederConfig(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${docurural.seed.admin.email:}") String adminEmail,
            @Value("${docurural.seed.admin.password:}") String adminPassword,
            @Value("${docurural.seed.admin.full-name:Administrador}") String adminFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        String normalizedEmail = adminEmail.toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Administrador inicial ya existe (email={}); no se inserta.", normalizedEmail);
            return;
        }

        User admin = User.builder()
                .fullName(adminFullName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Administrador inicial creado (email={}).", normalizedEmail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
