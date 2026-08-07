package co.edu.docurural.health.dto;

import java.time.Instant;

public record VersionResponseDto(
        String version,
        String commit,
        String branch,
        Instant buildTime) {
}
