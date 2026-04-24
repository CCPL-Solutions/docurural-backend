package co.edu.docurural.config.security;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.repository.UserRepository;
import co.edu.docurural.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    MessageSource messageSource;

    @InjectMocks
    CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void stubMessageSource() {
        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

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
        verifyNoInteractions(messageSource);
    }

    @Test
    void loadUserByUsername_missingEmail_throwsUsernameNotFound() {
        String missingEmail = "ghost@docurural.edu.co";
        when(userRepository.findByEmail(missingEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(missingEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("auth.login.invalid-credentials");

        verify(userRepository).findByEmail(missingEmail);
        verify(messageSource).getMessage(eq("auth.login.invalid-credentials"), isNull(), any());
    }

    @Test
    void loadUserByUsername_inactiveUser_throwsDisabled() {
        User inactive = TestFixtures.userInactive(3L);
        String email = inactive.getEmail();
        when(userRepository.findByEmail(inactive.getEmail())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("auth.login.account-disabled");

        verify(userRepository).findByEmail(inactive.getEmail());
        verify(messageSource).getMessage(eq("auth.login.account-disabled"), isNull(), any());
    }
}


