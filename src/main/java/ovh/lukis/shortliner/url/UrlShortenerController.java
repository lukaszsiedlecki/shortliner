package ovh.lukis.shortliner.url;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ovh.lukis.shortliner.kafka.ClickEvent;
import ovh.lukis.shortliner.kafka.ClickEventProducer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/shorten")
@AllArgsConstructor
class UrlShortenerController {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);
    private final UrlShortenerService urlShortenerService;
    private final ClickEventProducer clickEventProducer;
    private final MeterRegistry meterRegistry;

    @PostMapping
    public ResponseEntity<?> shortenUrl(@RequestBody ShortenRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        logger.info("Received request to shorten URL: {}", request.url);
        // Anonymous callers are allowed; their links have no owner.
        String ownerId = jwt != null ? jwt.getSubject() : null;
        try {
            UrlEntity shortenedUrl = urlShortenerService.shortenUrl(request.url, ownerId);
            logger.info("Responding with shortened URL: {}", shortenedUrl.getShortCode());
            return ResponseEntity.ok(new ShortenResponse(shortenedUrl));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL provided: {}", request.url);
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect URL"));
        }
    }

    @GetMapping
    public List<ShortenResponse> listOwnUrls(@AuthenticationPrincipal Jwt jwt) {
        return urlShortenerService.getUrlsOwnedBy(jwt.getSubject()).stream()
                .map(ShortenResponse::new)
                .toList();
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable(name = "shortCode") String shortCode,
                                          @AuthenticationPrincipal Jwt jwt,
                                          Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
        return urlShortenerService.deleteUrl(shortCode, jwt.getSubject(), isAdmin)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{shortCode}")
    public RedirectView getOriginalUrl(@PathVariable(name = "shortCode") String shortCode,
                                       HttpServletRequest request) {
        logger.info("Received GET request for short code: {}", shortCode);
        Optional<UrlEntity> urlOptional = urlShortenerService.getOriginalUrl(shortCode);

        if (urlOptional.isPresent()) {
            UrlEntity url = urlOptional.get();
            String originalUrl = url.getUrl();

            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "http://" + originalUrl;
            }

            // userId is the link's owner (analytics aggregates per owner), not the visitor.
            ClickEvent event = ClickEvent.create(
                    shortCode,
                    url.getOwnerId(),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    request.getHeader("Referer")
            );
            clickEventProducer.sendClickEvent(event);

            logger.info("Short code found: {} -> {}", shortCode, originalUrl);
            countRedirect("hit");
            return new RedirectView(originalUrl);
        } else {
            logger.warn("Short code not found: {}", shortCode);
            countRedirect("not_found");
            return new RedirectView("/error");
        }
    }

    private void countRedirect(String result) {
        Counter.builder("shortliner.redirects")
                .description("Outcomes of short code redirect lookups")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static class ShortenRequest {
        public String url;
    }

    static class ShortenResponse {
        public Long id;
        public String url;
        public String shortCode;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        public ShortenResponse(UrlEntity url) {
            this.id = url.getId();
            this.url = url.getUrl();
            this.shortCode = url.getShortCode();
            this.createdAt = url.getCreatedAt();
            this.updatedAt = url.getUpdatedAt();
        }
    }
}
