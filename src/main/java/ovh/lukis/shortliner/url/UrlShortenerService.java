package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
class UrlShortenerService {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    private UrlRepository urlRepository;

    public UrlEntity shortenUrl(String originalUrl) {
        logger.info("Received request to shorten URL: {}", originalUrl);

        // Fetch all records with the same URL (to avoid NonUniqueResultException)
        List<UrlEntity> existingUrls = urlRepository.findByUrl(originalUrl);
        if (!existingUrls.isEmpty()) {
            UrlEntity existingUrl = existingUrls.getFirst(); // Get the first available URL
            logger.info("URL already exists. Returning existing short code: {}", existingUrl.getShortCode());
            return existingUrl;
        }

        // Generate a unique short code
        String shortCode = UUID.randomUUID().toString().substring(0, 6);

        // Create new URL entity
        UrlEntity url = new UrlEntity();
        url.setUrl(originalUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setUpdatedAt(LocalDateTime.now());

        // Save to database
        UrlEntity savedUrl = urlRepository.save(url);
        logger.info("New URL shortened successfully: {} -> {}", originalUrl, shortCode);
        return savedUrl;
    }

    public Optional<UrlEntity> getOriginalUrl(String shortCode) {
        logger.info("Fetching original URL for short code: {}", shortCode);
        return urlRepository.findByShortCode(shortCode);
    }
}