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
DB_HOST=localhost
DB_NAME=shortliner
DB_USER=your_user
DB_PASSWORD=your_password
JWT_ISSUER_URI=http://localhost:8080
JWT_JWK_SET_URI=http://localhost:8080/.well-known/jwks.json
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

One Spring profile exists: `dev` (`application-dev.properties`). Docker Compose integration is disabled via `application.properties`.

## Architecture

The application is a URL shortener backed by PostgreSQL. The main flow:

1. **POST /shorten** (JWT required) — validates the URL, checks for duplicates in PostgreSQL, generates a 6-character UUID-prefix short code, saves via JPA, returns the entity. `@Retryable` handles concurrent duplicate-key violations.
2. **GET /shorten/{shortCode}** (public) — looks up the short code (Caffeine cache, then DB), fires an async Kafka click event to topic `shortliner.clicks`, and issues a 302 redirect.

Key classes:
- `UrlShortenerService` — core logic: URL validation, dedup check, `@Cacheable("urls")` lookup
- `UrlShortenerController` — REST layer; extracts JWT subject for user tracking
- `ClickEventProducer` / `ClickEvent` — fire-and-forget Kafka producer for analytics
- `SecurityConfig` — stateless OAuth2 JWT resource server; only POST /shorten and management endpoints require auth
- `CacheConfig` — Caffeine cache named `urls`, max 10 000 entries, 1-hour TTL

## Testing

Tests use H2 in-memory DB and disable Kafka autoconfiguration entirely (`application.properties` in `src/test/resources`). The `ClickEventProducer` is replaced with a no-op `@TestConfiguration` bean. Use `SecurityMockMvcRequestPostProcessors.jwt()` to simulate authenticated requests.
