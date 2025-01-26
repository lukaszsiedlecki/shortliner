package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/shorten")
@AllArgsConstructor
class UrlShortenerController {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);
    private UrlShortenerService urlShortenerService;

    @PostMapping
    public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request) {
        logger.info("Received POST request to shorten URL: {}", request.url);
        UrlEntity shortenedUrl = urlShortenerService.shortenUrl(request.url);
        logger.info("Responding with shortened URL: {}", shortenedUrl.getShortCode());
        return ResponseEntity.ok(new ShortenResponse(shortenedUrl));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<ShortenResponse> getOriginalUrl(@PathVariable(name = "shortCode") String shortCode) {
        logger.info("Received GET request for short code: {}", shortCode);
        Optional<UrlEntity> urlOptional = urlShortenerService.getOriginalUrl(shortCode);
        if (urlOptional.isPresent()) {
            logger.info("Short code found: {} -> {}", shortCode, urlOptional.get().getUrl());
            return ResponseEntity.ok(new ShortenResponse(urlOptional.get()));
        } else {
            logger.warn("Short code not found: {}", shortCode);
            return ResponseEntity.notFound().build();
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
