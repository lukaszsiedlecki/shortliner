package ovh.lukis.shortliner.url;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByShortCode(String shortCode);

    List<UrlEntity> findByUrl(String url);
}
