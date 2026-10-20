# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run (requires env vars — see below)
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "ovh.lukis.shortliner.url.UrlShortenerApplicationTests"

# Build Docker image
./gradlew bootBuildImage

# GraalVM native image (optional)
./gradlew nativeCompile
```

## Environment variables

The app requires these variables (put them in a `.env` file):

```
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shortliner
DB_USERNAME=your_user
DB_PASSWORD=your_password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KEYCLOAK_JWK_SET_URI=http://keycloak.local/realms/shortliner/protocol/openid-connect/certs  # in-cluster: http://keycloak.keycloak.svc.cluster.local/...
KEYCLOAK_ISSUER_URI=http://keycloak.local/realms/shortliner
```

`dev` (`application-dev.properties`) holds the real config and is always active — `application.properties` hardcodes `spring.profiles.active=dev`, and the cluster runs it too. Local-only noise (Spring Security DEBUG, `show-sql`) lives in the `local` profile: run with `SPRING_PROFILES_ACTIVE=dev,local`. Docker Compose integration is disabled via `application.properties`.

## Architecture

The application is a URL shortener backed by PostgreSQL. The main flow:

Auth: the browser talks only to `shortliner-gateway` (BFF), which forwards `/api/shortliner/**` here with the prefix stripped and an `Authorization: Bearer <Keycloak JWT>` header when the user is logged in (no header when anonymous). This service is an OAuth2 resource server and does its own authorization; the user ID is always `jwt.getSubject()`. The old `shortliner-auth` service is not used.

1. **POST /shorten** (anonymous allowed) — validates the URL, checks for duplicates in PostgreSQL (per owner), generates a 6-character UUID-prefix short code, saves via JPA with `ownerId = sub` when a JWT is present (else `null`), returns the entity. `short_code` is the only unique key, so `@Retryable` — Spring Framework 7's `org.springframework.resilience` one (`@EnableResilientMethods`), not spring-retry; `maxRetries = 2` (3 attempts), `delay = 0` — just regenerates the code on a collision; there is no unique key on `(url, owner_id)`, so concurrent requests for the same URL can both insert — dedup is best-effort. Each failed attempt counts `result=conflict`, so one request can increment the counter more than once.
2. **GET /shorten/{shortCode}** (anonymous) — looks up the short code (Caffeine cache, then DB), fires an async Kafka click event to topic `shortliner.clicks` with `userId` = the link's **owner** (what analytics aggregates by, not the visitor), and issues a 302 redirect.
3. **GET /shorten** (authenticated) — the caller's own links.
4. **DELETE /shorten/{shortCode}** (authenticated) — owner or `admin` only; 204, or 404 for missing *and* foreign links (existence isn't revealed). Evicts the `urls` cache entry.

Key classes:
- `UrlShortenerService` — core logic: URL validation, dedup check, `@Cacheable("urls")` lookup
- `UrlShortenerController` — REST layer
- `ClickEventProducer` / `ClickEvent` — fire-and-forget Kafka producer for analytics
- `SecurityConfig` — the single (profile-independent) security config: stateless JWT resource server, CSRF off, no CORS (same-origin via the gateway). `JwtDecoder` uses the JWKS URI and validates `iss` explicitly — never set `issuer-uri`, since `keycloak.local` doesn't resolve from pods. Keycloak `realm_access.roles` map to `ROLE_<name>`. Actuator health/prometheus are public, other actuator endpoints need `admin`; unmatched paths are `permitAll` so unknown routes stay 404.
- `UrlEntity.ownerId` — `owner_id VARCHAR(36)`, nullable, indexed
- Schema is owned by **Flyway** (`src/main/resources/db/migration`, `V<n>__*.sql`); Hibernate runs with `ddl-auto=validate`, so every entity change needs a new migration. `V1` reproduces the schema Hibernate generated before Flyway; pre-Flyway databases are baselined at version 1 (`spring.flyway.baseline-on-migrate=true`) and only receive `V2+`. Never edit an applied migration.
- `CacheConfig` — Caffeine cache named `urls`, max 10 000 entries, 1-hour TTL

## Spring Boot 4 notes

Boot 4.1 is modular: auto-configuration only kicks in when the technology's **starter** is on the classpath (`spring-boot-starter-flyway`, `-kafka`, `-opentelemetry`, …). A bare library such as `flyway-core` silently gets no auto-configuration — with Flyway that means migrations stop running without any error. Test slices need their test starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`). JSON is Jackson 3 (`tools.jackson`); `ClickEventProducer` gets the auto-configured `JsonMapper` injected. The Gradle wrapper must stay ≥ 9.8 — older Gradle can't compile the build script with the Boot 4 plugin when running on JDK 27.

## Testing

Tests use H2 in-memory DB with the schema built by the Flyway migrations (`ddl-auto=validate`, so a migration/entity mismatch fails the build) and disable Kafka autoconfiguration entirely (`application.properties` in `src/test/resources`). They also set `management.opentelemetry.map-environment-variables=false` so ambient `OTEL_*` variables can't configure exporters — BuildKit's docker-container driver (used in CI) injects `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=unix:///dev/otel-grpc.sock` into `RUN` steps, which otherwise fails every context in the Docker build. The test `application.properties` shadows the main one, so the `dev` profile is not active in tests and resource-server/actuator properties are repeated there. The `ClickEventProducer` is replaced with a `@TestConfiguration` bean that records sent events. Authenticated calls use `spring-security-test`'s `jwt()` post-processor (via `spring-boot-starter-security-test`) — no Keycloak needed.

## Observability

- **Metrics**: Micrometer + `micrometer-registry-prometheus`, scraped at `/actuator/prometheus` (always on; `management.endpoints.web.exposure.include=health,prometheus,metrics`). Includes HTTP latency histograms (`http_server_requests`) and business counters `shortliner_links_created_total{result=success|duplicate|validation_error|conflict}`, `shortliner_redirects_total{result=hit|not_found}` and `shortliner_click_events_published_total{result=success|failure}`, incremented via injected `MeterRegistry` in `UrlShortenerService`/`UrlShortenerController`/`ClickEventProducer`. Never tag with short codes or URLs. Requests rejected by Spring Security with 401 carry `uri="UNKNOWN"` (Security 7 matches paths without consulting Spring MVC), so they can't be broken down by path. Cache hit ratio comes from `cache_gets_total{cache="urls"}`; Kafka from `spring_kafka_template_*` and `kafka_producer_*` (both appear only after the first send). `spring.application.name=shortliner` drives the `application` metric tag, the ECS `service.name`, and the OTel service name.
- **Tracing**: Micrometer Tracing with the OTel bridge + OTLP exporter (`spring-boot-starter-opentelemetry`), plus Kafka producer trace-context propagation (`spring.kafka.template.observation-enabled=true`). Export is **disabled by default** and configured with the **standard OTel env vars**, which Boot 4.1 maps onto `management.*` properties with precedence over `application.properties`: set `OTEL_TRACES_EXPORTER=otlp` and `OTEL_EXPORTER_OTLP_ENDPOINT` to the collector's *base* URL (Boot appends `/v1/traces`) once a collector (e.g. Tempo/Jaeger) is available. OTLP metrics push and OTLP log export are switched off in `application.properties` (`management.otlp.metrics.export.enabled`, `management.logging.export.otlp.enabled`) — Prometheus and Loki cover those; without that, the OTLP registry the starter pulls in pushes to `localhost:4318` every minute.
- **Logging**: trace/span IDs are added to the MDC automatically once tracing is on the classpath, regardless of export status. Structured JSON console logs are opt-in via `LOGGING_STRUCTURED_FORMAT_CONSOLE` (e.g. `logstash`, `ecs`) — unset locally for readable output, set to `logstash` in the k8s Deployment.
