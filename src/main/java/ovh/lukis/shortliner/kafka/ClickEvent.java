package ovh.lukis.shortliner.kafka;

import java.time.Instant;

public record ClickEvent(
        String shortCode,
        String userId,
        String timestamp,
        String ip,
        String userAgent,
        String referrer
) {
    public static ClickEvent create(String shortCode, String userId, String ip, String userAgent, String referrer) {
        return new ClickEvent(
                shortCode,
                userId,
                Instant.now().toString(),
                ip,
                userAgent,
                referrer
        );
    }
}
