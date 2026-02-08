package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/shorten")
@AllArgsConstructor
class UrlShortenerController {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);
    private UrlShortenerService urlShortenerService;

    @PostMapping
    public ResponseEntity<?> shortenUrl(@RequestBody ShortenRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        logger.info("User [{}] requested to shorten URL: {}", userId, request.url);
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
    public RedirectView getOriginalUrl(@PathVariable(name = "shortCode") String shortCode) {
        logger.info("Received GET request for short code: {}", shortCode);
        Optional<UrlEntity> urlOptional = urlShortenerService.getOriginalUrl(shortCode);

        if (urlOptional.isPresent()) {
            String originalUrl = urlOptional.get().getUrl();

            // Ensure URL is absolute (if missing, prepend "http://")
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "http://" + originalUrl; // Default to HTTP
            }

            logger.info("Short code found: {} -> {}", shortCode, originalUrl);
            return new RedirectView(originalUrl);
        } else {
            logger.warn("Short code not found: {}", shortCode);
            return new RedirectView("/error");
        }
    }


    // DTO class for request
    static class ShortenRequest {
        public String url;
    }

    // DTO class for response
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
