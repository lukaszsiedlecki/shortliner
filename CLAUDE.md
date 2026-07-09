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
```

One Spring profile exists: `dev` (`application-dev.properties`), and it's always active — `application.properties` hardcodes `spring.profiles.active=dev`. Docker Compose integration is disabled via `application.properties`.

## Architecture

The application is a URL shortener backed by PostgreSQL. The main flow:

1. **POST /shorten** — validates the URL, checks for duplicates in PostgreSQL, generates a 6-character UUID-prefix short code, saves via JPA, returns the entity. `@Retryable` handles concurrent duplicate-key violations.
2. **GET /shorten/{shortCode}** (public) — looks up the short code (Caffeine cache, then DB), fires an async Kafka click event to topic `shortliner.clicks`, and issues a 302 redirect.

Key classes:
- `UrlShortenerService` — core logic: URL validation, dedup check, `@Cacheable("urls")` lookup
- `UrlShortenerController` — REST layer
- `ClickEventProducer` / `ClickEvent` — fire-and-forget Kafka producer for analytics
- `DevSecurityConfig` (`@Profile("!prd")`) — permits all requests; this is the config actually in effect, since `spring.profiles.active` is hardcoded to `dev`
- `SecurityConfig` (`@Profile("prd")`) — stricter authorization rules (only GET /shorten/{shortCode} and health/error are public) but currently dead code — no authentication mechanism (e.g. JWT) is wired up yet, and no `prd` profile activation path exists
- `CacheConfig` — Caffeine cache named `urls`, max 10 000 entries, 1-hour TTL

## Testing

Tests use H2 in-memory DB and disable Kafka autoconfiguration entirely (`application.properties` in `src/test/resources`). The `ClickEventProducer` is replaced with a no-op `@TestConfiguration` bean. No authentication simulation is needed — `DevSecurityConfig` permits all requests in the (always-active) `dev` profile.

## Observability

- **Metrics**: Micrometer + `micrometer-registry-prometheus`, scraped at `/actuator/prometheus` (always on; `management.endpoints.web.exposure.include=health,prometheus,metrics`). Includes HTTP latency histograms (`http_server_requests`) and business counters `shortliner_url_shortened_total{outcome=created|duplicate|invalid}` and `shortliner_url_redirect_total{outcome=found|not_found}`, incremented via injected `MeterRegistry` in `UrlShortenerService`/`UrlShortenerController`.
- **Tracing**: Micrometer Tracing with the OTel bridge + OTLP exporter, plus Kafka producer trace-context propagation (`spring.kafka.template.observation-enabled=true`). Export is **disabled by default** — set `OTEL_TRACING_EXPORT_ENABLED=true` and `OTEL_EXPORTER_OTLP_ENDPOINT` once a collector (e.g. Tempo/Jaeger) is available.
- **Logging**: trace/span IDs are added to the MDC automatically once tracing is on the classpath, regardless of export status. Structured JSON console logs are opt-in via `LOGGING_STRUCTURED_FORMAT_CONSOLE` (e.g. `logstash`, `ecs`) — unset locally for readable output, set to `logstash` in the k8s Deployment.
