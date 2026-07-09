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
- Spring Security (CORS/CSRF handling; production authentication mechanism TBD)
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
```

3. Run the application:
```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## API Endpoints

| Endpoint               | Method | Auth | Description              |
|------------------------|--------|------|--------------------------|
| `/`                    | GET    | No   | Home page                |
| `/shorten/{shortCode}` | GET    | No   | Redirect to original URL |
| `/shorten`             | POST   | No*  | Create shortened URL     |

\* All endpoints are currently open — the active Spring profile is always
`dev`, which permits every request. A `prd` profile exists with stricter
authorization rules, but no authentication mechanism is wired up yet; it
will be added before this goes to production.

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
