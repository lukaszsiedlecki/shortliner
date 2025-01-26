package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
class UrlShortenerService {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    private UrlRepository urlRepository;

    public UrlEntity shortenUrl(String originalUrl) {
        logger.info("Received request to shorten URL: {}", originalUrl);
        // Generate a unique short code
        String shortCode = UUID.randomUUID().toString().substring(0, 6);

        // Create URL entity
        UrlEntity url = new UrlEntity();
        url.setUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setUpdatedAt(LocalDateTime.now());

        // Save to database
        UrlEntity save = urlRepository.save(url);
        logger.info("URL shortened successfully: {} -> {}", originalUrl, shortCode);
        return save;
    }

    public Optional<UrlEntity> getOriginalUrl(String shortCode) {
        logger.info("Fetching original URL for short code: {}", shortCode);
        return urlRepository.findByShortCode(shortCode);
    }
}