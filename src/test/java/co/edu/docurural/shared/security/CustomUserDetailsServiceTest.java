package co.edu.docurural.shared.security;

import co.edu.docurural.user.domain.entity.User;
import co.edu.docurural.user.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_existingActiveUser_returnsPrincipalWithCorrectAuthorities() {
        User editor = TestFixtures.userEditor(2L);
        when(userRepository.findByEmail(editor.getEmail())).thenReturn(Optional.of(editor));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(editor.getEmail());

        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EDITOR");

        verify(userRepository).findByEmail(editor.getEmail());
    }

    @Test
    void loadUserByUsername_missingEmail_throwsUsernameNotFound() {
        String missingEmail = "ghost@docurural.edu.co";
        when(userRepository.findByEmail(missingEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(missingEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmail(missingEmail);
    }

    @Test
    void loadUserByUsername_inactiveUser_returnsPrincipalWithIsEnabledFalse() {
        User inactive = TestFixtures.userInactive(3L);
        when(userRepository.findByEmail(inactive.getEmail())).thenReturn(Optional.of(inactive));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(inactive.getEmail());

        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        assertThat(userDetails.isEnabled()).isFalse();
        verify(userRepository).findByEmail(inactive.getEmail());
    }
}
