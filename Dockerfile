# Use the official eclipse-temurin:23-jdk image
FROM eclipse-temurin:23-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy Gradle configuration and source files
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
COPY src src

# Grant write permissions to the /app directory
RUN chmod -R 777 /app

# Download dependencies and build the application
RUN ./gradlew clean build --no-daemon

# Exclude the -plain.jar and copy the main .jar file
RUN cp $(find build/libs -type f -name '*-SNAPSHOT.jar' ! -name '*-plain.jar') /app/app.jar

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
