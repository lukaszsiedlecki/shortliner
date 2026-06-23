# ShortLiner - URL Shortener

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Cloud Run](https://img.shields.io/badge/Cloud%20Run-GCP-blue)

ShortLiner is a modern URL shortening application optimized for high performance and reliability. The application is deployed on Google Cloud Run, providing automatic scaling and high availability.

## Features

- Shortening long URLs into concise, memorable codes
- Automatic redirects from shortened URLs
- JWT-based authentication for protected endpoints
- Click event tracking via Kafka
- Caching of frequently used URLs for better performance
- Concurrent request handling with retry mechanism
- URL validation
- Modern user interface

## Technologies

- Java 25
- Spring Boot 3.5.10
- Spring Security with OAuth2 Resource Server (JWT)
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Caffeine Cache
- Google Cloud Run
- Google Cloud SQL
- Thymeleaf
- Bootstrap 5

## Requirements

- Java 25 or newer
- Gradle 9.1+
- PostgreSQL
- Kafka (for click event tracking)
- (Optional) Google Cloud Platform account for deployment

## Local Development

1. Clone the repository:
```bash
git clone https://github.com/yourusername/shortliner.git
cd shortliner
```

2. Configure environment variables in `.env` file:
```properties
DB_HOST=localhost
DB_NAME=shortliner
DB_USER=your_user
DB_PASSWORD=your_password
JWT_ISSUER_URI=http://localhost:8080
JWT_JWK_SET_URI=http://localhost:8080/.well-known/jwks.json
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
| `/shorten`             | POST   | Yes  | Create shortened URL     |

### Example: Create Short URL

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"url": "https://example.com/very/long/url"}'
```

## Google Cloud Run Deployment

1. Build Docker image:
```bash
./gradlew bootBuildImage
```

2. Deploy to Cloud Run:
```bash
gcloud run deploy shortliner \
  --image gcr.io/your-project/shortliner \
  --platform managed \
  --region your-preferred-region \
  --allow-unauthenticated
```

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
