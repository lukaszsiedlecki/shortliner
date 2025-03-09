package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UrlShortenerService {
    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    private UrlRepository urlRepository;

    @Transactional
    @Retryable(value = DataIntegrityViolationException.class, maxAttempts = 3)
    public UrlEntity shortenUrl(String originalUrl) {
        logger.info("Received request to shorten URL: {}", originalUrl);

        // Validate the formatted URL
        if (!isValidURL(originalUrl)) {
            logger.warn("Invalid URL provided: {}", originalUrl);
            throw new IllegalArgumentException("Nieprawidłowy adres URL");
        }

        // Fetch all records with the same URL (to avoid NonUniqueResultException)
        List<UrlEntity> existingUrls = urlRepository.findByUrl(originalUrl);
        if (!existingUrls.isEmpty()) {
            UrlEntity existingUrl = existingUrls.getFirst();
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
        try {
            UrlEntity savedUrl = urlRepository.save(url);
            logger.info("New URL shortened successfully: {} -> {}", originalUrl, shortCode);
            return savedUrl;
        } catch (DataIntegrityViolationException e) {
            logger.warn("Concurrent save attempt detected for URL: {}", originalUrl);
            throw e;
        }
    }

    /**
     * Validates if a given string is a proper URL.
     */
    boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    @Cacheable(value = "urls", key = "#shortCode", unless = "#result == null")
    public Optional<UrlEntity> getOriginalUrl(String shortCode) {
        logger.info("Fetching original URL for short code: {}", shortCode);
        return urlRepository.findByShortCode(shortCode);
    }
}