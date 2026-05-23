package co.edu.docurural.shared.security;

import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserStatus;
import co.edu.docurural.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *   <li>Si la cuenta está {@link UserStatus#INACTIVE} el principal devuelto reporta
 *       {@code isEnabled() == false}; el {@code DaoAuthenticationProvider} ejecuta
 *       sus {@code preAuthenticationChecks} después de esta carga y lanza
 *       {@code DisabledException} fuera del envoltorio interno que convertiría
 *       cualquier excepción lanzada aquí en {@code InternalAuthenticationServiceException}.
 *       Así la excepción llega intacta al {@code GlobalExceptionHandler} que la
 *       traduce a 403 con el mensaje exacto del requerimiento AUTH-01.</li>
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
                    return new UsernameNotFoundException("User not found");
                });

        return CustomUserPrincipal.forAuthentication(user);
    }
}
