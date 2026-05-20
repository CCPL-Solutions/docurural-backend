package co.edu.docurural.shared.security;

import co.edu.docurural.user.domain.entity.User;
import co.edu.docurural.user.domain.enums.UserRole;
import co.edu.docurural.shared.util.MessageResolver;
import co.edu.docurural.support.TestFixtures;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-32chars-minimum-len";
    private static final String ISSUER = "docurural";
    private static final long EXPIRATION_MS = 1_800_000L;

    @Mock
    JwtProperties jwtProperties;
    @Mock
    MessageResolver messageResolver;

    @InjectMocks
    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messageResolver.get(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generateToken_embedsSubjectEmailRoleIssuerIatExp() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        when(jwtProperties.getExpirationMs()).thenReturn(EXPIRATION_MS);

        User admin = TestFixtures.userAdmin(1L);

        String token = jwtTokenProvider.generateToken(admin);

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo("1");
        assertThat(decoded.getClaim("email").asString()).isEqualTo("ana.admin@docurural.edu.co");
        assertThat(decoded.getClaim("role").asString()).isEqualTo("ADMIN");
        assertThat(decoded.getClaim("tokver").asInt()).isEqualTo(0);
        assertThat(decoded.getIssuer()).isEqualTo(ISSUER);
        assertThat(decoded.getIssuedAt()).isNotNull();
        assertThat(decoded.getExpiresAt()).isNotNull();
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());

        verify(jwtProperties, atLeastOnce()).getExpirationMs();
        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verifyNoInteractions(messageResolver);
    }

    @Test
    void parseAndValidate_withValidToken_returnsParsedClaims() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        when(jwtProperties.getExpirationMs()).thenReturn(EXPIRATION_MS);

        User editor = TestFixtures.userEditor(2L);
        String token = jwtTokenProvider.generateToken(editor);

        JwtTokenProvider.ParsedJwt parsed = jwtTokenProvider.parseAndValidate(token);

        assertThat(parsed.getUserId()).isEqualTo(2L);
        assertThat(parsed.getEmail()).isEqualTo("erik.editor@docurural.edu.co");
        assertThat(parsed.getRole()).isEqualTo(UserRole.EDITOR);
        assertThat(parsed.getTokenVersion()).isEqualTo(0);

        verify(jwtProperties, atLeastOnce()).getExpirationMs();
        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verifyNoInteractions(messageResolver);
    }

    @Test
    void parseAndValidate_withExpiredToken_throwsCredentialsExpired() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        when(jwtProperties.getExpirationMs()).thenReturn(-1000L);

        User admin = TestFixtures.userAdmin(1L);
        String expiredToken = jwtTokenProvider.generateToken(admin);

        assertThatThrownBy(() -> jwtTokenProvider.parseAndValidate(expiredToken))
                .isInstanceOf(CredentialsExpiredException.class);

        verify(jwtProperties, atLeastOnce()).getExpirationMs();
        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verify(messageResolver).get(anyString());
    }

    @Test
    void parseAndValidate_withInvalidSignature_throwsBadCredentials() {
        String secretA = "secret-A-32chars-minimum-length!!";
        String secretB = "secret-B-32chars-minimum-length!!";

        Instant now = Instant.now();
        String tokenSignedWithA = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("1")
                .withClaim("email", "ana.admin@docurural.edu.co")
                .withClaim("role", "ADMIN")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(EXPIRATION_MS)))
                .sign(Algorithm.HMAC256(secretA));

        when(jwtProperties.getSecret()).thenReturn(secretB);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);

        assertThatThrownBy(() -> jwtTokenProvider.parseAndValidate(tokenSignedWithA))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verify(messageResolver).get(anyString());
    }

    @Test
    void parseAndValidate_withWrongIssuer_throwsBadCredentials() {
        Instant now = Instant.now();
        String tokenWithWrongIssuer = JWT.create()
                .withIssuer("wrong-issuer")
                .withSubject("1")
                .withClaim("email", "ana.admin@docurural.edu.co")
                .withClaim("role", "ADMIN")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(EXPIRATION_MS)))
                .sign(Algorithm.HMAC256(SECRET));

        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);

        assertThatThrownBy(() -> jwtTokenProvider.parseAndValidate(tokenWithWrongIssuer))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verify(messageResolver).get(anyString());
    }

    @Test
    void parseAndValidate_withMalformedToken_throwsBadCredentials() {
        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);

        assertThatThrownBy(() -> jwtTokenProvider.parseAndValidate("not.a.valid.jwt"))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verify(messageResolver).get(anyString());
    }

    @Test
    void parseAndValidate_withNonNumericSubject_throwsBadCredentials() {
        Instant now = Instant.now();
        String tokenWithBadSubject = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("not-a-number")
                .withClaim("email", "ana.admin@docurural.edu.co")
                .withClaim("role", "ADMIN")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(EXPIRATION_MS)))
                .sign(Algorithm.HMAC256(SECRET));

        when(jwtProperties.getSecret()).thenReturn(SECRET);
        when(jwtProperties.getIssuer()).thenReturn(ISSUER);

        assertThatThrownBy(() -> jwtTokenProvider.parseAndValidate(tokenWithBadSubject))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtProperties, atLeastOnce()).getIssuer();
        verify(jwtProperties, atLeastOnce()).getSecret();
        verify(messageResolver).get(anyString());
    }

    @Test
    void generateToken_withBlankSecret_throwsIllegalState() {
        when(jwtProperties.getSecret()).thenReturn("");

        User admin = TestFixtures.userAdmin(1L);

        assertThatThrownBy(() -> jwtTokenProvider.generateToken(admin))
                .isInstanceOf(IllegalStateException.class);

        verify(jwtProperties, atLeastOnce()).getSecret();
        verifyNoInteractions(messageResolver);
    }
}
