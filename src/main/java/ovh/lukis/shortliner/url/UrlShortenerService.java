package ovh.lukis.shortliner.url;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
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

        // Ensure the URL has a proper scheme (http or https)
        String formattedUrl = formatUrl(originalUrl);

        // Validate the formatted URL
        if (!isValidUrl(formattedUrl)) {
            logger.warn("Invalid URL provided: {}", originalUrl);
            throw new IllegalArgumentException("Nieprawidłowy adres URL");
        }

        // Fetch all records with the same URL (to avoid NonUniqueResultException)
        List<UrlEntity> existingUrls = urlRepository.findByUrl(formattedUrl);
        if (!existingUrls.isEmpty()) {
            UrlEntity existingUrl = existingUrls.getFirst(); // Get the first available URL
            logger.info("URL already exists. Returning existing short code: {}", existingUrl.getShortCode());
            return existingUrl;
        }

        // Generate a unique short code
        String shortCode = UUID.randomUUID().toString().substring(0, 6);

        // Create new URL entity
        UrlEntity url = new UrlEntity();
        url.setUrl(formattedUrl);
        url.setShortCode(shortCode);
        url.setCreatedAt(LocalDateTime.now());
        url.setUpdatedAt(LocalDateTime.now());

        // Save to database
        UrlEntity savedUrl = urlRepository.save(url);
        logger.info("New URL shortened successfully: {} -> {}", formattedUrl, shortCode);
        return savedUrl;
    }

    /**
     * Ensures the URL starts with a proper scheme (http or https).
     */
    private String formatUrl(String url) {
        if (!url.matches("^(https?://).*")) {  // If URL doesn't start with http:// or https://
            return "https://" + url;  // Default to https
        }
        return url;
    }

    /**
     * Validates if a given string is a proper URL.
     */
    private boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);  // This will throw an exception if the URL is invalid
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }


    public Optional<UrlEntity> getOriginalUrl(String shortCode) {
        logger.info("Fetching original URL for short code: {}", shortCode);
        return urlRepository.findByShortCode(shortCode);
    }
}