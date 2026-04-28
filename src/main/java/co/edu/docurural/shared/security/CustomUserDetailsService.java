package co.edu.docurural.shared.security;

import co.edu.docurural.shared.domain.entity.User;
import co.edu.docurural.shared.domain.enums.UserStatus;
import co.edu.docurural.shared.domain.repository.UserRepository;
import co.edu.docurural.shared.util.MessageResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga usuarios por email para el flujo de autenticación con
 * {@code DaoAuthenticationProvider}.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si no existe el correo se lanza {@link UsernameNotFoundException}, que el
 *       proveedor convierte en {@code BadCredentialsException} al enmascarar la
 *       diferencia entre "usuario no existe" y "clave inválida" (comportamiento
 *       por defecto de Spring Security con {@code hideUserNotFoundExceptions=true}).</li>
 *   <li>Si la cuenta está {@link UserStatus#INACTIVE} se lanza
 *       {@link DisabledException} con el mensaje en español exacto del requerimiento
 *       AUTH-01, para que el {@code GlobalExceptionHandler} responda con 403.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MessageResolver messageResolver;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Intento de login con email inexistente: {}", email);
                    return new UsernameNotFoundException("User not found");
                });

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException(messageResolver.get("auth.login.account-disabled"));
        }

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPasswordHash());
    }
}
