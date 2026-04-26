package co.edu.docurural.shared.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve la IP cliente priorizando X-Forwarded-For y usando remoteAddr como fallback.
 */
public final class ClientIpResolver {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final int IP_ADDRESS_MAX_LENGTH = 45;

    private ClientIpResolver() {
    }

    public static String from(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIdx = forwardedFor.indexOf(',');
            String first = (commaIdx >= 0 ? forwardedFor.substring(0, commaIdx) : forwardedFor).trim();
            if (!first.isEmpty()) {
                return truncate(first);
            }
        }

        String remote = request.getRemoteAddr();
        return remote == null ? null : truncate(remote);
    }

    private static String truncate(String value) {
        if (value.length() <= IP_ADDRESS_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, IP_ADDRESS_MAX_LENGTH);
    }
}

