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

    public CustomUserPrincipal(Long id, String email, UserRole role, UserStatus status, String passwordHash) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    /**
     * Construye un principal a partir de la entidad {@code User} recargada desde BD.
     * Refleja el estado y rol vigentes en el momento de la petición.
     */
    public static CustomUserPrincipal fromEntity(User user) {
        return new CustomUserPrincipal(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), null);
    }

    /**
     * Construye un principal sin hash de contraseña; usado por el filtro JWT cuando
     * los claims ya fueron validados criptográficamente.
     *
     * @deprecated Usar {@link #fromEntity(User)} en el filtro para reflejar estado real
     * del usuario. Este método permanece para uso en tests.
     */
    @Deprecated
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
