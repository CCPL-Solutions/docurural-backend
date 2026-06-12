package co.edu.docurural.shared.security;

import co.edu.docurural.user.entity.User;
import co.edu.docurural.user.enums.UserRole;
import co.edu.docurural.user.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Principal de seguridad usado tanto por el login con {@code DaoAuthenticationProvider}
 * como por el filtro JWT.
 *
 * <p>Envuelve los datos mínimos ({@code id}, {@code email}, {@code role}, {@code status})
 * necesarios para construir el {@code Authentication} en el contexto de Spring Security
 * sin acoplar las capas superiores a la entidad JPA {@code User}.
 *
 * <p>Las autoridades se exponen como {@code ROLE_<rol>} (prefijo requerido por Spring
 * Security para {@code hasRole(...)}).
 *
 * <p>{@link #getPassword()} devuelve el hash BCrypt cuando el principal se construye
 * durante el flujo de login (para que {@code DaoAuthenticationProvider} compare contra
 * la contraseña enviada); en el flujo JWT el hash no está disponible y se devuelve
 * {@code null}.
 */
@Getter
public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final String passwordHash;

    private CustomUserPrincipal(Long id, String email, UserRole role, UserStatus status, String passwordHash) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    /**
     * Para el flujo de login ({@code DaoAuthenticationProvider}): incluye el hash BCrypt
     * para que el proveedor pueda comparar la contraseña enviada.
     */
    public static CustomUserPrincipal forAuthentication(User user) {
        return new CustomUserPrincipal(
                user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getPasswordHash());
    }

    /**
     * Para el filtro JWT: construye el principal desde la entidad recargada de BD.
     * No necesita el hash porque la autenticación ya fue validada criptográficamente.
     */
    public static CustomUserPrincipal fromEntity(User user) {
        return new CustomUserPrincipal(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), null);
    }

    /**
     * Construye un principal mínimo desde claims JWT cuando la entidad completa no está disponible.
     * Útil para tests o contextos donde solo se tiene id, email y rol.
     */
    public static CustomUserPrincipal fromJwtClaims(Long id, String email, UserRole role) {
        return new CustomUserPrincipal(id, email, role, UserStatus.ACTIVE, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
