# ShortLiner - URL Shortener

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

ShortLiner is a modern URL shortening application optimized for high performance and reliability.

## Features

- Shortening long URLs into concise, memorable codes
- Automatic redirects from shortened URLs
- Click event tracking via Kafka
- Caching of frequently used URLs for better performance
- Concurrent request handling with retry mechanism
- URL validation
- Modern user interface
- Prometheus metrics, OpenTelemetry distributed tracing, and structured JSON logging

## Technologies

- Java 25
- Spring Boot 3.5.10
- Spring Security OAuth2 resource server (Keycloak JWTs relayed by `shortliner-gateway`)
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Caffeine Cache
- Thymeleaf
- Bootstrap 5
- Micrometer (Prometheus registry, OpenTelemetry tracing)

## Requirements

- Java 25 or newer
- Gradle 9.1+
- PostgreSQL
- Kafka (for click event tracking)

## Local Development

1. Clone the repository:
```bash
git clone https://github.com/yourusername/shortliner.git
cd shortliner
```

2. Configure environment variables in `.env.local` file:
```properties
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shortliner
DB_USERNAME=your_user
DB_PASSWORD=your_password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KEYCLOAK_JWK_SET_URI=http://keycloak.local/realms/shortliner/protocol/openid-connect/certs
KEYCLOAK_ISSUER_URI=http://keycloak.local/realms/shortliner
```

3. Run the application:
```bash
./gradlew bootRun
```

Optionally add `SPRING_PROFILES_ACTIVE=dev,local` to `.env.local` to get Spring Security DEBUG logs and SQL output (`application-local.properties`).

The application will be available at `http://localhost:8080`

## API Endpoints

| Endpoint               | Method | Auth                 | Description                                        |
|------------------------|--------|----------------------|----------------------------------------------------|
| `/`                    | GET    | No                   | Home page                                          |
| `/shorten/{shortCode}` | GET    | No                   | Redirect to original URL                           |
| `/shorten`             | POST   | Optional             | Create shortened URL; owned by the caller if a JWT is sent |
| `/shorten`             | GET    | User                 | List the caller's own links                        |
| `/shorten/{shortCode}` | DELETE | Owner or `admin`     | Delete a link (204); 404 if missing or not yours   |

Auth is a Keycloak (realm `shortliner`) bearer JWT, normally added by `shortliner-gateway`.
A request with an invalid or expired token gets 401 even on anonymous endpoints. Locally, run
`shortliner-gateway` and log in through `http://localhost:8084` to get tokens relayed here.

### Example: Create Short URL

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/very/long/url"}'
```

## Observability

- **Metrics**: Prometheus-formatted metrics at `/actuator/prometheus`, including HTTP latency histograms and business counters for shorten/redirect outcomes.
- **Tracing**: OpenTelemetry tracing (with Kafka trace-context propagation) is wired in but export is off by default — set `OTEL_TRACING_EXPORT_ENABLED=true` and `OTEL_EXPORTER_OTLP_ENDPOINT` to send traces to a collector.
- **Logging**: set `LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash` (or `ecs`) for structured JSON logs with automatic trace/span correlation; unset for plain console output.

```bash
curl http://localhost:8080/actuator/prometheus
```

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
