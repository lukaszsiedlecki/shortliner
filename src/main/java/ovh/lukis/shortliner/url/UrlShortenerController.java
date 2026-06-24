package ovh.lukis.shortliner.url;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import ovh.lukis.shortliner.kafka.ClickEvent;
import ovh.lukis.shortliner.kafka.ClickEventProducer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/shorten")
@AllArgsConstructor
class UrlShortenerController {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);
    private final UrlShortenerService urlShortenerService;
    private final ClickEventProducer clickEventProducer;

    @PostMapping
    public ResponseEntity<?> shortenUrl(@RequestBody ShortenRequest request) {
        logger.info("Received request to shorten URL: {}", request.url);
        try {
            UrlEntity shortenedUrl = urlShortenerService.shortenUrl(request.url);
            logger.info("Responding with shortened URL: {}", shortenedUrl.getShortCode());
            return ResponseEntity.ok(new ShortenResponse(shortenedUrl));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid URL provided: {}", request.url);
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect URL"));
        }
    }

    @GetMapping("/{shortCode}")
    public RedirectView getOriginalUrl(@PathVariable(name = "shortCode") String shortCode,
                                       HttpServletRequest request) {
        logger.info("Received GET request for short code: {}", shortCode);
        Optional<UrlEntity> urlOptional = urlShortenerService.getOriginalUrl(shortCode);

        if (urlOptional.isPresent()) {
            String originalUrl = urlOptional.get().getUrl();

            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "http://" + originalUrl;
            }

            ClickEvent event = ClickEvent.create(
                    shortCode,
                    null,
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    request.getHeader("Referer")
            );
            clickEventProducer.sendClickEvent(event);

            logger.info("Short code found: {} -> {}", shortCode, originalUrl);
            return new RedirectView(originalUrl);
        } else {
            logger.warn("Short code not found: {}", shortCode);
            return new RedirectView("/error");
        }
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
