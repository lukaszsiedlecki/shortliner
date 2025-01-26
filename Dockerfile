# Build stage
FROM eclipse-temurin:23-jdk AS build

# Set the working directory inside the container
WORKDIR /app

# Copy Gradle configuration and source files
COPY build.gradle settings.gradle ./
COPY src src

# Install Gradle
RUN apt-get update && apt-get install -y wget unzip \
    && wget https://services.gradle.org/distributions/gradle-8.12.1-bin.zip \
    && unzip gradle-8.12.1-bin.zip -d /opt \
    && ln -s /opt/gradle-8.12.1/bin/gradle /usr/bin/gradle

# Download dependencies and build the application
RUN gradle clean build --no-daemon

# Package stage
FROM eclipse-temurin:23-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the built .jar file from the build stage
COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]