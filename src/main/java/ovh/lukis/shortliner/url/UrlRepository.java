package ovh.lukis.shortliner.url;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByShortCode(String shortCode);

    // A null ownerId is derived to "owner_id IS NULL", so anonymous links dedupe among themselves.
    List<UrlEntity> findByUrlAndOwnerId(String url, String ownerId);

    List<UrlEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
