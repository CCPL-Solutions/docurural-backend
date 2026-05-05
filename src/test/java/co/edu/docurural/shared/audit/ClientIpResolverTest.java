package co.edu.docurural.shared.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void from_withNullRequest_returnsNull() {
        assertThat(ClientIpResolver.from(null)).isNull();
    }

    @Test
    void from_withXForwardedForSingleIp_usesHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");

        String ip = ClientIpResolver.from(request);

        assertThat(ip).isEqualTo("203.0.113.42");
        verify(request, never()).getRemoteAddr();
    }

    @Test
    void from_withXForwardedForCommaSeparated_usesFirst() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42, 10.0.0.1, 10.0.0.2");

        String ip = ClientIpResolver.from(request);

        assertThat(ip).isEqualTo("203.0.113.42");
    }

    @Test
    void from_withBlankXForwardedFor_fallsBackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        String ip = ClientIpResolver.from(request);

        assertThat(ip).isEqualTo("192.168.1.50");
    }

    @Test
    void from_withIpLongerThan45chars_truncates() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String longIp = "a".repeat(60);
        when(request.getHeader("X-Forwarded-For")).thenReturn(longIp);

        String ip = ClientIpResolver.from(request);

        assertThat(ip)
                .hasSize(45)
                .isEqualTo(longIp.substring(0, 45));
    }
}

