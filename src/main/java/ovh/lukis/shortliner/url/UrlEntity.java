package ovh.lukis.shortliner.url;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
class UrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    @Column(unique = true)
    private String shortCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}