# ShortLiner - URL Shortener

![Java](https://img.shields.io/badge/Java-23-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Cloud Run](https://img.shields.io/badge/Cloud%20Run-GCP-blue)

ShortLiner is a modern URL shortening application optimized for high performance and reliability. The application is deployed on Google Cloud Run, providing automatic scaling and high availability.

## 🚀 Features

- Shortening long URLs into concise, memorable codes
- Automatic redirects from shortened URLs
- Caching of frequently used URLs for better performance
- Concurrent request handling with retry mechanism
- URL validation
- Modern user interface

## 🛠 Technologies

- Java 23
- Spring Boot 3.4
- Spring Data JPA
- PostgreSQL
- Caffeine Cache
- Google Cloud Run
- Google Cloud SQL
- Thymeleaf
- Bootstrap 5

## 💻 Requirements

- Java 23 or newer
- Gradle 8.8+
- PostgreSQL
- (Optional) Google Cloud Platform account for deployment

## 🏃‍♂️ Local Development

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
```

3. Run the application:
```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## ☁️ Google Cloud Run Deployment

1. Build Docker image:
```bash
./gradlew bootBuildImage
```

2. Deploy to Cloud Run:
```bash
gcloud run deploy shortliner \
  --image gcr.io/your-project/shortliner \
  --platform managed \
  --region europe-central2 \
  --allow-unauthenticated
```

## 📝 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details. 