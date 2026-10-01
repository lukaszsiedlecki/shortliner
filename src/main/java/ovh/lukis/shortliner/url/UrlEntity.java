package ovh.lukis.shortliner.url;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(name = "idx_url_entity_owner_id", columnList = "owner_id"))
@Data
@NoArgsConstructor
public class UrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    @Column(unique = true)
    private String shortCode;

    /** Keycloak user ID ({@code sub}) of the creator; {@code null} for links created anonymously. */
    @Column(name = "owner_id", length = 36)
    private String ownerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}