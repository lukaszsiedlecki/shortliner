package ovh.lukis.shortliner.bucket;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ovh.lukis.shortliner.url.UrlShortenerService;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class BucketReader {
    private static final Logger logger = LoggerFactory.getLogger(BucketReader.class);
    private UrlShortenerService urlShortenerService;

    public List<String> readBucket() {
        List<String> shortLinedUrls = new ArrayList<>();
        Storage storage = StorageOptions.getDefaultInstance().getService();

        Bucket shortLiner = storage.get("shortliner");

        shortLiner.list().iterateAll().forEach(blob -> {
            String shortCode = urlShortenerService.shortenUrl(new String(blob.getContent())).getShortCode();
            if (!shortLinedUrls.contains(shortCode)) {
                shortLinedUrls.add(shortCode);
            }
        });
        logger.info("Read {} URLs from the bucket", shortLinedUrls.size());
        logger.info("Shortened URLs: {}", String.join(",", shortLinedUrls));
        return shortLinedUrls;
    }
}
