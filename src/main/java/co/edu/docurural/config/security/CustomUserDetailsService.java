package co.edu.docurural.config.security;

import co.edu.docurural.entity.User;
import co.edu.docurural.enums.UserStatus;
import co.edu.docurural.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga usuarios por email para el flujo de autenticacion con
 * {@code DaoAuthenticationProvider}.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si no existe el correo se lanza {@link UsernameNotFoundException}, que el
 *       proveedor convierte en {@code BadCredentialsException} al enmascarar la
 *       diferencia entre "usuario no existe" y "clave invalida" (comportamiento
 *       por defecto de Spring Security con {@code hideUserNotFoundExceptions=true}).</li>
 *   <li>Si la cuenta esta {@link UserStatus#INACTIVE} se lanza
 *       {@link DisabledException} con el mensaje en espanol exacto del requerimiento
 *       AUTH-01, para que el {@code GlobalExceptionHandler} responda con 403.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Intento de login con email inexistente: {}", email);
                    return new UsernameNotFoundException("Correo o contrasena incorrectos");
                });

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException(
                    "Su cuenta ha sido desactivada. Contacte al administrador");
        }

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPasswordHash());
    }
}
